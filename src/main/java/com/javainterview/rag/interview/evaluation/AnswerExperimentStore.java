package com.javainterview.rag.interview.evaluation;

import java.util.List;
import java.util.Optional;

public interface AnswerExperimentStore {
    AnswerExperimentRun createOrFind(AnswerExperimentRun run);
    Optional<AnswerExperimentRun> find(String runId);
    List<AnswerExperimentRun> findRecent(int limit);
    List<AnswerExperimentRun> findRecoverable();
    void saveProgress(String runId, AnswerExperimentStatus status, List<AnswerExperimentCaseResult> results,
                      AnswerExperimentReport report, String errorType, boolean completed);
    void saveDecision(String candidateRunId, String baselineRunId, AnswerExperimentDecision decision, String note);
}
