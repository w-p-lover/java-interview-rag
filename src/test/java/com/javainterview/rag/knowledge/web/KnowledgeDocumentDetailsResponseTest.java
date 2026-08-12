package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.document.KnowledgeDocumentSummary;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNull;

class KnowledgeDocumentDetailsResponseTest {

    @Test
    void keepsNullOutlineForVersionsRegisteredBeforeTheOutlineMigration() {
        KnowledgeDocumentSummary legacyVersion = new KnowledgeDocumentSummary(
                "java/legacy.md", "version-1", 10, LocalDateTime.now(), null);

        KnowledgeDocumentDetailsResponse response = KnowledgeDocumentDetailsResponse.from(
                List.of(legacyVersion));

        assertNull(response.outline());
    }
}
