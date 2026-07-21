package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.document.MarkdownChunk;
import com.javainterview.rag.knowledge.document.MarkdownImportResult;

import java.util.List;

public record MarkdownImportResponse(String source, String version, boolean imported,
                                     int chunkCount, List<ChunkResponse> chunks) {

    public static MarkdownImportResponse from(MarkdownImportResult result) {
        List<ChunkResponse> chunks = result.chunks().stream()
                .map(ChunkResponse::from)
                .toList();
        return new MarkdownImportResponse(result.document().source(), result.document().version(),
                result.imported(), chunks.size(), chunks);
    }

    public record ChunkResponse(int order, int sectionOrder, int headingLevel,
                                List<String> headingPath, String content) {

        private static ChunkResponse from(MarkdownChunk chunk) {
            return new ChunkResponse(chunk.order(), chunk.sectionOrder(), chunk.headingLevel(),
                    chunk.headingPath(), chunk.content());
        }
    }
}
