package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.document.KnowledgeDocumentSummary;

import java.time.LocalDateTime;

public record KnowledgeDocumentVersionItem(
        String version,
        int contentLength,
        LocalDateTime registeredAt,
        boolean current,
        boolean restorable
) {

    static KnowledgeDocumentVersionItem from(KnowledgeDocumentSummary version, boolean current,
                                              boolean restorable) {
        return new KnowledgeDocumentVersionItem(
                version.version(), version.contentLength(), version.registeredAt(), current, restorable);
    }
}
