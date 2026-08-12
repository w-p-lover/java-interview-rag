package com.javainterview.rag.knowledge.document;

import java.time.LocalDateTime;

public record KnowledgeDocumentSummary(
        String source,
        String version,
        int contentLength,
        LocalDateTime registeredAt,
        KnowledgeDocumentOutline outline
) {
}
