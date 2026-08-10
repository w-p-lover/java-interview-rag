package com.javainterview.rag.knowledge.document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InMemoryMarkdownImportRegistry implements MarkdownImportRegistry {

    private final Map<ImportKey, KnowledgeDocumentSummary> registeredImports = new LinkedHashMap<>();
    private final Map<String, ImportKey> currentImports = new LinkedHashMap<>();

    @Override
    public boolean register(MarkdownDocument document) {
        ImportKey key = new ImportKey(document.source(), document.version());
        if (registeredImports.containsKey(key)) {
            return false;
        }
        registeredImports.put(key, new KnowledgeDocumentSummary(
                document.source(), document.version(), document.content().length(), LocalDateTime.now()));
        currentImports.put(document.source(), key);
        return true;
    }

    @Override
    public boolean isRegistered(MarkdownDocument document) {
        return registeredImports.containsKey(new ImportKey(document.source(), document.version()));
    }

    @Override
    public boolean unregister(MarkdownDocument document) {
        ImportKey key = new ImportKey(document.source(), document.version());
        if (registeredImports.remove(key) == null) {
            return false;
        }
        if (key.equals(currentImports.get(document.source()))) {
            restorePreviousCurrent(document.source());
        }
        return true;
    }

    @Override
    public boolean unregisterBySource(String source) {
        boolean removed = registeredImports.keySet().removeIf(importKey -> importKey.source().equals(source));
        currentImports.remove(source);
        return removed;
    }

    @Override
    public List<KnowledgeDocumentSummary> findCurrentDocuments() {
        return currentImports.values().stream()
                .map(registeredImports::get)
                .sorted(Comparator.comparing(KnowledgeDocumentSummary::registeredAt).reversed()
                        .thenComparing(KnowledgeDocumentSummary::source))
                .toList();
    }

    private void restorePreviousCurrent(String source) {
        List<ImportKey> keys = new ArrayList<>(registeredImports.keySet());
        for (int index = keys.size() - 1; index >= 0; index--) {
            ImportKey candidate = keys.get(index);
            if (candidate.source().equals(source)) {
                currentImports.put(source, candidate);
                return;
            }
        }
        currentImports.remove(source);
    }

    private record ImportKey(String source, String version) {
    }
}
