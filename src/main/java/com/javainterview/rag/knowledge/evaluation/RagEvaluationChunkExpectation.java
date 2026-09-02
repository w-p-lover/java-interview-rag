package com.javainterview.rag.knowledge.evaluation;

import java.util.Objects;

/** 固定评估中的 Chunk 级期望身份。 */
public record RagEvaluationChunkExpectation(String source, int chunkOrder) {

    public RagEvaluationChunkExpectation {
        Objects.requireNonNull(source, "Chunk 来源不能为空");
        if (source.isBlank()) {
            throw new IllegalArgumentException("Chunk 来源不能为空");
        }
        if (chunkOrder < 0) {
            throw new IllegalArgumentException("Chunk 顺序不能小于 0");
        }
    }
}
