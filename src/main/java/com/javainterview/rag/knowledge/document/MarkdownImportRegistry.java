package com.javainterview.rag.knowledge.document;

import java.util.List;

public interface MarkdownImportRegistry {

    boolean register(MarkdownDocument document);

    boolean isRegistered(MarkdownDocument document);

    boolean unregister(MarkdownDocument document);

    boolean unregisterBySource(String source);

    List<KnowledgeDocumentSummary> findCurrentDocuments();
}
