package com.javainterview.rag.knowledge.document;

import java.util.HashSet;
import java.util.Set;

public final class InMemoryMarkdownImportRegistry implements MarkdownImportRegistry {

    private final Set<ImportKey> registeredImports = new HashSet<>();

    @Override
    public boolean register(MarkdownDocument document) {
        return registeredImports.add(new ImportKey(document.source(), document.version()));
    }

    @Override
    public boolean unregister(MarkdownDocument document) {
        return registeredImports.remove(new ImportKey(document.source(), document.version()));
    }

    private record ImportKey(String source, String version) {
    }
}
