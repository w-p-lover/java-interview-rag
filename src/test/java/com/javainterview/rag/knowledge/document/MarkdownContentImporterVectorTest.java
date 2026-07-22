package com.javainterview.rag.knowledge.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MarkdownContentImporterVectorTest {

    private final MarkdownChunkIndexer chunkIndexer = mock(MarkdownChunkIndexer.class);
    private final MarkdownContentImporter importer = new MarkdownContentImporter(
            new InMemoryMarkdownImportRegistry(), 100, chunkIndexer);

    @Test
    void indexesChunksOnlyForTheFirstImport() {
        MarkdownImportResult first = importer.importContent("java/basic.md", "# Java\n集合");
        MarkdownImportResult duplicate = importer.importContent("java/basic.md", "# Java\n集合");

        assertTrue(first.imported());
        assertEquals(1, first.chunks().size());
        assertFalse(duplicate.imported());
        verify(chunkIndexer).index(first.document(), first.chunks());
    }

    @Test
    void removesRegistrationWhenVectorIndexingFailsSoTheDocumentCanRetry() {
        doThrow(new IllegalStateException("Qdrant unavailable"))
                .doNothing()
                .when(chunkIndexer).index(any(MarkdownDocument.class), any());

        assertThrows(IllegalStateException.class,
                () -> importer.importContent("java/retry.md", "# Java\n重试"));

        MarkdownImportResult retried = importer.importContent("java/retry.md", "# Java\n重试");

        assertTrue(retried.imported());
        verify(chunkIndexer, times(2)).index(any(MarkdownDocument.class), any());
    }
}
