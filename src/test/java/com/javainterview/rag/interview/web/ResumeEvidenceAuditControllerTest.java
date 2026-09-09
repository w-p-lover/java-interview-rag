package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.resume.ResumeEvidenceAuditRequestLog;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditRequestLogQueryService;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditResult;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditService;
import org.springframework.ai.retry.NonTransientAiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
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

class ResumeEvidenceAuditControllerTest {

    @Test
    void returnsSeparatedVerifiedAuditSections() throws Exception {
        ResumeEvidenceAuditService service = mock(ResumeEvidenceAuditService.class);
        ResumeEvidenceAuditRequestLogQueryService logQueryService =
                mock(ResumeEvidenceAuditRequestLogQueryService.class);
        when(service.audit("项目：使用 Spring Boot 构建面试系统"))
                .thenReturn(new ResumeEvidenceAuditResult(
                        "request-1",
                        "RESUME_EVIDENCE_AUDITOR",
                        "resume-evidence-audit-v1",
                        List.of(new ResumeEvidenceAuditResult.ConfirmedFact(
                                ResumeEvidenceAuditResult.FactType.PROJECT,
                                "使用 Spring Boot 构建面试系统",
                                "使用 Spring Boot 构建面试系统")),
                        List.of("缺少量化结果"),
                        List.of("项目范围需要核对"),
                        List.of("补充可核验的质量指标")));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new ResumeEvidenceAuditController(service, logQueryService)).build();

        mockMvc.perform(post("/api/interviews/resume-audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resumeText":"项目：使用 Spring Boot 构建面试系统"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("request-1"))
                .andExpect(jsonPath("$.role").value("RESUME_EVIDENCE_AUDITOR"))
                .andExpect(jsonPath("$.confirmedFacts[0].type").value("PROJECT"))
                .andExpect(jsonPath("$.confirmedFacts[0].evidence")
                        .value("使用 Spring Boot 构建面试系统"))
                .andExpect(jsonPath("$.missingInformation[0]").value("缺少量化结果"))
                .andExpect(jsonPath("$.risks[0]").value("项目范围需要核对"))
                .andExpect(jsonPath("$.suggestions[0]").value("补充可核验的质量指标"));
    }

    @Test
    void mapsInputAndEvidenceFailuresToDifferentHttpStatuses() throws Exception {
        ResumeEvidenceAuditService service = mock(ResumeEvidenceAuditService.class);
        ResumeEvidenceAuditRequestLogQueryService logQueryService =
                mock(ResumeEvidenceAuditRequestLogQueryService.class);
        when(service.audit(" ")).thenThrow(new IllegalArgumentException("简历内容不能为空"));
        when(service.audit("虚构证据"))
                .thenThrow(new IllegalStateException("模型返回的简历事实证据不在原始简历中"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new ResumeEvidenceAuditController(service, logQueryService)).build();

        mockMvc.perform(post("/api/interviews/resume-audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resumeText\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("简历内容不能为空"));
        mockMvc.perform(post("/api/interviews/resume-audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resumeText\":\"虚构证据\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("模型返回的简历事实证据不在原始简历中"));
    }

    @Test
    void mapsModelAuthorizationFailureToSafeBadGatewayMessage() throws Exception {
        ResumeEvidenceAuditService service = mock(ResumeEvidenceAuditService.class);
        ResumeEvidenceAuditRequestLogQueryService logQueryService =
                mock(ResumeEvidenceAuditRequestLogQueryService.class);
        when(service.audit("真实简历")).thenThrow(new NonTransientAiException("模型服务返回 HTTP 401"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new ResumeEvidenceAuditController(service, logQueryService)).build();

        mockMvc.perform(post("/api/interviews/resume-audits")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resumeText\":\"真实简历\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message")
                        .value("模型服务鉴权失败，请检查 API Key 和模型权限配置"));
    }

    @Test
    void returnsMetadataOnlyRequestLogs() throws Exception {
        ResumeEvidenceAuditService service = mock(ResumeEvidenceAuditService.class);
        ResumeEvidenceAuditRequestLogQueryService logQueryService =
                mock(ResumeEvidenceAuditRequestLogQueryService.class);
        when(logQueryService.findRecent(20)).thenReturn(List.of(
                new ResumeEvidenceAuditRequestLog(
                        1L,
                        "request-1",
                        "RESUME_EVIDENCE_AUDITOR",
                        "resume-evidence-audit-v1",
                        "qwen3.7-plus",
                        ResumeEvidenceAuditRequestLog.Status.SUCCEEDED,
                        100,
                        20,
                        120,
                        500,
                        null,
                        LocalDateTime.of(2026, 8, 14, 10, 30))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new ResumeEvidenceAuditController(service, logQueryService)).build();

        mockMvc.perform(get("/api/interviews/resume-audits/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value("request-1"))
                .andExpect(jsonPath("$[0].promptVersion").value("resume-evidence-audit-v1"))
                .andExpect(jsonPath("$[0].totalTokens").value(120))
                .andExpect(jsonPath("$[0].resumeText").doesNotExist())
                .andExpect(jsonPath("$[0].prompt").doesNotExist())
                .andExpect(jsonPath("$[0].result").doesNotExist());
    }
}
