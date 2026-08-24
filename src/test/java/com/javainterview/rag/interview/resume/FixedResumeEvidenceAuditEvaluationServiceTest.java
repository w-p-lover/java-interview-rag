package com.javainterview.rag.interview.resume;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FixedResumeEvidenceAuditEvaluationServiceTest {

    @Test
    void calculatesDeterministicMetricsWithoutReturningResumeContent() {
        FixedResumeEvidenceAuditDataset dataset = new FixedResumeEvidenceAuditDataset();
        ResumeEvidenceAuditService auditService = mockAuditService(dataset, null);
        FixedResumeEvidenceAuditEvaluationService service =
                new FixedResumeEvidenceAuditEvaluationService(dataset, auditService);

        FixedResumeEvidenceAuditEvaluationService.Report report = service.evaluate();

        assertEquals(FixedResumeEvidenceAuditDataset.VERSION, report.datasetVersion());
        assertEquals("resume-evidence-audit-v1", report.promptVersion());
        assertEquals(6, report.totalCaseCount());
        assertEquals(6, report.passedCaseCount());
        assertEquals(1.0, report.metrics().auditSuccessRate());
        assertEquals(1.0, report.metrics().structureParseSuccessRate());
        assertEquals(1.0, report.metrics().factAccuracyRate());
        assertEquals(1.0, report.metrics().expectedFactRecallRate());
        assertEquals(1.0, report.metrics().evidenceLegalRate());
        assertEquals(1.0, report.metrics().missingInformationRecallRate());
        assertEquals(0.0, report.metrics().hallucinationRate());
        assertEquals(1.0, report.metrics().riskSignalRecallRate());
        assertEquals(6, report.metrics().tokenMeasuredCaseCount());
        assertEquals(720, report.metrics().totalTokens());
        assertEquals(120.0, report.metrics().averageTotalTokens());
        assertEquals(null, report.metrics().estimatedCostCny());
        assertEquals(35.0, report.metrics().averageElapsedMillis());
        assertEquals(60L, report.metrics().p95ElapsedMillis());
        assertTrue(report.costEstimationNote().contains("不估算费用"));
        assertEquals(List.of(
                        "complete-resume",
                        "missing-result",
                        "time-conflict",
                        "skill-stacking",
                        "vague-project",
                        "prompt-injection"),
                report.cases().stream()
                        .map(FixedResumeEvidenceAuditEvaluationService.CaseResult::caseId)
                        .toList());
        verify(auditService, times(6)).auditForEvaluation(anyString());
    }

    @Test
    void keepsFailedEvidenceValidationInAggregateRates() {
        FixedResumeEvidenceAuditDataset dataset = new FixedResumeEvidenceAuditDataset();
        ResumeEvidenceAuditService auditService = mockAuditService(dataset, "missing-result");
        FixedResumeEvidenceAuditEvaluationService service =
                new FixedResumeEvidenceAuditEvaluationService(dataset, auditService);

        FixedResumeEvidenceAuditEvaluationService.Report report = service.evaluate();

        assertEquals(5, report.passedCaseCount());
        assertEquals(0.8333, report.metrics().auditSuccessRate());
        assertEquals(1.0, report.metrics().structureParseSuccessRate());
        assertEquals(0.9, report.metrics().expectedFactRecallRate());
        assertEquals(0.9, report.metrics().evidenceLegalRate());
        FixedResumeEvidenceAuditEvaluationService.CaseResult failed = report.cases().stream()
                .filter(result -> "missing-result".equals(result.caseId()))
                .findFirst()
                .orElseThrow();
        assertEquals("FAILED", failed.status());
        assertEquals("IllegalStateException", failed.errorType());
        assertEquals(1, failed.candidateFactCount());
        assertEquals(0, failed.legalEvidenceCount());
    }

    private ResumeEvidenceAuditService mockAuditService(
            FixedResumeEvidenceAuditDataset dataset,
            String failedCaseId) {
        ResumeEvidenceAuditService auditService = mock(ResumeEvidenceAuditService.class);
        when(auditService.roleName()).thenReturn("RESUME_EVIDENCE_AUDITOR");
        when(auditService.promptVersion()).thenReturn("resume-evidence-audit-v1");
        when(auditService.modelName()).thenReturn("model-test");
        when(auditService.auditForEvaluation(anyString())).thenAnswer(invocation -> {
            String resumeText = invocation.getArgument(0);
            FixedResumeEvidenceAuditDataset.Case evaluationCase = dataset.findAll().stream()
                    .filter(item -> item.resumeText().equals(resumeText))
                    .findFirst()
                    .orElseThrow();
            int caseIndex = dataset.findAll().indexOf(evaluationCase);
            if (evaluationCase.id().equals(failedCaseId)) {
                return new ResumeEvidenceAuditService.EvaluationAttempt(
                        null,
                        true,
                        evaluationCase.expectedFacts().size(),
                        0,
                        100,
                        20,
                        120,
                        (caseIndex + 1) * 10L,
                        new IllegalStateException("模型返回的简历事实证据不在原始简历中"));
            }
            ResumeEvidenceAuditResult result = result(evaluationCase);
            return new ResumeEvidenceAuditService.EvaluationAttempt(
                    result,
                    true,
                    evaluationCase.expectedFacts().size(),
                    evaluationCase.expectedFacts().size(),
                    100,
                    20,
                    120,
                    (caseIndex + 1) * 10L,
                    null);
        });
        return auditService;
    }

    private ResumeEvidenceAuditResult result(FixedResumeEvidenceAuditDataset.Case evaluationCase) {
        List<String> missingInformation = evaluationCase.expectedMissingSignals().isEmpty()
                ? List.of()
                : List.of("缺少" + String.join("和", evaluationCase.expectedMissingSignals()));
        List<String> risks = evaluationCase.expectedRiskSignals().isEmpty()
                ? List.of()
                : List.of(String.join("和", evaluationCase.expectedRiskSignals()) + "需要人工核对");
        return new ResumeEvidenceAuditResult(
                "request-" + evaluationCase.id(),
                "RESUME_EVIDENCE_AUDITOR",
                "resume-evidence-audit-v1",
                evaluationCase.expectedFacts().stream()
                        .map(fact -> new ResumeEvidenceAuditResult.ConfirmedFact(
                                fact.type(), fact.evidence(), fact.evidence()))
                        .toList(),
                missingInformation,
                risks,
                List.of("仅保留脱敏建议计数"));
    }
}
