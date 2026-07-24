package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.rag.RagRetrievalSource;

import java.util.List;
import java.util.Objects;

public record RagRetrievalEvaluationCaseResult(
        String evaluationCaseId,
        String question,
        List<String> expectedSources,
        List<RagRetrievalSource> actualSources,
        boolean expectedSourceMatched
) {

    public RagRetrievalEvaluationCaseResult {
        Objects.requireNonNull(evaluationCaseId, "评估题标识不能为空");
        Objects.requireNonNull(question, "评估问题不能为空");
        expectedSources = List.copyOf(Objects.requireNonNull(expectedSources, "期望来源不能为空"));
        actualSources = List.copyOf(Objects.requireNonNull(actualSources, "实际来源不能为空"));
    }
}
