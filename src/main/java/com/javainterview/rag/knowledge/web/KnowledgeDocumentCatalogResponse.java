package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.document.KnowledgeDocumentSummary;

import java.util.List;

public record KnowledgeDocumentCatalogResponse(
        int sourceCount,
        List<KnowledgeDocumentCatalogItem> documents
) {

    static KnowledgeDocumentCatalogResponse from(List<KnowledgeDocumentSummary> documents) {
        List<KnowledgeDocumentCatalogItem> items = documents.stream()
                .map(KnowledgeDocumentCatalogItem::from)
                .toList();
        return new KnowledgeDocumentCatalogResponse(items.size(), items);
    }
}
