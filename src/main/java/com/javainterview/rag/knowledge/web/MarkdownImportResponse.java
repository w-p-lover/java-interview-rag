package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.document.MarkdownChunk;
import com.javainterview.rag.knowledge.document.MarkdownImportResult;
import com.javainterview.rag.knowledge.document.KnowledgeBlockType;

import java.util.List;

public record MarkdownImportResponse(String source, String version, boolean imported,
                                     int chunkCount, List<ChunkResponse> chunks, String format,
                                     Integer pageCount, List<Integer> pagesNeedingOcr,
                                     List<String> warnings) {

    public MarkdownImportResponse {
        chunks = List.copyOf(chunks);
        pagesNeedingOcr = List.copyOf(pagesNeedingOcr);
        warnings = List.copyOf(warnings);
    }

    public static MarkdownImportResponse from(MarkdownImportResult result) {
        return from(result, "MARKDOWN", null, List.of(), List.of());
    }

    public static MarkdownImportResponse from(MarkdownImportResult result, String format,
                                              Integer pageCount, List<Integer> pagesNeedingOcr,
                                              List<String> warnings) {
        List<ChunkResponse> chunks = result.chunks().stream()
                .map(ChunkResponse::from)
                .toList();
        return new MarkdownImportResponse(result.document().source(), result.document().version(),
                result.imported(), chunks.size(), chunks, format, pageCount, pagesNeedingOcr, warnings);
    }

    public record ChunkResponse(int order, int sectionOrder, int headingLevel,
                                List<String> headingPath, String content,
                                Integer pageNumber, KnowledgeBlockType blockType) {

        private static ChunkResponse from(MarkdownChunk chunk) {
            return new ChunkResponse(chunk.order(), chunk.sectionOrder(), chunk.headingLevel(),
                    chunk.headingPath(), chunk.content(), chunk.pageNumber(), chunk.blockType());
        }
    }
}
