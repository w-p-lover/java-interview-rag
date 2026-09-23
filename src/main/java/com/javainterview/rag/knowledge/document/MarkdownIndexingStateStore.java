package com.javainterview.rag.knowledge.document;

import java.util.Optional;
import java.util.List;

public interface MarkdownIndexingStateStore {

    void markIndexing(MarkdownDocument document, int chunkCount);

    void markReady(MarkdownDocument document, int chunkCount);

    void markFailed(MarkdownDocument document, int chunkCount, RuntimeException exception);

    Optional<MarkdownIndexingState> find(String source, String documentVersion);

    void removeBySource(String source);

    void beginBuild(MarkdownDocument document, int chunkCount, MarkdownIndexBuild build);

    boolean publishBuild(MarkdownDocument document, int chunkCount, MarkdownIndexBuild build);

    void failBuild(MarkdownDocument document, int chunkCount, MarkdownIndexBuild build,
                   RuntimeException exception, boolean temporaryCleaned);

    List<MarkdownIndexPublication> findPublished();
}
