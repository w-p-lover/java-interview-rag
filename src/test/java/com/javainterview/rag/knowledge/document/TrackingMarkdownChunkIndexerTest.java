package com.javainterview.rag.knowledge.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doAnswer;

class TrackingMarkdownChunkIndexerTest {

    @Test
    void recordsReadyAndFailedIndexingStatesAndCleansSource() {
        MarkdownChunkIndexer delegate = mock(MarkdownChunkIndexer.class);
        InMemoryMarkdownIndexingStateStore store = new InMemoryMarkdownIndexingStateStore();
        TrackingMarkdownChunkIndexer tracker = new TrackingMarkdownChunkIndexer(delegate, store);
        MarkdownDocument document = MarkdownDocument.create("java/index.md", "内容");
        List<MarkdownChunk> chunks = List.of(new MarkdownChunk(
                document.source(), 0, 0, 1, List.of("Java"), "内容"));

        tracker.index(document, chunks);
        assertEquals(MarkdownIndexingStatus.READY,
                store.find(document.source(), document.version()).orElseThrow().status());

        doThrow(new IllegalStateException("索引不可用"))
                .when(delegate).rebuild(any(MarkdownDocument.class), any());
        assertThrows(IllegalStateException.class, () -> tracker.rebuild(document, chunks));
        assertEquals(MarkdownIndexingStatus.FAILED,
                store.find(document.source(), document.version()).orElseThrow().status());

        tracker.deleteBySource(document.source());
        assertTrue(store.find(document.source(), document.version()).isEmpty());
    }

    @Test
    void keepsStateWhenSourceDeletionFails() {
        MarkdownChunkIndexer delegate = mock(MarkdownChunkIndexer.class);
        InMemoryMarkdownIndexingStateStore store = new InMemoryMarkdownIndexingStateStore();
        TrackingMarkdownChunkIndexer tracker = new TrackingMarkdownChunkIndexer(delegate, store);
        MarkdownDocument document = MarkdownDocument.create("java/delete-failure.md", "内容");
        List<MarkdownChunk> chunks = List.of(new MarkdownChunk(
                document.source(), 0, 0, 1, List.of("Java"), "内容"));

        tracker.index(document, chunks);
        doThrow(new IllegalStateException("索引删除失败"))
                .when(delegate).deleteBySource(document.source());

        assertThrows(IllegalStateException.class,
                () -> tracker.deleteBySource(document.source()));
        assertEquals(MarkdownIndexingStatus.READY,
                store.find(document.source(), document.version()).orElseThrow().status());
        verify(delegate).deleteBySource(document.source());
        verify(delegate, never()).deleteVersion(document);
    }

    @Test
    void denseSuccessAndSparseFailureKeepPublishedGenerationAndCleanTemporaryData() {
        GenerationAwareMarkdownChunkIndexer dense = mock(GenerationAwareMarkdownChunkIndexer.class);
        GenerationAwareMarkdownChunkIndexer sparse = mock(GenerationAwareMarkdownChunkIndexer.class);
        InMemoryMarkdownIndexingStateStore store = new InMemoryMarkdownIndexingStateStore();
        TrackingMarkdownChunkIndexer tracker = new TrackingMarkdownChunkIndexer(
                new CompositeMarkdownChunkIndexer(List.of(dense, sparse)), store);
        MarkdownDocument document = MarkdownDocument.create("java/atomic.md", "内容");
        List<MarkdownChunk> chunks = List.of(new MarkdownChunk(
                document.source(), 0, 0, 1, List.of("Java"), "新内容"));
        store.markReady(document, 1);
        doThrow(new IllegalStateException("Sparse build failed"))
                .when(sparse).buildGeneration(same(document), same(chunks), any());

        assertThrows(IllegalStateException.class,
                () -> tracker.rebuild(document, chunks, "attempt-1"));

        MarkdownIndexingState state = store.find(document.source(), document.version()).orElseThrow();
        assertEquals(MarkdownIndexPublication.LEGACY_GENERATION, state.publishedGeneration());
        assertEquals(MarkdownIndexingStatus.FAILED, state.status());
        verify(dense).deleteGeneration(same(document), any());
        verify(sparse).deleteGeneration(same(document), any());
    }

    @Test
    void bothStoresBuildAndPublishTheSameGeneration() {
        GenerationAwareMarkdownChunkIndexer dense = mock(GenerationAwareMarkdownChunkIndexer.class);
        GenerationAwareMarkdownChunkIndexer sparse = mock(GenerationAwareMarkdownChunkIndexer.class);
        InMemoryMarkdownIndexingStateStore store = new InMemoryMarkdownIndexingStateStore();
        TrackingMarkdownChunkIndexer tracker = new TrackingMarkdownChunkIndexer(
                new CompositeMarkdownChunkIndexer(List.of(dense, sparse)), store);
        MarkdownDocument document = MarkdownDocument.create("java/publish.md", "内容");
        List<MarkdownChunk> chunks = List.of(new MarkdownChunk(
                document.source(), 0, 0, 1, List.of("Java"), "内容"));

        tracker.index(document, chunks, "attempt-1");

        var build = org.mockito.ArgumentCaptor.forClass(MarkdownIndexBuild.class);
        verify(dense).buildGeneration(same(document), same(chunks), build.capture());
        verify(sparse).buildGeneration(same(document), same(chunks), same(build.getValue()));
        assertEquals(build.getValue().generation(),
                store.find(document.source(), document.version()).orElseThrow().publishedGeneration());
    }

    @Test
    void staleAttemptCannotPublishAfterANewerAttemptTakesOwnership() {
        GenerationAwareMarkdownChunkIndexer delegate = mock(GenerationAwareMarkdownChunkIndexer.class);
        InMemoryMarkdownIndexingStateStore store = new InMemoryMarkdownIndexingStateStore();
        TrackingMarkdownChunkIndexer tracker = new TrackingMarkdownChunkIndexer(delegate, store);
        MarkdownDocument document = MarkdownDocument.create("java/stale.md", "内容");
        List<MarkdownChunk> chunks = List.of(new MarkdownChunk(
                document.source(), 0, 0, 1, List.of("Java"), "内容"));
        store.markReady(document, 1);
        doAnswer(invocation -> {
            store.beginBuild(document, chunks.size(),
                    new MarkdownIndexBuild("new-generation", "attempt-2"));
            return null;
        }).when(delegate).verifyGeneration(same(document), same(chunks), any());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> tracker.rebuild(document, chunks, "attempt-1"));

        assertEquals("索引构建任务已被更新代次替代，禁止发布旧结果", exception.getMessage());
        MarkdownIndexingState state = store.find(document.source(), document.version()).orElseThrow();
        assertEquals(MarkdownIndexPublication.LEGACY_GENERATION, state.publishedGeneration());
        assertEquals("new-generation", state.buildingGeneration());
        assertEquals("attempt-2", state.attemptId());
        verify(delegate).deleteGeneration(same(document), any());
    }
}
