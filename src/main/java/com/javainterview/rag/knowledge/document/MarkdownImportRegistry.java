package com.javainterview.rag.knowledge.document;

public interface MarkdownImportRegistry {

    boolean register(MarkdownDocument document);

    boolean isRegistered(MarkdownDocument document);

    boolean unregister(MarkdownDocument document);

    boolean unregisterBySource(String source);
}
