package com.javainterview.rag.knowledge.persistence;

import com.javainterview.rag.knowledge.document.MarkdownDocument;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MySqlMarkdownImportRegistryTest {

    private final KnowledgeDocumentVersionMapper mapper = mock(KnowledgeDocumentVersionMapper.class);
    private final MySqlMarkdownImportRegistry registry = new MySqlMarkdownImportRegistry(mapper);

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
                "resume/backend.md", "version-1", 7);
        when(mapper.selectCurrentVersions()).thenReturn(List.of(entity));

        var documents = registry.findCurrentDocuments();

        assertEquals(1, documents.size());
        assertEquals(entity.getSource(), documents.get(0).source());
        assertEquals(entity.getVersion(), documents.get(0).version());
        assertEquals(entity.getContentLength(), documents.get(0).contentLength());
        assertEquals(entity.getCreatedAt(), documents.get(0).registeredAt());
    }

    @Test
    void listsAllVersionsForTheExactSource() {
        KnowledgeDocumentVersionEntity latest = KnowledgeDocumentVersionEntity.create(
                "resume/backend.md", "version-2", 8);
        KnowledgeDocumentVersionEntity original = KnowledgeDocumentVersionEntity.create(
                "resume/backend.md", "version-1", 7);
        when(mapper.selectVersionsBySource("resume/backend.md")).thenReturn(List.of(latest, original));

        var versions = registry.findVersionsBySource("resume/backend.md");

        assertEquals(List.of("version-2", "version-1"),
                versions.stream().map(version -> version.version()).toList());
        verify(mapper).selectVersionsBySource("resume/backend.md");
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

        assertEquals("SELECT COUNT(*) FROM t_knowledge_document_version "
                        + "WHERE BINARY source = BINARY #{source} AND version = #{version}",
                registeredSelect.value()[0]);
        assertEquals("DELETE FROM t_knowledge_document_version "
                + "WHERE BINARY source = BINARY #{source} AND version = #{version}",
                versionDelete.value()[0]);
        assertEquals("DELETE FROM t_knowledge_document_version "
                + "WHERE BINARY source = BINARY #{source}", sourceDelete.value()[0]);
        assertEquals("SELECT current_version.id, current_version.source, current_version.version, "
                + "current_version.content_length, current_version.created_at "
                + "FROM t_knowledge_document_version current_version "
                + "LEFT JOIN t_knowledge_document_version newer_version "
                + "ON BINARY newer_version.source = BINARY current_version.source "
                + "AND newer_version.id > current_version.id "
                + "WHERE newer_version.id IS NULL "
                + "ORDER BY current_version.created_at DESC, current_version.id DESC",
                currentVersionsSelect.value()[0]);
        assertEquals("SELECT id, source, version, content_length, created_at "
                        + "FROM t_knowledge_document_version "
                        + "WHERE BINARY source = BINARY #{source} "
                        + "ORDER BY id DESC",
                sourceVersionsSelect.value()[0]);
    }
}
