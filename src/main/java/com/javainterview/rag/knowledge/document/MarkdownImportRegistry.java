package com.javainterview.rag.knowledge.document;

import java.util.List;

public interface MarkdownImportRegistry {

    boolean register(MarkdownDocument document);

    boolean register(MarkdownDocument document, KnowledgeDocumentOutline outline);

    boolean isRegistered(MarkdownDocument document);

    boolean unregister(MarkdownDocument document);

    boolean unregisterBySource(String source);

    void updateOutline(MarkdownDocument document, KnowledgeDocumentOutline outline);

    List<KnowledgeDocumentSummary> findCurrentDocuments();

    List<KnowledgeDocumentSummary> findVersionsBySource(String source);
}
