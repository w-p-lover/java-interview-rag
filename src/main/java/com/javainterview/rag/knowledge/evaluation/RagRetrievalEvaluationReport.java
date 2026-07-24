package com.javainterview.rag.knowledge.evaluation;

import java.util.List;
import java.util.Objects;

public record RagRetrievalEvaluationReport(
        int topK,
        double similarityThreshold,
        int totalCaseCount,
        int expectedSourceHitCount,
        double expectedSourceHitRate,
        List<RagRetrievalEvaluationCaseResult> cases
) {

    public RagRetrievalEvaluationReport {
        cases = List.copyOf(Objects.requireNonNull(cases, "评估结果不能为空"));
    }
}
