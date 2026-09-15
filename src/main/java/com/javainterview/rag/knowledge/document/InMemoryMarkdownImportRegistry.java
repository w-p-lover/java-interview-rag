package com.javainterview.rag.knowledge.document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InMemoryMarkdownImportRegistry implements MarkdownImportRegistry {

    private final Map<ImportKey, KnowledgeDocumentSummary> registeredImports = new LinkedHashMap<>();
    private final Map<ImportKey, KnowledgeDocumentSnapshot> snapshots = new LinkedHashMap<>();
    private final Map<String, ImportKey> currentImports = new LinkedHashMap<>();

    @Override
    public boolean register(MarkdownDocument document, KnowledgeDocumentOutline outline,
                            List<MarkdownSection> sections) {
        ImportKey key = new ImportKey(document.source(), document.version());
        if (registeredImports.containsKey(key)) {
            return false;
        }
        registeredImports.put(key, new KnowledgeDocumentSummary(
                document.source(), document.version(), document.content().length(), LocalDateTime.now(), outline));
        snapshots.put(key, new KnowledgeDocumentSnapshot(document, sections));
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
        snapshots.remove(key);
        if (key.equals(currentImports.get(document.source()))) {
            restorePreviousCurrent(document.source());
        }
        return true;
    }

    @Override
    public boolean unregisterBySource(String source) {
        boolean removed = registeredImports.keySet().removeIf(importKey -> importKey.source().equals(source));
        snapshots.keySet().removeIf(importKey -> importKey.source().equals(source));
        currentImports.remove(source);
        return removed;
    }

    @Override
    public void updateOutline(MarkdownDocument document, KnowledgeDocumentOutline outline) {
        ImportKey key = new ImportKey(document.source(), document.version());
        KnowledgeDocumentSummary current = registeredImports.get(key);
        if (current == null) {
            throw new IllegalArgumentException("文档版本尚未登记，不能更新章节目录");
        }
        registeredImports.put(key, new KnowledgeDocumentSummary(
                current.source(), current.version(), current.contentLength(), current.registeredAt(), outline));
    }

    @Override
    public List<KnowledgeDocumentSummary> findCurrentDocuments() {
        return currentImports.values().stream()
                .map(registeredImports::get)
                .sorted(Comparator.comparing(KnowledgeDocumentSummary::registeredAt).reversed()
                        .thenComparing(KnowledgeDocumentSummary::source))
                .toList();
    }

    @Override
    public List<KnowledgeDocumentSummary> findVersionsBySource(String source) {
        List<KnowledgeDocumentSummary> versions = new ArrayList<>();
        registeredImports.forEach((key, summary) -> {
            if (key.source().equals(source)) {
                versions.add(0, summary);
            }
        });
        ImportKey current = currentImports.get(source);
        versions.sort(Comparator
                .comparing((KnowledgeDocumentSummary summary) -> current != null
                        && summary.version().equals(current.version())).reversed()
                .thenComparing(KnowledgeDocumentSummary::registeredAt, Comparator.reverseOrder()));
        return List.copyOf(versions);
    }

    @Override
    public java.util.Optional<KnowledgeDocumentSnapshot> findSnapshot(String source, String version) {
        return java.util.Optional.ofNullable(snapshots.get(new ImportKey(source, version)));
    }

    @Override
    public boolean activate(String source, String version) {
        ImportKey key = new ImportKey(source, version);
        if (!registeredImports.containsKey(key)) {
            return false;
        }
        currentImports.put(source, key);
        return true;
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
