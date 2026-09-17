package com.javainterview.rag.interview.resume;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResumeEvidenceAuditEvaluationRunServiceTest {

    @Test
    void reusesCompletedRunForSameContractWithoutRepeatingPaidCases() {
        FixedResumeEvidenceAuditDataset dataset = new FixedResumeEvidenceAuditDataset();
        ResumeEvidenceAuditService auditService = mockAuditService(dataset, null);
        ResumeEvidenceAuditEvaluationRunService service = service(dataset, auditService);

        ResumeEvidenceAuditEvaluationRunService.RunView first = service.start();
        ResumeEvidenceAuditEvaluationRunService.RunView repeated = service.start();

        assertEquals(ResumeEvidenceAuditEvaluationRun.Status.SUCCEEDED, first.status());
        assertEquals(first.runId(), repeated.runId());
        assertEquals(11, repeated.succeededCaseCount());
        assertEquals(0, repeated.failedCaseCount());
        assertEquals(0, repeated.pendingCaseCount());
        verify(auditService, times(11)).auditForEvaluation(anyString());
    }

    @Test
    void resumesOnlyFailedCaseAndKeepsSuccessfulResults() {
        FixedResumeEvidenceAuditDataset dataset = new FixedResumeEvidenceAuditDataset();
        ResumeEvidenceAuditService auditService = mockAuditService(dataset, "missing-result");
        ResumeEvidenceAuditEvaluationRunService service = service(dataset, auditService);

        ResumeEvidenceAuditEvaluationRunService.RunView failed = service.start();
        ResumeEvidenceAuditEvaluationRunService.RunView resumed = service.resume(failed.runId());

        assertEquals(ResumeEvidenceAuditEvaluationRun.Status.PARTIAL_FAILED, failed.status());
        assertEquals(10, failed.succeededCaseCount());
        assertEquals(1, failed.failedCaseCount());
        assertEquals(ResumeEvidenceAuditEvaluationRun.Status.SUCCEEDED, resumed.status());
        assertEquals(11, resumed.succeededCaseCount());
        verify(auditService, times(12)).auditForEvaluation(anyString());
    }

    private ResumeEvidenceAuditEvaluationRunService service(
            FixedResumeEvidenceAuditDataset dataset,
            ResumeEvidenceAuditService auditService) {
        FixedResumeEvidenceAuditEvaluationService evaluationService =
                new FixedResumeEvidenceAuditEvaluationService(dataset, auditService);
        return new ResumeEvidenceAuditEvaluationRunService(
                evaluationService, new InMemoryRunStore(), Runnable::run);
    }

    private ResumeEvidenceAuditService mockAuditService(
            FixedResumeEvidenceAuditDataset dataset,
            String failOnceCaseId) {
        ResumeEvidenceAuditService auditService = mock(ResumeEvidenceAuditService.class);
        when(auditService.roleName()).thenReturn("RESUME_EVIDENCE_AUDITOR");
        when(auditService.promptVersion()).thenReturn("resume-evidence-audit-v4");
        when(auditService.modelName()).thenReturn("model-test");
        AtomicBoolean failed = new AtomicBoolean(false);
        when(auditService.auditForEvaluation(anyString())).thenAnswer(invocation -> {
            String resumeText = invocation.getArgument(0);
            FixedResumeEvidenceAuditDataset.Case evaluationCase = dataset.findAll().stream()
                    .filter(item -> item.resumeText().equals(resumeText))
                    .findFirst()
                    .orElseThrow();
            if (evaluationCase.id().equals(failOnceCaseId) && failed.compareAndSet(false, true)) {
                return new ResumeEvidenceAuditService.EvaluationAttempt(
                        null, false, 0, 0, null, null, null, 10L,
                        new IllegalStateException("模型调用失败"));
            }
            return new ResumeEvidenceAuditService.EvaluationAttempt(
                    result(evaluationCase), true,
                    evaluationCase.expectedFacts().size(),
                    evaluationCase.expectedFacts().size(),
                    100, 20, 120, 10L, null);
        });
        return auditService;
    }

    private ResumeEvidenceAuditResult result(FixedResumeEvidenceAuditDataset.Case evaluationCase) {
        return new ResumeEvidenceAuditResult(
                "request-" + evaluationCase.id(),
                "RESUME_EVIDENCE_AUDITOR",
                "resume-evidence-audit-v4",
                evaluationCase.expectedFacts().stream()
                        .map(fact -> new ResumeEvidenceAuditResult.ConfirmedFact(
                                fact.type(), fact.evidence(), fact.evidence()))
                        .toList(),
                evaluationCase.expectedMissingSignals().stream()
                        .map(signal -> "缺少" + signal.alternatives().get(0))
                        .toList(),
                evaluationCase.expectedRiskSignals().stream()
                        .map(signal -> signal.alternatives().get(0) + "需要人工核对")
                        .toList(),
                List.of());
    }

    private static final class InMemoryRunStore
            implements ResumeEvidenceAuditEvaluationRunStore {

        private ResumeEvidenceAuditEvaluationRun run;

        @Override
        public synchronized ResumeEvidenceAuditEvaluationRun createOrFind(
                String runId, String datasetVersion, String role, String promptVersion,
                String modelName, int totalCaseCount) {
            if (run == null) {
                LocalDateTime now = LocalDateTime.now();
                run = new ResumeEvidenceAuditEvaluationRun(
                        runId, datasetVersion, role, promptVersion, modelName,
                        ResumeEvidenceAuditEvaluationRun.Status.QUEUED,
                        totalCaseCount, List.of(), null, now, now, null);
            }
            return run;
        }

        @Override
        public synchronized Optional<ResumeEvidenceAuditEvaluationRun> find(String runId) {
            return run != null && run.runId().equals(runId) ? Optional.of(run) : Optional.empty();
        }

        @Override
        public List<ResumeEvidenceAuditEvaluationRun> findRecoverable() {
            return List.of();
        }

        @Override
        public synchronized void saveProgress(
                String runId, ResumeEvidenceAuditEvaluationRun.Status status,
                List<FixedResumeEvidenceAuditEvaluationService.CaseResult> caseResults,
                String errorType, boolean completed) {
            LocalDateTime now = LocalDateTime.now();
            run = new ResumeEvidenceAuditEvaluationRun(
                    run.runId(), run.datasetVersion(), run.role(), run.promptVersion(),
                    run.modelName(), status, run.totalCaseCount(), caseResults, errorType,
                    run.createdAt(), now, completed ? now : null);
        }
    }
}
