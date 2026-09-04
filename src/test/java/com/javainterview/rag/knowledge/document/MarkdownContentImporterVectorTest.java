package com.javainterview.rag.knowledge.document;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
    void sendsMarkdownCodeAndTableChunksToTheExistingIndexer() {
        MarkdownImportResult result = importer.importContent("java/structured.md", """
                # Java
                ```java
                HashMap<String, Integer> counts = new HashMap<>();
                ```

                | 类型 | 用途 |
                | --- | --- |
                | HashMap | 映射 |
                """);

        assertEquals(List.of(KnowledgeBlockType.CODE, KnowledgeBlockType.TABLE),
                result.chunks().stream().map(MarkdownChunk::blockType).toList());
        verify(chunkIndexer).index(result.document(), result.chunks());
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
        verify(chunkIndexer).deleteVersion(any(MarkdownDocument.class));
    }

    @Test
    void compensatesEveryStoreWhenALaterIndexerFails() {
        MarkdownImportRegistry registry = new InMemoryMarkdownImportRegistry();
        MarkdownIndexingStateStore stateStore = new InMemoryMarkdownIndexingStateStore();
        MarkdownChunkIndexer denseIndexer = mock(MarkdownChunkIndexer.class);
        MarkdownChunkIndexer sparseIndexer = mock(MarkdownChunkIndexer.class);
        doThrow(new IllegalStateException("BM25 unavailable"))
                .doNothing()
                .when(sparseIndexer).index(any(MarkdownDocument.class), any());
        MarkdownChunkIndexer indexer = new TrackingMarkdownChunkIndexer(
                new CompositeMarkdownChunkIndexer(List.of(denseIndexer, sparseIndexer)), stateStore);
        MarkdownContentImporter sourceImporter = new MarkdownContentImporter(
                registry, 100, indexer, stateStore);

        assertThrows(IllegalStateException.class,
                () -> sourceImporter.importContent("java/partial.md", "# Java\n部分成功"));

        MarkdownDocument document = MarkdownDocument.create("java/partial.md", "# Java\n部分成功");
        assertFalse(registry.isRegistered(document));
        verify(denseIndexer).deleteVersion(argThat(candidate ->
                candidate.source().equals(document.source())
                        && candidate.version().equals(document.version())));
        verify(sparseIndexer).deleteVersion(argThat(candidate ->
                candidate.source().equals(document.source())
                        && candidate.version().equals(document.version())));

        MarkdownImportResult retried = sourceImporter.importContent(
                "java/partial.md", "# Java\n部分成功");
        assertTrue(retried.imported());
        assertEquals(MarkdownIndexingStatus.READY,
                stateStore.find(document.source(), document.version()).orElseThrow().status());
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
    void retainsIndexedVersionAndRetriesOnlyCleanupWhenOldVersionCleanupFails() {
        InMemoryMarkdownImportRegistry registry = new InMemoryMarkdownImportRegistry();
        InMemoryMarkdownIndexingStateStore stateStore = new InMemoryMarkdownIndexingStateStore();
        MarkdownChunkIndexer delegate = mock(MarkdownChunkIndexer.class);
        MarkdownChunkIndexer trackingIndexer = new TrackingMarkdownChunkIndexer(delegate, stateStore);
        MarkdownContentImporter sourceImporter = new MarkdownContentImporter(
                registry, 100, trackingIndexer, stateStore);
        doThrow(new IllegalStateException("Qdrant unavailable"))
                .doNothing()
                .when(delegate).deleteOtherVersions(any(MarkdownDocument.class));

        assertThrows(IllegalStateException.class,
                () -> sourceImporter.importContent("java/retry-cleanup.md", "# Java\n重试"));

        MarkdownDocument document = MarkdownDocument.create(
                "java/retry-cleanup.md", "# Java\n重试");
        assertTrue(registry.isRegistered(document));
        assertEquals(MarkdownIndexingStatus.FAILED,
                stateStore.find(document.source(), document.version()).orElseThrow().status());

        MarkdownImportResult retried = sourceImporter.importContent(
                "java/retry-cleanup.md", "# Java\n重试");

        assertFalse(retried.imported());
        verify(delegate, times(1)).index(any(MarkdownDocument.class), any());
        verify(delegate, times(2)).deleteOtherVersions(any(MarkdownDocument.class));
        assertEquals(MarkdownIndexingStatus.READY,
                stateStore.find(document.source(), document.version()).orElseThrow().status());
    }

    @Test
    void rebuildsTheCurrentVersionWithoutChangingRegistration() {
        InMemoryMarkdownImportRegistry registry = new InMemoryMarkdownImportRegistry();
        MarkdownChunkIndexer indexer = mock(MarkdownChunkIndexer.class);
        MarkdownContentImporter sourceImporter = new MarkdownContentImporter(registry, 100, indexer);
        sourceImporter.importContent("java/rebuild.md", "# Java\n原内容");
        MarkdownImportResult current = sourceImporter.importContent(
                "java/rebuild.md", "# Java\n新内容");

        MarkdownRebuildResult rebuilt = sourceImporter.rebuildContent(
                "java/rebuild.md", "# Java\n新内容");

        assertEquals(current.document().source(), rebuilt.document().source());
        assertEquals(current.document().content(), rebuilt.document().content());
        assertEquals(current.document().version(), rebuilt.document().version());
        assertEquals(current.chunks(), rebuilt.chunks());
        assertTrue(registry.isRegistered(current.document()));
        verify(indexer).rebuild(rebuilt.document(), rebuilt.chunks());
    }

    @Test
    void rejectsHistoricalVersionBeforeRebuildingVectors() {
        InMemoryMarkdownImportRegistry registry = new InMemoryMarkdownImportRegistry();
        MarkdownChunkIndexer indexer = mock(MarkdownChunkIndexer.class);
        MarkdownContentImporter sourceImporter = new MarkdownContentImporter(registry, 100, indexer);
        sourceImporter.importContent("java/rebuild.md", "# Java\n原内容");
        sourceImporter.importContent("java/rebuild.md", "# Java\n新内容");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> sourceImporter.rebuildContent("java/rebuild.md", "# Java\n原内容"));

        assertEquals("只能重建当前版本，历史版本不能直接恢复为检索版本", exception.getMessage());
        verify(indexer, never()).rebuild(any(), any());
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
    void updatesOutlineOnlyAfterVectorRebuildSucceeds() {
        MarkdownImportRegistry registry = mock(MarkdownImportRegistry.class);
        MarkdownChunkIndexer indexer = mock(MarkdownChunkIndexer.class);
        MarkdownContentImporter sourceImporter = new MarkdownContentImporter(registry, 100, indexer);
        MarkdownDocument document = MarkdownDocument.create("java/rebuild-order.md", "# Java\n内容");
        when(registry.findVersionsBySource(document.source())).thenReturn(List.of(
                new KnowledgeDocumentSummary(document.source(), document.version(),
                        document.content().length(), java.time.LocalDateTime.now(), null)));

        sourceImporter.rebuildContent(document.source(), document.content());

        var orderedCalls = inOrder(indexer, registry);
        orderedCalls.verify(indexer).rebuild(any(MarkdownDocument.class), any());
        orderedCalls.verify(registry).updateOutline(any(MarkdownDocument.class),
                any(KnowledgeDocumentOutline.class));
    }

    @Test
    void doesNotUpdateOutlineWhenVectorRebuildFails() {
        MarkdownImportRegistry registry = mock(MarkdownImportRegistry.class);
        MarkdownChunkIndexer indexer = mock(MarkdownChunkIndexer.class);
        MarkdownContentImporter sourceImporter = new MarkdownContentImporter(registry, 100, indexer);
        MarkdownDocument document = MarkdownDocument.create("java/rebuild-failure.md", "# Java\n内容");
        when(registry.findVersionsBySource(document.source())).thenReturn(List.of(
                new KnowledgeDocumentSummary(document.source(), document.version(),
                        document.content().length(), java.time.LocalDateTime.now(), null)));
        doThrow(new IllegalStateException("Qdrant unavailable"))
                .when(indexer).rebuild(any(MarkdownDocument.class), any());

        assertThrows(IllegalStateException.class,
                () -> sourceImporter.rebuildContent(document.source(), document.content()));

        verify(registry, never()).updateOutline(any(), any());
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
