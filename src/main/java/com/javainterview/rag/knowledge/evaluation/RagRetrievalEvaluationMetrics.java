package com.javainterview.rag.knowledge.evaluation;

/** 检索质量、上下文噪声和延迟的可复核聚合指标。 */
public record RagRetrievalEvaluationMetrics(
        double precisionAt5,
        double ndcgAt5,
        double chunkRecallAt5,
        double hardNegativeAvoidanceRate,
        double contextNoiseRate,
        double retrievalLatencyP95Ms
) {
}
