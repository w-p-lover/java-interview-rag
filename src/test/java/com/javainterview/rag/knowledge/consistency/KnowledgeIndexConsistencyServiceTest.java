package com.javainterview.rag.knowledge.consistency;

import com.javainterview.rag.knowledge.document.InMemoryMarkdownImportRegistry;
import com.javainterview.rag.knowledge.document.InMemoryMarkdownIndexingStateStore;
import com.javainterview.rag.knowledge.document.KnowledgeDocumentOutline;
import com.javainterview.rag.knowledge.document.MarkdownChunk;
import com.javainterview.rag.knowledge.document.MarkdownContentImporter;
import com.javainterview.rag.knowledge.document.MarkdownDocument;
import com.javainterview.rag.knowledge.document.MarkdownIndexingStatus;
import com.javainterview.rag.knowledge.document.MarkdownRebuildResult;
import com.javainterview.rag.knowledge.vector.LuceneMarkdownChunkIndex;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeIndexConsistencyServiceTest {

    private final InMemoryMarkdownImportRegistry registry = new InMemoryMarkdownImportRegistry();
    private final InMemoryMarkdownIndexingStateStore stateStore = new InMemoryMarkdownIndexingStateStore();
    private final LuceneMarkdownChunkIndex sparseIndex =
            new LuceneMarkdownChunkIndex(new ByteBuffersDirectory());

    @AfterEach
    void closeIndex() {
        sparseIndex.close();
    }

    @Test
    void reportsConsistentCountsWithoutRequiringRecoveredProcessState() {
        MarkdownDocument document = registerCurrent("java/collections.md", "HashMap", 2);
        sparseIndex.index(document, chunks(document, 2));
        KnowledgeIndexConsistencyService service = service(
                (source, version) -> new KnowledgeIndexCounts(2, 2));

        KnowledgeIndexConsistencyReport report = service.inspect(document.source());

        assertTrue(report.consistent());
        assertEquals(2, report.expectedChunkCount());
        assertFalse(report.indexingState().tracked());
        assertEquals(KnowledgeIndexConsistencyReport.StoreStatus.CONSISTENT,
                report.dense().status());
        assertEquals(KnowledgeIndexConsistencyReport.StoreStatus.CONSISTENT,
                report.sparse().status());
        assertTrue(report.dense().indexVersionMatched());
        assertTrue(report.sparse().indexVersionMatched());
    }

    @Test
    void distinguishesMissingChunksStaleVersionsAndFailedIndexingState() {
        MarkdownDocument oldDocument = MarkdownDocument.create("java/collections.md", "old");
        sparseIndex.index(oldDocument, chunks(oldDocument, 1));
        MarkdownDocument current = registerCurrent("java/collections.md", "current", 2);
        sparseIndex.index(current, chunks(current, 2));
        stateStore.markFailed(current, 2, new IllegalStateException("cleanup"));
        KnowledgeIndexConsistencyService service = service(
                (source, version) -> new KnowledgeIndexCounts(1, 3));

        KnowledgeIndexConsistencyReport report = service.inspect(current.source());

        assertFalse(report.consistent());
        assertEquals(KnowledgeIndexConsistencyReport.StoreStatus.MISSING_CHUNKS,
                report.dense().status());
        assertEquals(KnowledgeIndexConsistencyReport.StoreStatus.STALE_VERSIONS,
                report.sparse().status());
        assertEquals(1L, report.sparse().staleVersionChunkCount());
        assertEquals("FAILED", report.indexingState().status().name());
        assertEquals("IllegalStateException", report.indexingState().errorType());
    }

    @Test
    void preservesUnavailableStoreAsDiagnosticResult() {
        MarkdownDocument document = registerCurrent("java/collections.md", "HashMap", 1);
        sparseIndex.index(document, chunks(document, 1));
        KnowledgeIndexConsistencyService service = service((source, version) -> {
            throw new IllegalStateException("qdrant down");
        });

        KnowledgeIndexConsistencyReport report = service.inspect(document.source());

        assertFalse(report.consistent());
        assertEquals(KnowledgeIndexConsistencyReport.StoreStatus.UNAVAILABLE,
                report.dense().status());
        assertEquals("IllegalStateException", report.dense().errorType());
        assertEquals(KnowledgeIndexConsistencyReport.StoreStatus.CONSISTENT,
                report.sparse().status());
    }

    @Test
    void distinguishesAllAndPartialIndexVersionMismatch() {
        MarkdownDocument document = registerCurrent("java/collections.md", "HashMap", 2);

        KnowledgeIndexConsistencyReport allMismatch = service(
                (source, version) -> new KnowledgeIndexCounts(2, 2, 0))
                .inspect(document.source());
        KnowledgeIndexConsistencyReport partialMismatch = service(
                (source, version) -> new KnowledgeIndexCounts(2, 2, 1))
                .inspect(document.source());

        assertFalse(allMismatch.consistent());
        assertEquals(KnowledgeIndexConsistencyReport.IndexVersionStatus.ALL_MISMATCH,
                allMismatch.dense().indexVersionStatus());
        assertFalse(partialMismatch.consistent());
        assertEquals(KnowledgeIndexConsistencyReport.IndexVersionStatus.PARTIAL_MISMATCH,
                partialMismatch.dense().indexVersionStatus());
    }

    @Test
    void rejectsBlankOrMissingSource() {
        KnowledgeIndexConsistencyService service = service(
                (source, version) -> new KnowledgeIndexCounts(0, 0));

        assertEquals("文档来源不能为空", assertThrows(IllegalArgumentException.class,
                () -> service.inspect(" ")).getMessage());
        assertEquals("资料不存在", assertThrows(NoSuchElementException.class,
                () -> service.inspect("java/missing.md")).getMessage());
    }

    @Test
    void repairsCurrentSnapshotAndReturnsConvergedReport() {
        MarkdownDocument document = registerCurrent("java/repair.md", "HashMap", 2);
        List<MarkdownChunk> expectedChunks = chunks(document, 2);
        sparseIndex.index(document, expectedChunks.subList(0, 1));
        AtomicInteger inspections = new AtomicInteger();
        KnowledgeVectorIndexInspector vectorInspector = (source, version) ->
                inspections.getAndIncrement() == 0
                        ? new KnowledgeIndexCounts(1, 1)
                        : new KnowledgeIndexCounts(2, 2);
        MarkdownContentImporter importer = mock(MarkdownContentImporter.class);
        when(importer.rebuildCurrentSnapshot(document.source())).thenAnswer(invocation -> {
            sparseIndex.rebuild(document, expectedChunks);
            return new MarkdownRebuildResult(document, expectedChunks);
        });
        KnowledgeIndexConsistencyService service = new KnowledgeIndexConsistencyService(
                registry, stateStore, vectorInspector, sparseIndex, importer);

        KnowledgeIndexRepairResult result = service.repair(document.source());

        assertTrue(result.consistency().consistent());
        assertEquals(2, result.chunkCount());
        verify(importer).rebuildCurrentSnapshot(document.source());
    }

    @Test
    void rejectsRepairWhenIndexIsAlreadyConsistent() {
        MarkdownDocument document = registerCurrent("java/consistent.md", "HashMap", 1);
        sparseIndex.index(document, chunks(document, 1));
        MarkdownContentImporter importer = mock(MarkdownContentImporter.class);
        KnowledgeIndexConsistencyService service = new KnowledgeIndexConsistencyService(
                registry, stateStore, (source, version) -> new KnowledgeIndexCounts(1, 1),
                sparseIndex, importer);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.repair(document.source()));

        assertEquals("当前索引已经一致，无需修复", exception.getMessage());
        verify(importer, never()).rebuildCurrentSnapshot(document.source());
    }

    @Test
    void rejectsRepairWhenAStoreIsUnavailable() {
        MarkdownDocument document = registerCurrent("java/unavailable.md", "HashMap", 1);
        sparseIndex.index(document, chunks(document, 1));
        MarkdownContentImporter importer = mock(MarkdownContentImporter.class);
        KnowledgeIndexConsistencyService service = new KnowledgeIndexConsistencyService(
                registry, stateStore, (source, version) -> {
                    throw new IllegalStateException("qdrant down");
                }, sparseIndex, importer);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.repair(document.source()));

        assertEquals("索引存储不可用，不能执行修复", exception.getMessage());
        verify(importer, never()).rebuildCurrentSnapshot(document.source());
    }

    @Test
    void marksRepairFailedWhenPostInspectionStillFindsMismatch() {
        MarkdownDocument document = registerCurrent("java/not-converged.md", "HashMap", 2);
        List<MarkdownChunk> expectedChunks = chunks(document, 2);
        sparseIndex.index(document, expectedChunks);
        MarkdownContentImporter importer = mock(MarkdownContentImporter.class);
        when(importer.rebuildCurrentSnapshot(document.source()))
                .thenReturn(new MarkdownRebuildResult(document, expectedChunks));
        KnowledgeIndexConsistencyService service = new KnowledgeIndexConsistencyService(
                registry, stateStore, (source, version) -> new KnowledgeIndexCounts(1, 1),
                sparseIndex, importer);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.repair(document.source()));

        assertEquals("修复后索引仍不一致，请查看最新巡检结果", exception.getMessage());
        assertEquals(MarkdownIndexingStatus.FAILED,
                stateStore.find(document.source(), document.version()).orElseThrow().status());
    }

    private KnowledgeIndexConsistencyService service(KnowledgeVectorIndexInspector vectorInspector) {
        return new KnowledgeIndexConsistencyService(
                registry, stateStore, vectorInspector, sparseIndex,
                mock(MarkdownContentImporter.class));
    }

    private MarkdownDocument registerCurrent(String source, String content, int chunkCount) {
        MarkdownDocument document = MarkdownDocument.create(source, content);
        registry.register(document, new KnowledgeDocumentOutline(0, chunkCount, List.of()));
        return document;
    }

    private List<MarkdownChunk> chunks(MarkdownDocument document, int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(order -> new MarkdownChunk(document.source(), order, order,
                        1, List.of("Java"), "chunk " + order))
                .toList();
    }
}
