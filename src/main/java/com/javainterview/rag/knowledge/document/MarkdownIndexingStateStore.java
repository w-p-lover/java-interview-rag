package com.javainterview.rag.knowledge.document;

import java.util.Optional;

public interface MarkdownIndexingStateStore {

    void markIndexing(MarkdownDocument document, int chunkCount);

    void markReady(MarkdownDocument document, int chunkCount);

    void markFailed(MarkdownDocument document, int chunkCount, RuntimeException exception);

    Optional<MarkdownIndexingState> find(String source, String documentVersion);

    void removeBySource(String source);
}
