package com.javainterview.rag.knowledge.document;

public interface MarkdownImportRegistry {

    boolean register(MarkdownDocument document);

    boolean unregister(MarkdownDocument document);
}
