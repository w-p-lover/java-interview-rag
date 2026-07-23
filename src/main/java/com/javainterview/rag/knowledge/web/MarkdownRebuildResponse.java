package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.document.MarkdownRebuildResult;

public record MarkdownRebuildResponse(String source, String version, int chunkCount) {

    public static MarkdownRebuildResponse from(MarkdownRebuildResult result) {
        return new MarkdownRebuildResponse(result.document().source(), result.document().version(),
                result.chunks().size());
    }
}
