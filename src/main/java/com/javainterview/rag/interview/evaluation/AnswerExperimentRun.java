package com.javainterview.rag.interview.evaluation;

import java.time.LocalDateTime;
import java.util.List;

public record AnswerExperimentRun(String runId, String name, String datasetVersion,
                                  String promptVersion, String rubricVersion, String modelName,
                                  String contractHash, AnswerExperimentStatus status,
                                  int totalCaseCount, List<AnswerExperimentCaseResult> caseResults,
                                  AnswerExperimentReport report, String errorType,
                                  String baselineRunId, AnswerExperimentDecision decision,
                                  String decisionNote, LocalDateTime createdAt, LocalDateTime updatedAt,
                                  LocalDateTime completedAt, LocalDateTime decidedAt) {
    public boolean terminal() { return status == AnswerExperimentStatus.SUCCEEDED || status == AnswerExperimentStatus.PARTIAL_FAILED; }
}
