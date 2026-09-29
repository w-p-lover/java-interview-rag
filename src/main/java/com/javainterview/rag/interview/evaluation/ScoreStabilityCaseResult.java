package com.javainterview.rag.interview.evaluation;

import java.util.List;

public record ScoreStabilityCaseResult(
        String caseId,
        Status status,
        boolean passed,
        boolean expectedKnowledgeGrounded,
        List<Integer> overallScores,
        List<Boolean> knowledgeGroundedResults,
        long latencyMs,
        String errorType) {

    public ScoreStabilityCaseResult {
        if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("评分稳定性样本标识不能为空");
        }
        overallScores = overallScores == null ? List.of() : List.copyOf(overallScores);
        knowledgeGroundedResults = knowledgeGroundedResults == null
                ? List.of() : List.copyOf(knowledgeGroundedResults);
    }

    public boolean complete(int repeatCount) {
        return status == Status.SUCCEEDED
                && (overallScores.size() >= repeatCount || overallScores.isEmpty());
    }

    public enum Status { SUCCEEDED, FAILED }
}
