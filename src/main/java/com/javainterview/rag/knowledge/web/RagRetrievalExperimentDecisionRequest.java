package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.experiment.RagRetrievalExperimentDecision;

public record RagRetrievalExperimentDecisionRequest(
        String baselineRunId,
        RagRetrievalExperimentDecision decision,
        String note
) {
}
