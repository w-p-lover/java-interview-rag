package com.javainterview.rag.interview.evaluation;

import java.time.LocalDateTime;
import java.util.List;

public record ScoreStabilityRun(
        String runId, String name, String datasetVersion, String promptVersion,
        String rubricVersion, String modelName, String contractHash,
        ScoreStabilityStatus status, int totalCaseCount, int repeatCount,
        List<ScoreStabilityCaseResult> caseResults, ScoreStabilityReport report,
        String errorType, String baselineRunId, AnswerExperimentDecision decision,
        String decisionNote, LocalDateTime createdAt, LocalDateTime updatedAt,
        LocalDateTime completedAt, LocalDateTime decidedAt) {
    public boolean terminal() {
        return status == ScoreStabilityStatus.SUCCEEDED || status == ScoreStabilityStatus.PARTIAL_FAILED;
    }
}
