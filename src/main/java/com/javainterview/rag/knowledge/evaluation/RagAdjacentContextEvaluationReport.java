package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.rag.RagRetrievalSource;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record RagAdjacentContextEvaluationReport(
        String candidateStrategy,
        int expandedCaseCount,
        int additionalVectorQueryCount,
        int addedContextCount,
        Map<String, List<RagRetrievalSource>> addedContexts,
        RagAdjacentContextMetrics baselineContext,
        RagAdjacentContextMetrics candidateContext,
        RagRetrievalEvaluationReport baseline,
        RagRetrievalEvaluationReport candidate
) {

    public RagAdjacentContextEvaluationReport {
        Objects.requireNonNull(candidateStrategy, "相邻上下文策略不能为空");
        Objects.requireNonNull(addedContexts, "逐题新增上下文不能为空");
        Map<String, List<RagRetrievalSource>> copiedContexts = new LinkedHashMap<>();
        addedContexts.forEach((caseId, contexts) -> copiedContexts.put(caseId, List.copyOf(contexts)));
        addedContexts = Collections.unmodifiableMap(copiedContexts);
        Objects.requireNonNull(baselineContext, "基线上下文指标不能为空");
        Objects.requireNonNull(candidateContext, "候选上下文指标不能为空");
        Objects.requireNonNull(baseline, "基线检索报告不能为空");
        Objects.requireNonNull(candidate, "候选检索报告不能为空");
    }
}
