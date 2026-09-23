package com.javainterview.rag.knowledge.document;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.List;

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

    @Override
    public synchronized void beginBuild(MarkdownDocument document, int chunkCount,
                                        MarkdownIndexBuild build) {
        Objects.requireNonNull(build, "索引构建信息不能为空");
        MarkdownIndexingState existing = states.get(new Key(document.source(), document.version()));
        int publishedChunkCount = existing == null ? 0 : existing.chunkCount();
        String publishedGeneration = existing == null ? null : existing.publishedGeneration();
        states.put(new Key(document.source(), document.version()),
                new MarkdownIndexingState(document.source(), document.version(), publishedChunkCount,
                        MarkdownIndexingStatus.INDEXING, null, publishedGeneration,
                        build.generation(), chunkCount, build.attemptId()));
    }

    @Override
    public synchronized boolean publishBuild(MarkdownDocument document, int chunkCount,
                                             MarkdownIndexBuild build) {
        Key key = new Key(document.source(), document.version());
        MarkdownIndexingState existing = states.get(key);
        if (!owns(existing, build)) {
            return false;
        }
        states.replaceAll((candidate, state) -> candidate.source().equals(document.source())
                && !candidate.equals(key)
                ? new MarkdownIndexingState(state.source(), state.documentVersion(), state.chunkCount(),
                state.status(), state.errorType(), null, state.buildingGeneration(),
                state.buildingChunkCount(), state.attemptId())
                : state);
        states.put(key, new MarkdownIndexingState(document.source(), document.version(), chunkCount,
                MarkdownIndexingStatus.READY, null, build.generation(), null, null, null));
        return true;
    }

    @Override
    public synchronized void failBuild(MarkdownDocument document, int chunkCount,
                                       MarkdownIndexBuild build, RuntimeException exception,
                                       boolean temporaryCleaned) {
        Objects.requireNonNull(exception, "索引异常不能为空");
        Key key = new Key(document.source(), document.version());
        MarkdownIndexingState existing = states.get(key);
        if (!owns(existing, build)) {
            return;
        }
        states.put(key, new MarkdownIndexingState(document.source(), document.version(),
                existing.chunkCount(), MarkdownIndexingStatus.FAILED,
                exception.getClass().getSimpleName(), existing.publishedGeneration(),
                temporaryCleaned ? null : build.generation(),
                temporaryCleaned ? null : chunkCount, null));
    }

    @Override
    public synchronized List<MarkdownIndexPublication> findPublished() {
        return states.values().stream()
                .filter(state -> state.publishedGeneration() != null)
                .map(state -> new MarkdownIndexPublication(state.source(), state.documentVersion(),
                        state.publishedGeneration(), state.buildingGeneration()))
                .toList();
    }

    private void put(MarkdownDocument document, int chunkCount,
                     MarkdownIndexingStatus status, String errorType) {
        Objects.requireNonNull(document, "Markdown 文档不能为空");
        MarkdownIndexingState existing = states.get(new Key(document.source(), document.version()));
        String publishedGeneration = status == MarkdownIndexingStatus.READY
                ? (existing != null && existing.publishedGeneration() != null
                ? existing.publishedGeneration() : MarkdownIndexPublication.LEGACY_GENERATION)
                : existing == null ? null : existing.publishedGeneration();
        states.put(new Key(document.source(), document.version()),
                new MarkdownIndexingState(document.source(), document.version(), chunkCount,
                        status, errorType, publishedGeneration,
                        null, null, null));
    }

    private boolean owns(MarkdownIndexingState state, MarkdownIndexBuild build) {
        return state != null
                && Objects.equals(state.buildingGeneration(), build.generation())
                && Objects.equals(state.attemptId(), build.attemptId());
    }

    private record Key(String source, String version) {
    }
}
