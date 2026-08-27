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

        assertEquals("resume-evidence-audit-dataset-v7", report.datasetVersion());
        assertEquals("resume-evidence-audit-v3", report.promptVersion());
        assertEquals(11, report.totalCaseCount());
        assertEquals(11, report.passedCaseCount());
        assertEquals(1.0, report.metrics().auditSuccessRate());
        assertEquals(1.0, report.metrics().structureParseSuccessRate());
        assertEquals(1.0, report.metrics().factAccuracyRate());
        assertEquals(1.0, report.metrics().expectedFactRecallRate());
        assertEquals(1.0, report.metrics().evidenceLegalRate());
        assertEquals(1.0, report.metrics().missingInformationRecallRate());
        assertEquals(0.0, report.metrics().hallucinationRate());
        assertEquals(1.0, report.metrics().riskSignalRecallRate());
        assertEquals(30, report.metrics().factCapacityLimit());
        assertEquals(30, report.metrics().maxCandidateFactCount());
        assertEquals(1, report.metrics().capacityReachedCaseCount());
        assertEquals(11, report.metrics().tokenMeasuredCaseCount());
        assertEquals(1320, report.metrics().totalTokens());
        assertEquals(120.0, report.metrics().averageTotalTokens());
        assertEquals(null, report.metrics().estimatedCostCny());
        assertEquals(60.0, report.metrics().averageElapsedMillis());
        assertEquals(110L, report.metrics().p95ElapsedMillis());
        assertEquals(List.of(16, 12, 11, 15, 11, 19, 6), report.factTypeMetrics().stream()
                .map(FixedResumeEvidenceAuditEvaluationService.FactTypeMetric::expectedFactCount)
                .toList());
        assertTrue(report.factTypeMetrics().stream()
                .allMatch(metric -> metric.recallRate() == 1.0 && metric.accuracyRate() == 1.0));
        assertTrue(report.costEstimationNote().contains("不估算费用"));
        assertEquals(List.of(
                        "complete-resume",
                        "missing-result",
                        "time-conflict",
                        "skill-stacking",
                        "vague-project",
                        "prompt-injection",
                        "education-employment",
                        "education-employment-date-first",
                        "education-employment-multiple",
                        "project-responsibility",
                        "multi-section-capacity"),
                report.cases().stream()
                        .map(FixedResumeEvidenceAuditEvaluationService.CaseResult::caseId)
                        .toList());
        verify(auditService, times(11)).auditForEvaluation(anyString());
    }

    @Test
    void keepsFailedEvidenceValidationInAggregateRates() {
        FixedResumeEvidenceAuditDataset dataset = new FixedResumeEvidenceAuditDataset();
        ResumeEvidenceAuditService auditService = mockAuditService(dataset, "missing-result");
        FixedResumeEvidenceAuditEvaluationService service =
                new FixedResumeEvidenceAuditEvaluationService(dataset, auditService);

        FixedResumeEvidenceAuditEvaluationService.Report report = service.evaluate();

        assertEquals(10, report.passedCaseCount());
        assertEquals(0.9091, report.metrics().auditSuccessRate());
        assertEquals(1.0, report.metrics().structureParseSuccessRate());
        assertEquals(0.9667, report.metrics().expectedFactRecallRate());
        assertEquals(0.9667, report.metrics().evidenceLegalRate());
        FixedResumeEvidenceAuditEvaluationService.CaseResult failed = report.cases().stream()
                .filter(result -> "missing-result".equals(result.caseId()))
                .findFirst()
                .orElseThrow();
        assertEquals("FAILED", failed.status());
        assertEquals("IllegalStateException", failed.errorType());
        assertEquals(3, failed.candidateFactCount());
        assertEquals(0, failed.legalEvidenceCount());
    }

    private ResumeEvidenceAuditService mockAuditService(
            FixedResumeEvidenceAuditDataset dataset,
            String failedCaseId) {
        ResumeEvidenceAuditService auditService = mock(ResumeEvidenceAuditService.class);
        when(auditService.roleName()).thenReturn("RESUME_EVIDENCE_AUDITOR");
        when(auditService.promptVersion()).thenReturn("resume-evidence-audit-v3");
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
                : evaluationCase.expectedMissingSignals().stream()
                .map(signal -> "缺少" + signal.alternatives().get(0))
                .toList();
        List<String> risks = evaluationCase.expectedRiskSignals().isEmpty()
                ? List.of()
                : evaluationCase.expectedRiskSignals().stream()
                .map(signal -> signal.alternatives().get(0) + "需要人工核对")
                .toList();
        return new ResumeEvidenceAuditResult(
                "request-" + evaluationCase.id(),
                "RESUME_EVIDENCE_AUDITOR",
                "resume-evidence-audit-v3",
                evaluationCase.expectedFacts().stream()
                        .map(fact -> new ResumeEvidenceAuditResult.ConfirmedFact(
                                fact.type(), fact.evidence(),
                                "missing-result".equals(evaluationCase.id())
                                        ? "使用 Spring Boot 开发订单服务"
                                        : fact.evidence()))
                        .toList(),
                missingInformation,
                risks,
                List.of("仅保留脱敏建议计数"));
    }
}
