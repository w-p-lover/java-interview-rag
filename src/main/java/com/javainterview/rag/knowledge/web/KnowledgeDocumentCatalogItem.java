package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.document.KnowledgeDocumentSummary;

import java.time.LocalDateTime;

public record KnowledgeDocumentCatalogItem(
        String source,
        String version,
        int contentLength,
        LocalDateTime registeredAt
) {

    static KnowledgeDocumentCatalogItem from(KnowledgeDocumentSummary document) {
        return new KnowledgeDocumentCatalogItem(
                document.source(), document.version(), document.contentLength(), document.registeredAt());
    }
}
