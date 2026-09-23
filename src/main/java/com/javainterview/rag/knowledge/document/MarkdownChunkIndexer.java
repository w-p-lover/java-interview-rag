package com.javainterview.rag.knowledge.document;

import java.util.List;

@FunctionalInterface
public interface MarkdownChunkIndexer {

    void index(MarkdownDocument document, List<MarkdownChunk> chunks);

    default void index(MarkdownDocument document, List<MarkdownChunk> chunks, String attemptId) {
        index(document, chunks);
    }

    default void rebuild(MarkdownDocument document, List<MarkdownChunk> chunks) {
        index(document, chunks);
        deleteOtherVersions(document);
    }

    default void rebuild(MarkdownDocument document, List<MarkdownChunk> chunks, String attemptId) {
        rebuild(document, chunks);
    }

    default void deleteOtherVersions(MarkdownDocument document) {
    }

    default void deleteVersion(MarkdownDocument document) {
    }

    default void deleteBySource(String source) {
    }

    default boolean cleansFailedWrites() {
        return false;
    }

    static MarkdownChunkIndexer noOp() {
        return (document, chunks) -> {
        };
    }
}
