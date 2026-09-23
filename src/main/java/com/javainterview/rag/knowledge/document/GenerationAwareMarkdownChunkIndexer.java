package com.javainterview.rag.knowledge.document;

import java.util.List;

public interface GenerationAwareMarkdownChunkIndexer extends MarkdownChunkIndexer {

    void buildGeneration(MarkdownDocument document, List<MarkdownChunk> chunks,
                         MarkdownIndexBuild build);

    default void verifyGeneration(MarkdownDocument document, List<MarkdownChunk> chunks,
                                  MarkdownIndexBuild build) {
    }

    void deleteGeneration(MarkdownDocument document, MarkdownIndexBuild build);

    void deleteOtherGenerations(MarkdownDocument document, MarkdownIndexBuild publishedBuild);
}
