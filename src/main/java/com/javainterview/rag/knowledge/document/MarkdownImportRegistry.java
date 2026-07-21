package com.javainterview.rag.knowledge.document;

import java.util.HashSet;
import java.util.Set;

public final class MarkdownImportRegistry {

    private final Set<ImportKey> registeredImports = new HashSet<>();

    public boolean register(MarkdownDocument document) {
        return registeredImports.add(new ImportKey(document.source(), document.version()));
    }

    private record ImportKey(String source, String version) {
    }
}
