package com.javainterview.rag.knowledge.document;

import java.time.LocalDateTime;

public record KnowledgeDocumentSummary(
        String source,
        String version,
        int contentLength,
        LocalDateTime registeredAt,
        KnowledgeDocumentOutline outline,
        String fileId
) {
    public KnowledgeDocumentSummary(String source, String version, int contentLength,
                                    LocalDateTime registeredAt, KnowledgeDocumentOutline outline) {
        this(source, version, contentLength, registeredAt, outline, null);
    }
}
