package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.document.MarkdownRebuildResult;

import java.util.List;

public record MarkdownRebuildResponse(String source, String version, int chunkCount,
                                      String format, Integer pageCount,
                                      List<Integer> pagesNeedingOcr, List<String> warnings) {

    public MarkdownRebuildResponse {
        pagesNeedingOcr = List.copyOf(pagesNeedingOcr);
        warnings = List.copyOf(warnings);
    }

    public static MarkdownRebuildResponse from(MarkdownRebuildResult result) {
        return from(result, "MARKDOWN", null, List.of(), List.of());
    }

    public static MarkdownRebuildResponse from(MarkdownRebuildResult result, String format,
                                                Integer pageCount, List<Integer> pagesNeedingOcr,
                                                List<String> warnings) {
        return new MarkdownRebuildResponse(result.document().source(), result.document().version(),
                result.chunks().size(), format, pageCount, pagesNeedingOcr, warnings);
    }
}
