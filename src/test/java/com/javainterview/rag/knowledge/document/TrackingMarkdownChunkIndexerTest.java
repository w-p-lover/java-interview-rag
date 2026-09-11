package com.javainterview.rag.knowledge.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
}
