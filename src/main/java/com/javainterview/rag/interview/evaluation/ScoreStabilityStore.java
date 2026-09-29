package com.javainterview.rag.interview.evaluation;

import java.util.List;
import java.util.Optional;

public interface ScoreStabilityStore {
    ScoreStabilityRun createOrFind(ScoreStabilityRun run);
    Optional<ScoreStabilityRun> find(String runId);
    List<ScoreStabilityRun> findRecent(int limit);
    List<ScoreStabilityRun> findRecoverable();
    void saveProgress(String runId, ScoreStabilityStatus status, List<ScoreStabilityCaseResult> results,
                      ScoreStabilityReport report, String errorType, boolean completed);
    void saveDecision(String candidateRunId, String baselineRunId, AnswerExperimentDecision decision, String note);
}
