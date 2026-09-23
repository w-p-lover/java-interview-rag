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
        index(document, chunks, null);
    }

    @Override
    public void index(MarkdownDocument document, List<MarkdownChunk> chunks, String attemptId) {
        trackGeneration(document, chunks, attemptId, false);
    }

    @Override
    public void rebuild(MarkdownDocument document, List<MarkdownChunk> chunks) {
        rebuild(document, chunks, null);
    }

    @Override
    public void rebuild(MarkdownDocument document, List<MarkdownChunk> chunks, String attemptId) {
        trackGeneration(document, chunks, attemptId, true);
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
        delegate.deleteBySource(source);
        // 只有所有索引删除成功后才能清理状态；失败时保留状态供巡检和人工重试使用。
        stateStore.removeBySource(source);
    }

    @Override
    public boolean cleansFailedWrites() {
        return delegate instanceof GenerationAwareMarkdownChunkIndexer;
    }

    private void track(MarkdownDocument document, List<MarkdownChunk> chunks, Runnable operation) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        Objects.requireNonNull(chunks, "Markdown Chunk 列表不能为空");
        track(document, chunks.size(), operation);
    }

    private void trackGeneration(MarkdownDocument document, List<MarkdownChunk> chunks,
                                 String attemptId, boolean rebuild) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        Objects.requireNonNull(chunks, "Markdown Chunk 列表不能为空");
        if (!(delegate instanceof GenerationAwareMarkdownChunkIndexer generationAware)) {
            track(document, chunks, () -> {
                if (rebuild) {
                    delegate.rebuild(document, chunks);
                } else {
                    delegate.index(document, chunks);
                }
            });
            return;
        }
        MarkdownIndexBuild build = MarkdownIndexBuild.create(attemptId);
        stateStore.beginBuild(document, chunks.size(), build);
        try {
            generationAware.buildGeneration(document, chunks, build);
            generationAware.verifyGeneration(document, chunks, build);
        } catch (RuntimeException exception) {
            failAndClean(document, chunks.size(), build, generationAware, exception);
            throw exception;
        }
        if (!stateStore.publishBuild(document, chunks.size(), build)) {
            IllegalStateException stale = new IllegalStateException("索引构建任务已被更新代次替代，禁止发布旧结果");
            failAndClean(document, chunks.size(), build, generationAware, stale);
            throw stale;
        }
        try {
            generationAware.deleteOtherGenerations(document, build);
        } catch (RuntimeException exception) {
            stateStore.markFailed(document, chunks.size(), exception);
            throw exception;
        }
    }

    private void failAndClean(MarkdownDocument document, int chunkCount,
                              MarkdownIndexBuild build,
                              GenerationAwareMarkdownChunkIndexer generationAware,
                              RuntimeException failure) {
        boolean cleaned = false;
        try {
            generationAware.deleteGeneration(document, build);
            cleaned = true;
        } catch (RuntimeException cleanupException) {
            failure.addSuppressed(cleanupException);
        }
        stateStore.failBuild(document, chunkCount, build, failure, cleaned);
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
