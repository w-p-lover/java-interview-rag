package com.javainterview.rag.interview.resume;

import java.util.List;
import java.util.Optional;

public interface ResumeEvidenceAuditEvaluationRunStore {

    ResumeEvidenceAuditEvaluationRun createOrFind(
            String runId,
            String datasetVersion,
            String role,
            String promptVersion,
            String modelName,
            int totalCaseCount);

    Optional<ResumeEvidenceAuditEvaluationRun> find(String runId);

    List<ResumeEvidenceAuditEvaluationRun> findRecoverable();

    void saveProgress(
            String runId,
            ResumeEvidenceAuditEvaluationRun.Status status,
            List<FixedResumeEvidenceAuditEvaluationService.CaseResult> caseResults,
            String errorType,
            boolean completed);
}
