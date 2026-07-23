package com.javainterview.rag.knowledge.document;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void removesOtherVersionsAfterIndexingTheChangedContent() {
        MarkdownImportResult original = importer.importContent("java/basic.md", "# Java\n原内容");
        MarkdownImportResult changed = importer.importContent("java/basic.md", "# Java\n新内容");

        var orderedCalls = inOrder(chunkIndexer);
        orderedCalls.verify(chunkIndexer).index(original.document(), original.chunks());
        orderedCalls.verify(chunkIndexer).deleteOtherVersions(original.document());
        orderedCalls.verify(chunkIndexer).index(changed.document(), changed.chunks());
        orderedCalls.verify(chunkIndexer).deleteOtherVersions(changed.document());
    }

    @Test
    void removesRegistrationWhenOldVectorCleanupFailsSoTheVersionCanRetry() {
        doThrow(new IllegalStateException("Qdrant unavailable"))
                .doNothing()
                .when(chunkIndexer).deleteOtherVersions(any(MarkdownDocument.class));

        assertThrows(IllegalStateException.class,
                () -> importer.importContent("java/retry-cleanup.md", "# Java\n重试"));

        MarkdownImportResult retried = importer.importContent(
                "java/retry-cleanup.md", "# Java\n重试");

        assertTrue(retried.imported());
        verify(chunkIndexer, times(2)).index(any(MarkdownDocument.class), any());
        verify(chunkIndexer, times(2)).deleteOtherVersions(any(MarkdownDocument.class));
    }

    @Test
    void rebuildsARegisteredHistoricalVersionWithoutChangingRegistration() {
        InMemoryMarkdownImportRegistry registry = new InMemoryMarkdownImportRegistry();
        MarkdownChunkIndexer indexer = mock(MarkdownChunkIndexer.class);
        MarkdownContentImporter sourceImporter = new MarkdownContentImporter(registry, 100, indexer);
        MarkdownImportResult original = sourceImporter.importContent(
                "java/rebuild.md", "# Java\n原内容");
        sourceImporter.importContent("java/rebuild.md", "# Java\n新内容");

        MarkdownRebuildResult rebuilt = sourceImporter.rebuildContent(
                "java/rebuild.md", "# Java\n原内容");

        assertEquals(original.document().source(), rebuilt.document().source());
        assertEquals(original.document().content(), rebuilt.document().content());
        assertEquals(original.document().version(), rebuilt.document().version());
        assertEquals(original.chunks(), rebuilt.chunks());
        assertTrue(registry.isRegistered(original.document()));
        verify(indexer).rebuild(rebuilt.document(), rebuilt.chunks());
    }

    @Test
    void rejectsRebuildForAnUnregisteredVersionBeforeIndexing() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> importer.rebuildContent("java/missing.md", "# Java\n未导入"));

        assertEquals("文档版本尚未导入，不能重建", exception.getMessage());
        verify(chunkIndexer, never()).rebuild(any(), any());
    }

    @Test
    void keepsRegistrationWhenRebuildFailsSoTheVersionCanRetry() {
        InMemoryMarkdownImportRegistry registry = new InMemoryMarkdownImportRegistry();
        MarkdownChunkIndexer indexer = mock(MarkdownChunkIndexer.class);
        MarkdownContentImporter sourceImporter = new MarkdownContentImporter(registry, 100, indexer);
        MarkdownImportResult imported = sourceImporter.importContent(
                "java/retry-rebuild.md", "# Java\n重试");
        doThrow(new IllegalStateException("Qdrant unavailable"))
                .doNothing()
                .when(indexer).rebuild(any(MarkdownDocument.class), any());

        assertThrows(IllegalStateException.class,
                () -> sourceImporter.rebuildContent("java/retry-rebuild.md", "# Java\n重试"));
        MarkdownRebuildResult retried = sourceImporter.rebuildContent(
                "java/retry-rebuild.md", "# Java\n重试");

        assertEquals(imported.document().source(), retried.document().source());
        assertEquals(imported.document().version(), retried.document().version());
        assertTrue(registry.isRegistered(imported.document()));
        verify(indexer, times(2)).rebuild(any(MarkdownDocument.class), any());
    }

    @Test
    void deletesVectorsBeforeRemovingAllRegistrationsForTheSource() {
        MarkdownImportRegistry registry = mock(MarkdownImportRegistry.class);
        MarkdownChunkIndexer indexer = mock(MarkdownChunkIndexer.class);
        MarkdownContentImporter sourceImporter = new MarkdownContentImporter(registry, 100, indexer);
        when(registry.unregisterBySource("java/delete.md")).thenReturn(true);

        assertTrue(sourceImporter.deleteBySource("java/delete.md"));

        var orderedCalls = inOrder(indexer, registry);
        orderedCalls.verify(indexer).deleteBySource("java/delete.md");
        orderedCalls.verify(registry).unregisterBySource("java/delete.md");
    }
}
