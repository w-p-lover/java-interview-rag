package com.javainterview.rag.knowledge.evaluation;

import java.util.List;

public record RagEvaluationCandidateResult(
        String candidateId,
        String question,
        boolean grounded,
        boolean expectedSourceMatched,
        List<String> expectedAnswerPoints,
        List<String> matchedAnswerPoints,
        List<String> actualSources,
        String retrievalStrategy,
        boolean degraded,
        String degradationReason
) {
    public RagEvaluationCandidateResult {
        expectedAnswerPoints = List.copyOf(expectedAnswerPoints);
        matchedAnswerPoints = List.copyOf(matchedAnswerPoints);
        actualSources = List.copyOf(actualSources);
    }
}
