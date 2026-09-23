package com.javainterview.rag.knowledge.consistency;

import com.javainterview.rag.knowledge.document.KnowledgeDocumentSummary;
import com.javainterview.rag.knowledge.document.MarkdownContentImporter;
import com.javainterview.rag.knowledge.document.MarkdownImportRegistry;
import com.javainterview.rag.knowledge.document.MarkdownIndexingState;
import com.javainterview.rag.knowledge.document.MarkdownIndexingStateStore;
import com.javainterview.rag.knowledge.document.MarkdownIndexingStatus;
import com.javainterview.rag.knowledge.vector.LuceneMarkdownChunkIndex;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;

public final class KnowledgeIndexConsistencyService {

    private final MarkdownImportRegistry importRegistry;
    private final MarkdownIndexingStateStore indexingStateStore;
    private final KnowledgeVectorIndexInspector vectorIndexInspector;
    private final LuceneMarkdownChunkIndex sparseIndex;
    private final MarkdownContentImporter contentImporter;

    public KnowledgeIndexConsistencyService(MarkdownImportRegistry importRegistry,
                                            MarkdownIndexingStateStore indexingStateStore,
                                            KnowledgeVectorIndexInspector vectorIndexInspector,
                                            LuceneMarkdownChunkIndex sparseIndex,
                                            MarkdownContentImporter contentImporter) {
        this.importRegistry = Objects.requireNonNull(importRegistry, "文档登记不能为空");
        this.indexingStateStore = Objects.requireNonNull(indexingStateStore, "索引状态存储不能为空");
        this.vectorIndexInspector = Objects.requireNonNull(vectorIndexInspector, "向量索引巡检器不能为空");
        this.sparseIndex = Objects.requireNonNull(sparseIndex, "稀疏索引不能为空");
        this.contentImporter = Objects.requireNonNull(contentImporter, "文档导入器不能为空");
    }

