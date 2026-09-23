package com.javainterview.rag.knowledge.document;

import java.util.Objects;

public record MarkdownIndexingState(
        String source,
        String documentVersion,
        int chunkCount,
        MarkdownIndexingStatus status,
        String errorType,
        String publishedGeneration,
        String buildingGeneration,
        Integer buildingChunkCount,
        String attemptId
) {

    public MarkdownIndexingState {
        Objects.requireNonNull(source, "文档来源不能为空");
        Objects.requireNonNull(documentVersion, "文档版本不能为空");
        if (chunkCount < 0) {
            throw new IllegalArgumentException("Chunk 数量不能小于 0");
        }
        Objects.requireNonNull(status, "索引状态不能为空");
        if (buildingChunkCount != null && buildingChunkCount < 0) {
            throw new IllegalArgumentException("构建 Chunk 数量不能小于 0");
        }
    }
}
