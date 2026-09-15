package com.javainterview.rag.knowledge.document;

import java.util.List;
import java.util.Optional;

public interface MarkdownImportRegistry {

    default boolean register(MarkdownDocument document) {
        return register(document, null, List.of());
    }

    default boolean register(MarkdownDocument document, KnowledgeDocumentOutline outline) {
        return register(document, outline, List.of());
    }

    boolean register(MarkdownDocument document, KnowledgeDocumentOutline outline,
                     List<MarkdownSection> sections);

    boolean isRegistered(MarkdownDocument document);

    boolean unregister(MarkdownDocument document);

    boolean unregisterBySource(String source);

    void updateOutline(MarkdownDocument document, KnowledgeDocumentOutline outline);

    List<KnowledgeDocumentSummary> findCurrentDocuments();

    List<KnowledgeDocumentSummary> findVersionsBySource(String source);

    Optional<KnowledgeDocumentSnapshot> findSnapshot(String source, String version);

    default boolean hasSnapshot(String source, String version) {
        return findSnapshot(source, version).isPresent();
    }

    boolean activate(String source, String version);
}
