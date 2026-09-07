package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.resume.ResumeExpressionCoachResult;
import com.javainterview.rag.interview.resume.ResumeExpressionCoachService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResumeExpressionCoachControllerTest {

    @Test
    void returnsExpressionCandidates() throws Exception {
        ResumeExpressionCoachService service = mock(ResumeExpressionCoachService.class);
        when(service.coach(any())).thenReturn(new ResumeExpressionCoachResult(
                "request-1", "RESUME_EXPRESSION_COACH", "resume-expression-coach-v1",
                "Java 后端工程师", List.of(new ResumeExpressionCoachResult.Candidate(
                        "candidate-1", "负责订单服务开发。", "保留已确认职责。", List.of(1),
                        List.of(new com.javainterview.rag.interview.resume.ResumeEvidenceAuditResult.ConfirmedFact(
                                com.javainterview.rag.interview.resume.ResumeEvidenceAuditResult.FactType.RESPONSIBILITY,
                                "负责订单服务开发。", "负责订单服务开发。")), List.of()))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new ResumeExpressionCoachController(service)).build();

        mockMvc.perform(post("/api/interviews/resume-expression")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetRole":"Java 后端工程师",
                                 "confirmedFacts":[{"type":"RESPONSIBILITY","statement":"负责订单服务开发。","evidence":"负责订单服务开发。"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("RESUME_EXPRESSION_COACH"))
                .andExpect(jsonPath("$.candidates[0].supportingFactNumbers[0]").value(1));
    }

    @Test
    void mapsCoachValidationFailure() throws Exception {
        ResumeExpressionCoachService service = mock(ResumeExpressionCoachService.class);
        when(service.coach(any())).thenThrow(new IllegalStateException("表达候选包含未在事实证据中出现的数字"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new ResumeExpressionCoachController(service)).build();

        mockMvc.perform(post("/api/interviews/resume-expression")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmedFacts\":[]}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("表达候选包含未在事实证据中出现的数字"));
    }
}