    public KnowledgeIndexConsistencyReport inspect(String source) {
        String normalizedSource = requireSource(source);
        KnowledgeDocumentSummary document = importRegistry.findCurrentDocuments().stream()
                .filter(candidate -> candidate.source().equals(normalizedSource))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("资料不存在"));
        Integer expectedChunkCount = document.outline() == null
                ? null : document.outline().chunkCount();
        KnowledgeIndexConsistencyReport.StoreCheck dense = inspectStore(expectedChunkCount,
                () -> vectorIndexInspector.inspect(document.source(), document.version()));
        KnowledgeIndexConsistencyReport.StoreCheck sparse = inspectStore(expectedChunkCount,
                () -> new KnowledgeIndexCounts(
                        sparseIndex.countBySourceAndVersion(document.source(), document.version()),
                        sparseIndex.countBySource(document.source()),
                        sparseIndex.countBySourceAndVersionAndMetadata(
                                document.source(), document.version())));
        KnowledgeIndexConsistencyReport.IndexingStateCheck indexingState =
                indexingState(document, expectedChunkCount);
        boolean consistent = dense.status() == KnowledgeIndexConsistencyReport.StoreStatus.CONSISTENT
                && sparse.status() == KnowledgeIndexConsistencyReport.StoreStatus.CONSISTENT
                && dense.indexVersionStatus() == KnowledgeIndexConsistencyReport.IndexVersionStatus.MATCHED
                && sparse.indexVersionStatus() == KnowledgeIndexConsistencyReport.IndexVersionStatus.MATCHED
                && (!indexingState.tracked()
                || (indexingState.status() == MarkdownIndexingStatus.READY
                && !Boolean.FALSE.equals(indexingState.expectedChunkCountMatched())));
        return new KnowledgeIndexConsistencyReport(
                document.source(), document.version(), expectedChunkCount, consistent,
                indexingState, dense, sparse);
    }

    public KnowledgeIndexRepairResult repair(String source) {
        KnowledgeIndexConsistencyReport before = inspect(source);
        if (before.consistent()) {
            throw new IllegalArgumentException("当前索引已经一致，无需修复");
        }
        if (before.dense().status() == KnowledgeIndexConsistencyReport.StoreStatus.UNAVAILABLE
                || before.sparse().status() == KnowledgeIndexConsistencyReport.StoreStatus.UNAVAILABLE) {
            throw new IllegalArgumentException("索引存储不可用，不能执行修复");
        }
        var rebuilt = contentImporter.rebuildCurrentSnapshot(before.source());
        KnowledgeIndexConsistencyReport after = inspect(before.source());
        if (!after.consistent()) {
            IllegalStateException exception = new IllegalStateException("修复后索引仍不一致，请查看最新巡检结果");
            indexingStateStore.markFailed(rebuilt.document(), rebuilt.chunks().size(), exception);
            throw exception;
        }
        return new KnowledgeIndexRepairResult(
                rebuilt.document().source(), rebuilt.document().version(), rebuilt.chunks().size(), after);
    }

    private KnowledgeIndexConsistencyReport.IndexingStateCheck indexingState(
            KnowledgeDocumentSummary document, Integer expectedChunkCount) {
        return indexingStateStore.find(document.source(), document.version())
                .map(state -> trackedState(state, expectedChunkCount))
                .orElseGet(() -> new KnowledgeIndexConsistencyReport.IndexingStateCheck(
                        false, null, null, null, null, null, null));
    }

    private KnowledgeIndexConsistencyReport.IndexingStateCheck trackedState(
            MarkdownIndexingState state, Integer expectedChunkCount) {
        Boolean matched = expectedChunkCount == null
                ? null : state.chunkCount() == expectedChunkCount;
        return new KnowledgeIndexConsistencyReport.IndexingStateCheck(
                true, state.status(), state.chunkCount(), matched, state.errorType(),
                state.publishedGeneration(), state.buildingGeneration());
    }

    private KnowledgeIndexConsistencyReport.StoreCheck inspectStore(
            Integer expectedChunkCount, Supplier<KnowledgeIndexCounts> inspection) {
        try {
            KnowledgeIndexCounts counts = inspection.get();
            KnowledgeIndexConsistencyReport.StoreStatus status = status(expectedChunkCount, counts);
            KnowledgeIndexConsistencyReport.IndexVersionStatus indexVersionStatus = indexVersionStatus(
                    expectedChunkCount, counts);
            return new KnowledgeIndexConsistencyReport.StoreCheck(
                    status,
                    counts.currentVersionChunkCount(),
                    counts.currentVersionMetadataMatchCount(),
                    counts.staleVersionChunkCount(),
                    counts.sourceChunkCount(),
                    expectedChunkCount == null ? null
                            : counts.currentVersionChunkCount() == expectedChunkCount,
                    indexVersionStatus == KnowledgeIndexConsistencyReport.IndexVersionStatus.MATCHED,
                    indexVersionStatus,
                    null);
        } catch (RuntimeException exception) {
            return new KnowledgeIndexConsistencyReport.StoreCheck(
                    KnowledgeIndexConsistencyReport.StoreStatus.UNAVAILABLE,
                    null, null, null, null, null, null,
                    KnowledgeIndexConsistencyReport.IndexVersionStatus.UNVERIFIABLE,
                    exception.getClass().getSimpleName());
        }
    }

    private KnowledgeIndexConsistencyReport.IndexVersionStatus indexVersionStatus(
            Integer expectedChunkCount, KnowledgeIndexCounts counts) {
        if (expectedChunkCount == null) {
            return KnowledgeIndexConsistencyReport.IndexVersionStatus.UNVERIFIABLE;
        }
        long currentCount = counts.currentVersionChunkCount();
        long matchedCount = counts.currentVersionMetadataMatchCount();
        if (currentCount == 0) {
            return expectedChunkCount == 0
                    ? KnowledgeIndexConsistencyReport.IndexVersionStatus.MATCHED
                    : KnowledgeIndexConsistencyReport.IndexVersionStatus.UNVERIFIABLE;
        }
        if (matchedCount == currentCount) {
            return KnowledgeIndexConsistencyReport.IndexVersionStatus.MATCHED;
        }
        return matchedCount == 0
                ? KnowledgeIndexConsistencyReport.IndexVersionStatus.ALL_MISMATCH
                : KnowledgeIndexConsistencyReport.IndexVersionStatus.PARTIAL_MISMATCH;
    }

    private KnowledgeIndexConsistencyReport.StoreStatus status(
            Integer expectedChunkCount, KnowledgeIndexCounts counts) {
        if (expectedChunkCount == null) {
            return KnowledgeIndexConsistencyReport.StoreStatus.UNVERIFIABLE;
        }
        if (counts.currentVersionChunkCount() < expectedChunkCount) {
            return KnowledgeIndexConsistencyReport.StoreStatus.MISSING_CHUNKS;
        }
        if (counts.currentVersionChunkCount() > expectedChunkCount) {
            return KnowledgeIndexConsistencyReport.StoreStatus.EXCESS_CHUNKS;
        }
        if (counts.staleVersionChunkCount() > 0) {
            return KnowledgeIndexConsistencyReport.StoreStatus.STALE_VERSIONS;
        }
        return KnowledgeIndexConsistencyReport.StoreStatus.CONSISTENT;
    }

    private String requireSource(String source) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("文档来源不能为空");
        }
        if (!source.equals(source.strip())) {
            throw new IllegalArgumentException("文档来源不能包含首尾空白");
        }
        return source;
    }
}
