package com.javainterview.rag.knowledge.persistence;

import com.javainterview.rag.knowledge.document.MarkdownDocument;
import com.javainterview.rag.knowledge.document.MarkdownIndexingState;
import com.javainterview.rag.knowledge.document.MarkdownIndexingStateStore;
import com.javainterview.rag.knowledge.document.MarkdownIndexingStatus;
import com.javainterview.rag.knowledge.document.MarkdownIndexBuild;
import com.javainterview.rag.knowledge.document.MarkdownIndexPublication;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Objects;
import java.util.List;

public class MySqlMarkdownIndexingStateStore implements MarkdownIndexingStateStore {

    private final KnowledgeIndexingStateMapper mapper;

    public MySqlMarkdownIndexingStateStore(KnowledgeIndexingStateMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "索引状态 Mapper 不能为空");
    }

    @Override
    public void markIndexing(MarkdownDocument document, int chunkCount) {
        save(document, chunkCount, MarkdownIndexingStatus.INDEXING, null);
    }

    @Override
    public void markReady(MarkdownDocument document, int chunkCount) {
        save(document, chunkCount, MarkdownIndexingStatus.READY, null);
    }

    @Override
    public void markFailed(MarkdownDocument document, int chunkCount, RuntimeException exception) {
        Objects.requireNonNull(exception, "索引异常不能为空");
        save(document, chunkCount, MarkdownIndexingStatus.FAILED,
                exception.getClass().getSimpleName());
    }

    @Override
    public Optional<MarkdownIndexingState> find(String source, String documentVersion) {
        KnowledgeIndexingStateEntity entity = mapper.find(source, documentVersion);
        return entity == null ? Optional.empty() : Optional.of(toState(entity));
    }

    @Override
    public void removeBySource(String source) {
        mapper.deleteBySource(source);
    }

    @Override
    public void beginBuild(MarkdownDocument document, int chunkCount, MarkdownIndexBuild build) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        Objects.requireNonNull(build, "索引构建信息不能为空");
        mapper.beginBuild(document.source(), document.version(), chunkCount,
                build.generation(), build.attemptId());
    }

    @Override
    @Transactional
    public boolean publishBuild(MarkdownDocument document, int chunkCount, MarkdownIndexBuild build) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        Objects.requireNonNull(build, "索引构建信息不能为空");
        if (mapper.publishBuild(document.source(), document.version(), chunkCount,
                build.generation(), build.attemptId()) != 1) {
            return false;
        }
        mapper.clearOtherPublishedVersions(document.source(), document.version());
        return true;
    }

    @Override
    public void failBuild(MarkdownDocument document, int chunkCount, MarkdownIndexBuild build,
                          RuntimeException exception, boolean temporaryCleaned) {
        Objects.requireNonNull(exception, "索引异常不能为空");
        mapper.failBuild(document.source(), document.version(), build.generation(),
                build.attemptId(), exception.getClass().getSimpleName(), temporaryCleaned);
    }

    @Override
    public List<MarkdownIndexPublication> findPublished() {
        return mapper.findPublished().stream()
                .map(entity -> new MarkdownIndexPublication(entity.getSource(),
                        entity.getDocumentVersion(), entity.getPublishedGeneration(),
                        entity.getBuildingGeneration()))
                .toList();
    }

    private void save(MarkdownDocument document, int chunkCount,
                      MarkdownIndexingStatus status, String errorType) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        if (chunkCount < 0) {
            throw new IllegalArgumentException("Chunk 数量不能小于 0");
        }
        mapper.upsert(document.source(), document.version(), chunkCount, status.name(), errorType);
    }

    private MarkdownIndexingState toState(KnowledgeIndexingStateEntity entity) {
        return new MarkdownIndexingState(entity.getSource(), entity.getDocumentVersion(),
                entity.getChunkCount(), MarkdownIndexingStatus.valueOf(entity.getStatus()),
                entity.getErrorType(), entity.getPublishedGeneration(),
                entity.getBuildingGeneration(), entity.getBuildingChunkCount(),
                entity.getAttemptId());
    }
}
