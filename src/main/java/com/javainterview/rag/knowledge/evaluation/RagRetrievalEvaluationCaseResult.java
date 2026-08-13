package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.rag.RagRetrievalSource;

import java.util.List;
import java.util.Objects;

public record RagRetrievalEvaluationCaseResult(
        String evaluationCaseId,
        String question,
        RagEvaluationCaseType type,
        List<String> expectedSources,
        List<List<String>> expectedHeadingPaths,
        List<RagRetrievalSource> actualSources,
        boolean expectedSourceMatched,
        Integer firstRelevantRank,
        boolean hitAt1,
        double recallAt5,
        double reciprocalRank,
        int expectedHeadingPathHitCount,
        double headingPathRecall,
        boolean rejectionExpected,
        boolean rejectedAtAnswerThreshold,
        boolean rejectionCorrect
) {

    public RagRetrievalEvaluationCaseResult {
        Objects.requireNonNull(evaluationCaseId, "评估题标识不能为空");
        Objects.requireNonNull(question, "评估问题不能为空");
        Objects.requireNonNull(type, "评估题型不能为空");
        expectedSources = List.copyOf(Objects.requireNonNull(expectedSources, "期望来源不能为空"));
        expectedHeadingPaths = Objects.requireNonNull(expectedHeadingPaths, "期望标题路径不能为空").stream()
                .map(List::copyOf)
                .toList();
        actualSources = List.copyOf(Objects.requireNonNull(actualSources, "实际来源不能为空"));
    }
}
