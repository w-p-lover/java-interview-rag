package com.javainterview.rag.knowledge.document;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InMemoryMarkdownIndexingStateStore implements MarkdownIndexingStateStore {

    private final Map<Key, MarkdownIndexingState> states = new LinkedHashMap<>();

    @Override
    public synchronized void markIndexing(MarkdownDocument document, int chunkCount) {
        put(document, chunkCount, MarkdownIndexingStatus.INDEXING, null);
    }

    @Override
    public synchronized void markReady(MarkdownDocument document, int chunkCount) {
        put(document, chunkCount, MarkdownIndexingStatus.READY, null);
    }

    @Override
    public synchronized void markFailed(MarkdownDocument document, int chunkCount,
                                         RuntimeException exception) {
        Objects.requireNonNull(exception, "索引异常不能为空");
        put(document, chunkCount, MarkdownIndexingStatus.FAILED,
                exception.getClass().getSimpleName());
    }

    @Override
    public synchronized Optional<MarkdownIndexingState> find(String source, String documentVersion) {
        return Optional.ofNullable(states.get(new Key(source, documentVersion)));
    }

    @Override
    public synchronized void removeBySource(String source) {
        states.keySet().removeIf(key -> key.source().equals(source));
    }

    private void put(MarkdownDocument document, int chunkCount,
                     MarkdownIndexingStatus status, String errorType) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        states.put(new Key(document.source(), document.version()),
                new MarkdownIndexingState(document.source(), document.version(), chunkCount,
                        status, errorType));
    }

    private record Key(String source, String version) {
    }
}
