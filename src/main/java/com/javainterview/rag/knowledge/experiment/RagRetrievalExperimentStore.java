package com.javainterview.rag.knowledge.experiment;

import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationReport;

import java.util.List;
import java.util.Optional;

public interface RagRetrievalExperimentStore {

    RagRetrievalExperimentRun createOrFind(RagRetrievalExperimentRun run);

    Optional<RagRetrievalExperimentRun> find(String runId);

    List<RagRetrievalExperimentRun> findRecent(int limit);

    List<RagRetrievalExperimentRun> findRecoverable();

    void saveProgress(String runId,
                      RagRetrievalExperimentStatus status,
                      List<RagRetrievalExperimentCaseResult> caseResults,
                      RagRetrievalEvaluationReport report,
                      String errorType,
                      boolean completed);

    void saveDecision(String candidateRunId,
                      String baselineRunId,
                      RagRetrievalExperimentDecision decision,
                      String note);
}
