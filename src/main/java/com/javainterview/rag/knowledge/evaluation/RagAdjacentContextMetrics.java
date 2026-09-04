package com.javainterview.rag.knowledge.evaluation;

/** 相邻扩展前后完整回答上下文的质量指标。 */
public record RagAdjacentContextMetrics(
        int contextCount,
        double averageContextCount,
        int relevantContextCount,
        double contextNoiseRate,
        int expectedSourceCount,
        int expectedSourceHitCount,
        double sourceRecallRate,
        int expectedChunkCount,
        int expectedChunkHitCount,
        double chunkRecallRate,
        int expectedAnswerPointCount,
        int expectedAnswerPointHitCount,
        double answerPointEvidenceRecallRate
) {
}
