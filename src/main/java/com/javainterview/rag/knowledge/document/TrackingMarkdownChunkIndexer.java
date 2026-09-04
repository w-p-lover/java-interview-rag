package com.javainterview.rag.knowledge.document;

import java.util.List;
import java.util.Objects;

/** 记录索引编排状态，供失败分层读取，不改变具体索引器行为。 */
public final class TrackingMarkdownChunkIndexer implements MarkdownChunkIndexer {

    private final MarkdownChunkIndexer delegate;
    private final MarkdownIndexingStateStore stateStore;

    public TrackingMarkdownChunkIndexer(MarkdownChunkIndexer delegate,
                                         MarkdownIndexingStateStore stateStore) {
        this.delegate = Objects.requireNonNull(delegate, "Chunk 索引器不能为空");
        this.stateStore = Objects.requireNonNull(stateStore, "索引状态存储不能为空");
    }

    @Override
    public void index(MarkdownDocument document, List<MarkdownChunk> chunks) {
        track(document, chunks, () -> delegate.index(document, chunks));
    }

    @Override
    public void rebuild(MarkdownDocument document, List<MarkdownChunk> chunks) {
        track(document, chunks, () -> delegate.rebuild(document, chunks));
    }

    @Override
    public void deleteOtherVersions(MarkdownDocument document) {
        int chunkCount = stateStore.find(document.source(), document.version())
                .map(MarkdownIndexingState::chunkCount)
                .orElse(0);
        track(document, chunkCount, () -> delegate.deleteOtherVersions(document));
    }

    @Override
    public void deleteVersion(MarkdownDocument document) {
        delegate.deleteVersion(document);
    }

    @Override
    public void deleteBySource(String source) {
        try {
            delegate.deleteBySource(source);
        } finally {
            stateStore.removeBySource(source);
        }
    }

    private void track(MarkdownDocument document, List<MarkdownChunk> chunks, Runnable operation) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        Objects.requireNonNull(chunks, "Markdown Chunk 列表不能为空");
        track(document, chunks.size(), operation);
    }

    private void track(MarkdownDocument document, int chunkCount, Runnable operation) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        stateStore.markIndexing(document, chunkCount);
        try {
            operation.run();
            stateStore.markReady(document, chunkCount);
        } catch (RuntimeException exception) {
            stateStore.markFailed(document, chunkCount, exception);
            throw exception;
        }
    }
}
