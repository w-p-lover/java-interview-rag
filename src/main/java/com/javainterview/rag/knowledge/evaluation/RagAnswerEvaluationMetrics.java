package com.javainterview.rag.knowledge.evaluation;

public record RagAnswerEvaluationMetrics(
        int claimCount,
        int citedClaimCount,
        int supportedClaimCount,
        int insufficientClaimCount,
        int conflictingClaimCount,
        int citedExpectedAnswerPointCount,
        double citationPrecision,
        double citationRecall,
        double unsupportedStatementRate,
        double evidenceConflictRate
) {
}
