package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.resume.FixedResumeEvidenceAuditEvaluationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResumeEvidenceAuditEvaluationControllerTest {

    @Test
    void returnsMetadataAndCountsWithoutResumeOrAuditText() throws Exception {
        FixedResumeEvidenceAuditEvaluationService service =
                mock(FixedResumeEvidenceAuditEvaluationService.class);
        when(service.evaluate()).thenReturn(report());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new ResumeEvidenceAuditEvaluationController(service)).build();

        mockMvc.perform(post("/api/interviews/evaluations/resume-audits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baselineRunId").value("run-1"))
                .andExpect(jsonPath("$.datasetVersion").value("resume-evidence-audit-dataset-v1"))
                .andExpect(jsonPath("$.metrics.factAccuracyRate").value(1.0))
                .andExpect(jsonPath("$.metrics.totalTokens").value(120))
                .andExpect(jsonPath("$.cases[0].caseId").value("complete-resume"))
                .andExpect(jsonPath("$.cases[0].matchedExpectedFactCount").value(4))
                .andExpect(jsonPath("$.resumeText").doesNotExist())
                .andExpect(jsonPath("$.confirmedFacts").doesNotExist())
                .andExpect(jsonPath("$.cases[0].evidence").doesNotExist())
                .andExpect(jsonPath("$.cases[0].suggestions").doesNotExist());
    }

    private FixedResumeEvidenceAuditEvaluationService.Report report() {
        FixedResumeEvidenceAuditEvaluationService.Metrics metrics =
                new FixedResumeEvidenceAuditEvaluationService.Metrics(
                        1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.0, 1.0,
                        1.0, 1, 120, 120.0, null, 500.0, 500L);
        FixedResumeEvidenceAuditEvaluationService.CaseResult caseResult =
                new FixedResumeEvidenceAuditEvaluationService.CaseResult(
                        "complete-resume", "SUCCEEDED", true,
                        true,
                        4, 4, 4, 0,
                        0, 0, 0, 0,
                        4, 4,
                        100, 20, 120, 500L, null);
        return new FixedResumeEvidenceAuditEvaluationService.Report(
                "run-1",
                "resume-evidence-audit-dataset-v1",
                "RESUME_EVIDENCE_AUDITOR",
                "resume-evidence-audit-v1",
                "model-test",
                1,
                1,
                metrics,
                "未配置模型单价，不估算费用；真实费用以模型服务商账单为准。",
                List.of(caseResult));
    }
}
