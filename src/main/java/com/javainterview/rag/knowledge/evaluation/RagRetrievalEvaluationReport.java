package com.javainterview.rag.knowledge.evaluation;

import java.util.List;
import java.util.Objects;

public record RagRetrievalEvaluationReport(
        int topK,
        double similarityThreshold,
        double answerSimilarityThreshold,
        int totalCaseCount,
        int answerableCaseCount,
        int noAnswerCaseCount,
        int expectedSourceHitCount,
        double expectedSourceHitRate,
        int hitAt1Count,
        double hitAt1Rate,
        double meanRecallAt5,
        double meanReciprocalRank,
        int expectedHeadingPathCount,
        int expectedHeadingPathHitCount,
        double headingPathRecallRate,
        int structureCaseCount,
        int expectedPageNumberCount,
        int expectedPageNumberHitCount,
        double pageNumberRecallRate,
        int expectedBlockTypeCount,
        int expectedBlockTypeHitCount,
        double blockTypeRecallRate,
        int expectedStructureCount,
        int expectedStructureHitCount,
        double structureRecallRate,
        int correctRejectionCount,
        double rejectionAccuracy,
        List<RagRetrievalEvaluationCaseResult> cases
) {

    public RagRetrievalEvaluationReport {
        cases = List.copyOf(Objects.requireNonNull(cases, "评估结果不能为空"));
    }
}
