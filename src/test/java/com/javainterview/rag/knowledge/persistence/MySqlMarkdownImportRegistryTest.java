package com.javainterview.rag.knowledge.persistence;

import com.javainterview.rag.knowledge.document.MarkdownDocument;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;

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
}
