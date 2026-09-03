package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.rag.RagAnswerClaimEvidence;
import com.javainterview.rag.knowledge.rag.RagRetrievalSource;

import java.util.List;
import java.util.Objects;

public record RagAnswerEvaluationCaseResult(
        String evaluationCaseId,
        String question,
        String answer,
        boolean grounded,
        List<String> expectedSources,
        List<RagRetrievalSource> actualSources,
        boolean expectedSourceMatched,
        List<String> expectedAnswerPoints,
        List<String> matchedAnswerPoints,
        List<String> unmatchedAnswerPoints,
        double answerPointCoverageRate,
        List<RagAnswerClaimEvidence> claimEvidence,
        RagAnswerEvaluationMetrics metrics
) {

    public RagAnswerEvaluationCaseResult(String evaluationCaseId, String question, String answer,
                                         boolean grounded, List<String> expectedSources,
                                         List<RagRetrievalSource> actualSources, boolean expectedSourceMatched,
                                         List<String> expectedAnswerPoints, List<String> matchedAnswerPoints,
                                         List<String> unmatchedAnswerPoints, double answerPointCoverageRate) {
        this(evaluationCaseId, question, answer, grounded, expectedSources, actualSources,
                expectedSourceMatched, expectedAnswerPoints, matchedAnswerPoints, unmatchedAnswerPoints,
                answerPointCoverageRate, List.of(), new RagAnswerEvaluationMetrics(
                        0, 0, 0, 0, 0, 0, 0.0, 0.0, 0.0, 0.0));
    }

    public RagAnswerEvaluationCaseResult {
        Objects.requireNonNull(evaluationCaseId, "评估题标识不能为空");
        Objects.requireNonNull(question, "评估问题不能为空");
        Objects.requireNonNull(answer, "评估回答不能为空");
        expectedSources = List.copyOf(Objects.requireNonNull(expectedSources, "期望来源不能为空"));
        actualSources = List.copyOf(Objects.requireNonNull(actualSources, "实际来源不能为空"));
        expectedAnswerPoints = List.copyOf(Objects.requireNonNull(expectedAnswerPoints, "预期回答要点不能为空"));
        matchedAnswerPoints = List.copyOf(Objects.requireNonNull(matchedAnswerPoints, "命中回答要点不能为空"));
        unmatchedAnswerPoints = List.copyOf(Objects.requireNonNull(unmatchedAnswerPoints, "未命中回答要点不能为空"));
        claimEvidence = List.copyOf(Objects.requireNonNull(claimEvidence, "回答证据判断不能为空"));
        Objects.requireNonNull(metrics, "回答评估指标不能为空");
    }
}
