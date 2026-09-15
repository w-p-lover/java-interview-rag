package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.document.MarkdownRebuildResult;

public record MarkdownRollbackResponse(String source, String version, int chunkCount) {
    static MarkdownRollbackResponse from(MarkdownRebuildResult result) {
        return new MarkdownRollbackResponse(result.document().source(), result.document().version(),
                result.chunks().size());
    }
}
