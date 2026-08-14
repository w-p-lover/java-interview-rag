package com.javainterview.rag.knowledge.evaluation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record RagRetrievalEvaluationComparisonReport(
        String candidateStrategy,
        int decomposedCaseCount,
        int additionalQueryCount,
        Map<String, List<String>> retrievalQueries,
        RagRetrievalEvaluationReport baseline,
        RagRetrievalEvaluationReport candidate
) {

    public RagRetrievalEvaluationComparisonReport {
        Objects.requireNonNull(candidateStrategy, "候选检索策略不能为空");
        Objects.requireNonNull(retrievalQueries, "逐题检索计划不能为空");
        Map<String, List<String>> copiedQueries = new LinkedHashMap<>();
        retrievalQueries.forEach((caseId, queries) -> copiedQueries.put(caseId, List.copyOf(queries)));
        retrievalQueries = Collections.unmodifiableMap(copiedQueries);
        Objects.requireNonNull(baseline, "基线评估报告不能为空");
        Objects.requireNonNull(candidate, "候选评估报告不能为空");
    }
}
