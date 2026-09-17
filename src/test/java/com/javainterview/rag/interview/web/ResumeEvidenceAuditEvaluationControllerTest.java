package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.resume.ResumeEvidenceAuditEvaluationRun;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditEvaluationRunService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResumeEvidenceAuditEvaluationControllerTest {

    @Test
    void startsAndQueriesRunWithoutReturningResumeContent() throws Exception {
        ResumeEvidenceAuditEvaluationRunService service =
                mock(ResumeEvidenceAuditEvaluationRunService.class);
        when(service.start()).thenReturn(runView());
        when(service.find("run-1")).thenReturn(runView());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new ResumeEvidenceAuditEvaluationController(service)).build();

        mockMvc.perform(post("/api/interviews/evaluations/resume-audits"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.runId").value("run-1"))
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.datasetVersion").value("resume-evidence-audit-dataset-v7"))
                .andExpect(jsonPath("$.totalCaseCount").value(11))
                .andExpect(jsonPath("$.resumeText").doesNotExist())
                .andExpect(jsonPath("$.confirmedFacts").doesNotExist());

        mockMvc.perform(get("/api/interviews/evaluations/resume-audits/run-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runId").value("run-1"))
                .andExpect(jsonPath("$.pendingCaseCount").value(11));
    }

    private ResumeEvidenceAuditEvaluationRunService.RunView runView() {
        LocalDateTime now = LocalDateTime.of(2026, 9, 17, 9, 0);
        return new ResumeEvidenceAuditEvaluationRunService.RunView(
                "run-1", ResumeEvidenceAuditEvaluationRun.Status.RUNNING,
                "resume-evidence-audit-dataset-v7",
                "RESUME_EVIDENCE_AUDITOR",
                "resume-evidence-audit-v4",
                "model-test",
                11, 0, 0, 11, null,
                now, now, null, List.of(), null);
    }
}
