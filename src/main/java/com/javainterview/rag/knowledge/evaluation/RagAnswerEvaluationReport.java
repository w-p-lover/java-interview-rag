package com.javainterview.rag.knowledge.evaluation;

import java.util.List;
import java.util.Objects;

public record RagAnswerEvaluationReport(
        String matchingMethod,
        int totalCaseCount,
        int groundedCaseCount,
        int expectedSourceHitCount,
        int totalAnswerPointCount,
        int matchedAnswerPointCount,
        double answerPointCoverageRate,
        List<RagAnswerEvaluationCaseResult> cases
) {

    public RagAnswerEvaluationReport {
        Objects.requireNonNull(matchingMethod, "匹配方法不能为空");
        cases = List.copyOf(Objects.requireNonNull(cases, "评估结果不能为空"));
    }
}
