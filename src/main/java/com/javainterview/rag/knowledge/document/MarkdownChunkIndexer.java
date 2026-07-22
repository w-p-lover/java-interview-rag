package com.javainterview.rag.knowledge.document;

import java.util.List;

@FunctionalInterface
public interface MarkdownChunkIndexer {

    void index(MarkdownDocument document, List<MarkdownChunk> chunks);

    static MarkdownChunkIndexer noOp() {
        return (document, chunks) -> {
        };
    }
}
