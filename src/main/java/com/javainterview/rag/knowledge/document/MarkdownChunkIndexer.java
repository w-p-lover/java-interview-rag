package com.javainterview.rag.knowledge.document;

import java.util.List;

@FunctionalInterface
public interface MarkdownChunkIndexer {

    void index(MarkdownDocument document, List<MarkdownChunk> chunks);

    default void deleteOtherVersions(MarkdownDocument document) {
    }

    default void deleteBySource(String source) {
    }

    static MarkdownChunkIndexer noOp() {
        return (document, chunks) -> {
        };
    }
}
