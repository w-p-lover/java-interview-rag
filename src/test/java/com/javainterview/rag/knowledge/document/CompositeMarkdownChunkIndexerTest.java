package com.javainterview.rag.knowledge.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CompositeMarkdownChunkIndexerTest {

    @Test
    void attemptsCleanupOnEveryStoreAndAggregatesFailures() {
        MarkdownChunkIndexer denseIndexer = mock(MarkdownChunkIndexer.class);
        MarkdownChunkIndexer sparseIndexer = mock(MarkdownChunkIndexer.class);
        MarkdownDocument document = MarkdownDocument.create("java/cleanup.md", "清理");
        doThrow(new IllegalStateException("Dense cleanup failed"))
                .when(denseIndexer).deleteVersion(document);
        doThrow(new IllegalArgumentException("Sparse cleanup failed"))
                .when(sparseIndexer).deleteVersion(document);
        CompositeMarkdownChunkIndexer indexer = new CompositeMarkdownChunkIndexer(
                List.of(denseIndexer, sparseIndexer));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> indexer.deleteVersion(document));

        verify(denseIndexer).deleteVersion(document);
        verify(sparseIndexer).deleteVersion(document);
        assertEquals(1, exception.getSuppressed().length);
        assertEquals("Sparse cleanup failed", exception.getSuppressed()[0].getMessage());
    }
}
