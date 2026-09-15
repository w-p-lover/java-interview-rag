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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
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
    void rebuildsCurrentVersionFromStoredSnapshot() {
        InMemoryMarkdownImportRegistry registry = new InMemoryMarkdownImportRegistry();
        MarkdownChunkIndexer indexer = mock(MarkdownChunkIndexer.class);
        MarkdownContentImporter sourceImporter = new MarkdownContentImporter(registry, 100, indexer);
        MarkdownImportResult current = sourceImporter.importContent(
                "java/repair.md", "# Java\n索引修复");

        MarkdownRebuildResult rebuilt = sourceImporter.rebuildCurrentSnapshot(current.document().source());

        assertEquals(current.document(), rebuilt.document());
        assertEquals(current.chunks(), rebuilt.chunks());
        verify(indexer).rebuild(current.document(), current.chunks());
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
    void rollsBackToStoredHistoricalSnapshotAndActivatesItAfterIndexing() {
        InMemoryMarkdownImportRegistry registry = new InMemoryMarkdownImportRegistry();
        MarkdownChunkIndexer indexer = mock(MarkdownChunkIndexer.class);
        MarkdownContentImporter sourceImporter = new MarkdownContentImporter(registry, 100, indexer);
        MarkdownImportResult original = sourceImporter.importContent("java/rollback.md", "# Java\n原内容");
        sourceImporter.importContent("java/rollback.md", "# Java\n新内容");

        MarkdownRebuildResult rollback = sourceImporter.rollbackToVersion(
                original.document().source(), original.document().version());

        assertEquals(original.document(), rollback.document());
        assertEquals(original.document().version(),
                registry.findVersionsBySource(original.document().source()).get(0).version());
        verify(indexer).rebuild(original.document(), original.chunks());
    }

    @Test
    void preservesStructuredPdfMetadataWhenRollingBack() {
        InMemoryMarkdownImportRegistry registry = new InMemoryMarkdownImportRegistry();
        MarkdownContentImporter sourceImporter = new MarkdownContentImporter(
                registry, 100, mock(MarkdownChunkIndexer.class));
        String source = "java/rollback.pdf";
        String originalContent = "# HashMap\n代码片段\n字段表格";
        List<MarkdownSection> sections = List.of(
                new MarkdownSection(source, 0, 1, List.of("HashMap"),
                        "代码片段", 2, KnowledgeBlockType.CODE),
                new MarkdownSection(source, 1, 1, List.of("HashMap"),
                        "字段表格", 3, KnowledgeBlockType.TABLE));
        MarkdownImportResult original = sourceImporter.importStructuredContent(
                source, originalContent, sections);
        sourceImporter.importContent(source, "# HashMap\n更新内容");

        MarkdownRebuildResult rollback = sourceImporter.rollbackToVersion(
                source, original.document().version());

        assertEquals(List.of(2, 3), rollback.chunks().stream().map(MarkdownChunk::pageNumber).toList());
        assertEquals(List.of(KnowledgeBlockType.CODE, KnowledgeBlockType.TABLE),
                rollback.chunks().stream().map(MarkdownChunk::blockType).toList());
        assertEquals(List.of(List.of("HashMap"), List.of("HashMap")),
                rollback.chunks().stream().map(MarkdownChunk::headingPath).toList());
    }

    @Test
    void keepsCurrentVersionWhenRollbackIndexingFails() {
        InMemoryMarkdownImportRegistry registry = new InMemoryMarkdownImportRegistry();
        MarkdownChunkIndexer indexer = mock(MarkdownChunkIndexer.class);
        MarkdownContentImporter sourceImporter = new MarkdownContentImporter(registry, 100, indexer);
        MarkdownImportResult original = sourceImporter.importContent("java/rollback-fail.md", "# Java\n原内容");
        MarkdownImportResult current = sourceImporter.importContent("java/rollback-fail.md", "# Java\n新内容");
        doThrow(new IllegalStateException("Qdrant unavailable"))
                .when(indexer).rebuild(any(MarkdownDocument.class), any());

        assertThrows(IllegalStateException.class, () -> sourceImporter.rollbackToVersion(
                original.document().source(), original.document().version()));

        assertEquals(current.document().version(),
                registry.findVersionsBySource(current.document().source()).get(0).version());
    }

    @Test
    void restoresCurrentSnapshotWhenHistoricalVersionActivationFails() {
        InMemoryMarkdownImportRegistry registry = spy(new InMemoryMarkdownImportRegistry());
        MarkdownChunkIndexer delegate = mock(MarkdownChunkIndexer.class);
        InMemoryMarkdownIndexingStateStore stateStore = new InMemoryMarkdownIndexingStateStore();
        MarkdownContentImporter sourceImporter = new MarkdownContentImporter(
                registry, 100, new TrackingMarkdownChunkIndexer(delegate, stateStore), stateStore);
        MarkdownImportResult original = sourceImporter.importContent(
                "java/rollback-activation.md", "# Java\n原内容");
        MarkdownImportResult current = sourceImporter.importContent(
                "java/rollback-activation.md", "# Java\n新内容");
        doReturn(false).when(registry)
                .activate(original.document().source(), original.document().version());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> sourceImporter.rollbackToVersion(
                        original.document().source(), original.document().version()));

        assertEquals("历史版本激活失败", exception.getMessage());
        assertEquals(current.document().version(),
                registry.findVersionsBySource(current.document().source()).get(0).version());
        assertEquals(MarkdownIndexingStatus.FAILED,
                stateStore.find(original.document().source(), original.document().version())
                        .orElseThrow().status());
        assertEquals(MarkdownIndexingStatus.READY,
                stateStore.find(current.document().source(), current.document().version())
                        .orElseThrow().status());
        var orderedCalls = inOrder(delegate);
        orderedCalls.verify(delegate).rebuild(original.document(), original.chunks());
        orderedCalls.verify(delegate).rebuild(current.document(), current.chunks());
    }

    @Test
    void keepsRollbackFailureAndMarksCurrentVersionFailedWhenCompensationAlsoFails() {
        InMemoryMarkdownImportRegistry registry = spy(new InMemoryMarkdownImportRegistry());
        MarkdownChunkIndexer delegate = mock(MarkdownChunkIndexer.class);
        InMemoryMarkdownIndexingStateStore stateStore = new InMemoryMarkdownIndexingStateStore();
        MarkdownContentImporter sourceImporter = new MarkdownContentImporter(
                registry, 100, new TrackingMarkdownChunkIndexer(delegate, stateStore), stateStore);
        MarkdownImportResult original = sourceImporter.importContent(
                "java/rollback-compensation.md", "# Java\n原内容");
        MarkdownImportResult current = sourceImporter.importContent(
                "java/rollback-compensation.md", "# Java\n新内容");
        doReturn(false).when(registry)
                .activate(original.document().source(), original.document().version());
        doNothing().doThrow(new IllegalStateException("原索引恢复失败"))
                .when(delegate).rebuild(any(MarkdownDocument.class), any());

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> sourceImporter.rollbackToVersion(
                        original.document().source(), original.document().version()));

        assertEquals("历史版本激活失败", exception.getMessage());
        assertEquals(1, exception.getSuppressed().length);
        assertEquals("原索引恢复失败", exception.getSuppressed()[0].getMessage());
        assertEquals(MarkdownIndexingStatus.FAILED,
                stateStore.find(current.document().source(), current.document().version())
                        .orElseThrow().status());
    }

    @Test
    void rejectsRollbackBeforeIndexingWhenCurrentVersionHasNoRecoverySnapshot() {
        MarkdownImportRegistry registry = mock(MarkdownImportRegistry.class);
        MarkdownChunkIndexer indexer = mock(MarkdownChunkIndexer.class);
        MarkdownContentImporter sourceImporter = new MarkdownContentImporter(registry, 100, indexer);
        MarkdownDocument current = MarkdownDocument.create("java/rollback-legacy.md", "# Java\n新内容");
        MarkdownDocument historical = MarkdownDocument.create("java/rollback-legacy.md", "# Java\n原内容");
        when(registry.findVersionsBySource(current.source())).thenReturn(List.of(
                new KnowledgeDocumentSummary(current.source(), current.version(), current.content().length(),
                        java.time.LocalDateTime.now(), null),
                new KnowledgeDocumentSummary(historical.source(), historical.version(), historical.content().length(),
                        java.time.LocalDateTime.now().minusDays(1), null)));
        when(registry.findSnapshot(historical.source(), historical.version()))
                .thenReturn(java.util.Optional.of(new KnowledgeDocumentSnapshot(
                        historical, new MarkdownHeadingParser().parse(historical))));
        when(registry.findSnapshot(current.source(), current.version()))
                .thenReturn(java.util.Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> sourceImporter.rollbackToVersion(historical.source(), historical.version()));

        assertEquals("当前版本没有可恢复快照，无法在回退失败时恢复索引，请重新上传当前版本原文件",
                exception.getMessage());
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
    void marksRebuildFailedWhenOutlineUpdateFailsAfterIndexing() {
        MarkdownImportRegistry registry = mock(MarkdownImportRegistry.class);
        MarkdownChunkIndexer indexer = mock(MarkdownChunkIndexer.class);
        MarkdownIndexingStateStore stateStore = new InMemoryMarkdownIndexingStateStore();
        MarkdownContentImporter sourceImporter = new MarkdownContentImporter(
                registry, 100, indexer, stateStore);
        MarkdownDocument document = MarkdownDocument.create("java/rebuild-outline-failure.md", "# Java\n内容");
        when(registry.findVersionsBySource(document.source())).thenReturn(List.of(
                new KnowledgeDocumentSummary(document.source(), document.version(),
                        document.content().length(), java.time.LocalDateTime.now(), null)));
        doThrow(new IllegalStateException("章节目录更新失败"))
                .when(registry).updateOutline(any(MarkdownDocument.class), any(KnowledgeDocumentOutline.class));

        assertThrows(IllegalStateException.class,
                () -> sourceImporter.rebuildContent(document.source(), document.content()));

        assertEquals(MarkdownIndexingStatus.FAILED,
                stateStore.find(document.source(), document.version()).orElseThrow().status());
        verify(indexer).rebuild(any(MarkdownDocument.class), any());
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
