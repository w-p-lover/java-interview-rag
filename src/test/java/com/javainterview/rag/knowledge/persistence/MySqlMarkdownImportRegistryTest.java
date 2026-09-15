package com.javainterview.rag.knowledge.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.knowledge.document.KnowledgeDocumentOutline;
import com.javainterview.rag.knowledge.document.KnowledgeDocumentOutlineNode;
import com.javainterview.rag.knowledge.document.KnowledgeBlockType;
import com.javainterview.rag.knowledge.document.MarkdownDocument;
import com.javainterview.rag.knowledge.document.MarkdownSection;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MySqlMarkdownImportRegistryTest {

    private final KnowledgeDocumentVersionMapper mapper = mock(KnowledgeDocumentVersionMapper.class);
    private final MySqlMarkdownImportRegistry registry = new MySqlMarkdownImportRegistry(
            mapper, new ObjectMapper());

    @Test
    void registersDocumentVersionWhenInsertSucceeds() {
        MarkdownDocument document = MarkdownDocument.create("resume/backend.md", "Java 后端");
        when(mapper.insert(any(KnowledgeDocumentVersionEntity.class))).thenReturn(1);

        assertTrue(registry.register(document));

        var entityCaptor = org.mockito.ArgumentCaptor.forClass(KnowledgeDocumentVersionEntity.class);
        verify(mapper).insert(entityCaptor.capture());
        KnowledgeDocumentVersionEntity entity = entityCaptor.getValue();
        assertEquals(document.source(), entity.getSource());
        assertEquals(document.version(), entity.getVersion());
        assertEquals(document.content().length(), entity.getContentLength());
        assertNull(entity.getOutlineJson());
        assertEquals(document.content(), entity.getContent());
        assertEquals("[]", entity.getSectionsJson());
    }

    @Test
    void storesAndReadsOutlineJson() {
        MarkdownDocument document = MarkdownDocument.create("resume/backend.md", "# Java\n后端");
        KnowledgeDocumentOutline outline = new KnowledgeDocumentOutline(
                1, 1, List.of(new KnowledgeDocumentOutlineNode("Java", 1, 0, 1, List.of())));
        when(mapper.insert(any(KnowledgeDocumentVersionEntity.class))).thenReturn(1);

        assertTrue(registry.register(document, outline));

        var entityCaptor = org.mockito.ArgumentCaptor.forClass(KnowledgeDocumentVersionEntity.class);
        verify(mapper).insert(entityCaptor.capture());
        KnowledgeDocumentVersionEntity stored = entityCaptor.getValue();
        KnowledgeDocumentVersionEntity loaded = KnowledgeDocumentVersionEntity.create(
                stored.getSource(), stored.getVersion(), stored.getContentLength(), stored.getOutlineJson());
        when(mapper.selectCurrentVersions()).thenReturn(List.of(loaded));
        assertEquals(outline, registry.findCurrentDocuments().get(0).outline());
    }

    @Test
    void storesAndReadsStructuredDocumentSnapshot() {
        MarkdownDocument document = MarkdownDocument.create("resume/backend.pdf", "# Java\n代码");
        List<MarkdownSection> sections = List.of(new MarkdownSection(
                document.source(), 0, 1, List.of("Java"), "代码", 2,
                KnowledgeBlockType.CODE));
        when(mapper.insert(any(KnowledgeDocumentVersionEntity.class))).thenReturn(1);

        assertTrue(registry.register(document, null, sections));

        var entityCaptor = org.mockito.ArgumentCaptor.forClass(KnowledgeDocumentVersionEntity.class);
        verify(mapper).insert(entityCaptor.capture());
        when(mapper.selectBySourceAndVersion(document.source(), document.version()))
                .thenReturn(entityCaptor.getValue());
        var snapshot = registry.findSnapshot(document.source(), document.version()).orElseThrow();
        assertEquals(document.content(), snapshot.document().content());
        assertEquals(sections, snapshot.sections());
    }

    @Test
    void returnsFalseForDuplicateSourceAndVersion() {
        when(mapper.insert(any(KnowledgeDocumentVersionEntity.class)))
                .thenThrow(new DuplicateKeyException("duplicate"));

        assertFalse(registry.register(MarkdownDocument.create("resume/backend.md", "Java 后端")));
    }

    @Test
    void propagatesDatabaseAvailabilityFailures() {
        when(mapper.insert(any(KnowledgeDocumentVersionEntity.class)))
                .thenThrow(new DataAccessResourceFailureException("unavailable"));

        assertThrows(DataAccessResourceFailureException.class,
                () -> registry.register(MarkdownDocument.create("resume/backend.md", "Java 后端")));
    }

    @Test
    void removesTheSameSourceAndVersionForFailedVectorIndexing() {
        MarkdownDocument document = MarkdownDocument.create("resume/backend.md", "Java 后端");
        when(mapper.deleteBySourceAndVersion(document.source(), document.version())).thenReturn(1);

        assertTrue(registry.unregister(document));

        verify(mapper).deleteBySourceAndVersion(document.source(), document.version());
    }

    @Test
    void removesAllVersionsForTheSameSource() {
        when(mapper.deleteBySource("resume/backend.md")).thenReturn(2);

        assertTrue(registry.unregisterBySource("resume/backend.md"));

        verify(mapper).deleteBySource("resume/backend.md");
    }

    @Test
    void findsRegisteredSourceAndVersion() {
        MarkdownDocument document = MarkdownDocument.create("resume/backend.md", "Java 后端");
        when(mapper.countBySourceAndVersion(document.source(), document.version())).thenReturn(1);

        assertTrue(registry.isRegistered(document));

        verify(mapper).countBySourceAndVersion(document.source(), document.version());
    }

    @Test
    void listsCurrentDocumentVersions() {
        KnowledgeDocumentVersionEntity entity = KnowledgeDocumentVersionEntity.create(
                "resume/backend.md", "version-1", 7, null);
        when(mapper.selectCurrentVersions()).thenReturn(List.of(entity));

        var documents = registry.findCurrentDocuments();

        assertEquals(1, documents.size());
        assertEquals(entity.getSource(), documents.get(0).source());
        assertEquals(entity.getVersion(), documents.get(0).version());
        assertEquals(entity.getContentLength(), documents.get(0).contentLength());
        assertEquals(entity.getCreatedAt(), documents.get(0).registeredAt());
        assertNull(documents.get(0).outline());
    }

    @Test
    void listsAllVersionsForTheExactSource() {
        KnowledgeDocumentVersionEntity latest = KnowledgeDocumentVersionEntity.create(
                "resume/backend.md", "version-2", 8, null);
        KnowledgeDocumentVersionEntity original = KnowledgeDocumentVersionEntity.create(
                "resume/backend.md", "version-1", 7, null);
        when(mapper.selectVersionsBySource("resume/backend.md")).thenReturn(List.of(latest, original));

        var versions = registry.findVersionsBySource("resume/backend.md");

        assertEquals(List.of("version-2", "version-1"),
                versions.stream().map(version -> version.version()).toList());
        verify(mapper).selectVersionsBySource("resume/backend.md");
    }

    @Test
    void updatesRegisteredOutline() {
        MarkdownDocument document = MarkdownDocument.create("resume/backend.md", "# Java\n后端");
        KnowledgeDocumentOutline outline = new KnowledgeDocumentOutline(
                1, 1, List.of(new KnowledgeDocumentOutlineNode("Java", 1, 0, 1, List.of())));
        when(mapper.updateOutline(any(), any(), any())).thenReturn(1);

        registry.updateOutline(document, outline);

        verify(mapper).updateOutline(document.source(), document.version(),
                "{\"headingCount\":1,\"chunkCount\":1,\"roots\":[{\"title\":\"Java\",\"level\":1,"
                        + "\"firstChunkOrder\":0,\"directChunkCount\":1,\"children\":[]}]}");
    }

    @Test
    void usesExactSourceMatchingForMapperStatements() throws NoSuchMethodException {
        Select registeredSelect = KnowledgeDocumentVersionMapper.class
                .getMethod("countBySourceAndVersion", String.class, String.class)
                .getAnnotation(Select.class);
        Delete versionDelete = KnowledgeDocumentVersionMapper.class
                .getMethod("deleteBySourceAndVersion", String.class, String.class)
                .getAnnotation(Delete.class);
        Delete sourceDelete = KnowledgeDocumentVersionMapper.class
                .getMethod("deleteBySource", String.class)
                .getAnnotation(Delete.class);
        Select currentVersionsSelect = KnowledgeDocumentVersionMapper.class
                .getMethod("selectCurrentVersions")
                .getAnnotation(Select.class);
        Select sourceVersionsSelect = KnowledgeDocumentVersionMapper.class
                .getMethod("selectVersionsBySource", String.class)
                .getAnnotation(Select.class);
        Update outlineUpdate = KnowledgeDocumentVersionMapper.class
                .getMethod("updateOutline", String.class, String.class, String.class)
                .getAnnotation(Update.class);
        Update activateUpdate = KnowledgeDocumentVersionMapper.class
                .getMethod("activate", String.class, String.class)
                .getAnnotation(Update.class);
        Select snapshotCountSelect = KnowledgeDocumentVersionMapper.class
                .getMethod("countSnapshotBySourceAndVersion", String.class, String.class)
                .getAnnotation(Select.class);

        assertEquals("SELECT COUNT(*) FROM t_knowledge_document_version "
                        + "WHERE BINARY source = BINARY #{source} AND version = #{version}",
                registeredSelect.value()[0]);
        assertEquals("DELETE FROM t_knowledge_document_version "
                + "WHERE BINARY source = BINARY #{source} AND version = #{version}",
                versionDelete.value()[0]);
        assertEquals("DELETE FROM t_knowledge_document_version "
                + "WHERE BINARY source = BINARY #{source}", sourceDelete.value()[0]);
        assertEquals("UPDATE t_knowledge_document_version SET outline_json = #{outlineJson} "
                + "WHERE BINARY source = BINARY #{source} AND version = #{version}",
                outlineUpdate.value()[0]);
        assertTrue(activateUpdate.value()[0].contains(
                "DATE_ADD(MAX(activated_at), INTERVAL 1 MICROSECOND)"));
        assertEquals("SELECT COUNT(*) FROM t_knowledge_document_version "
                        + "WHERE BINARY source = BINARY #{source} AND version = #{version} "
                        + "AND content IS NOT NULL AND sections_json IS NOT NULL",
                snapshotCountSelect.value()[0]);
        assertEquals("SELECT current_version.id, current_version.source, current_version.version, "
                + "current_version.content_length, current_version.outline_json, current_version.content, "
                + "current_version.sections_json, current_version.activated_at, current_version.created_at "
                + "FROM t_knowledge_document_version current_version "
                + "LEFT JOIN t_knowledge_document_version newer_version "
                + "ON BINARY newer_version.source = BINARY current_version.source "
                + "AND (newer_version.activated_at > current_version.activated_at "
                + "OR (newer_version.activated_at = current_version.activated_at AND newer_version.id > current_version.id)) "
                + "WHERE newer_version.id IS NULL "
                + "ORDER BY current_version.activated_at DESC, current_version.id DESC",
                currentVersionsSelect.value()[0]);
        assertEquals("SELECT id, source, version, content_length, outline_json, content, sections_json, activated_at, created_at "
                        + "FROM t_knowledge_document_version "
                        + "WHERE BINARY source = BINARY #{source} "
                        + "ORDER BY activated_at DESC, id DESC",
                sourceVersionsSelect.value()[0]);
    }
}
