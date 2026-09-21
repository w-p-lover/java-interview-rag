package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.matching.ResumeJobMatchResult;
import com.javainterview.rag.interview.matching.ResumeJobMatchService;
import com.javainterview.rag.interview.matching.ResumeJobMatchRequestLogQueryService;
import com.javainterview.rag.interview.matching.ResumeJobMatchRequestLog;
import com.javainterview.rag.interview.resume.ResumeAnalysisResultService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResumeJobMatchControllerTest {

    @Test
    void returnsStructuredJobMatchAssessment() throws Exception {
        ResumeJobMatchService service = mock(ResumeJobMatchService.class);
        ResumeJobMatchRequestLogQueryService logQueryService = mock(ResumeJobMatchRequestLogQueryService.class);
        when(service.match(org.mockito.ArgumentMatchers.any())).thenReturn(new ResumeJobMatchResult(
                "request-1", "JOB_MATCH_ADVISOR", "job-match-v1", "已有后端基础。",
                List.of(new ResumeJobMatchResult.Assessment(
                        "熟悉 Spring Boot", "熟悉 Spring Boot",
                        ResumeJobMatchResult.MatchStatus.MATCHED,
                        "使用 Spring Boot 开发订单服务", "已有直接项目证据。"))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new ResumeJobMatchController(service, logQueryService,
                        mock(ResumeAnalysisResultService.class))).build();

        mockMvc.perform(post("/api/interviews/job-matches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetRole":"Java 后端工程师","jobDescription":"熟悉 Spring Boot。",
                                 "confirmedFacts":[{"type":"TECHNOLOGY","statement":"使用 Spring Boot 开发订单服务",
                                 "evidence":"使用 Spring Boot 开发订单服务"}]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("JOB_MATCH_ADVISOR"))
                .andExpect(jsonPath("$.assessments[0].status").value("MATCHED"))
                .andExpect(jsonPath("$.assessments[0].requirementEvidence").value("熟悉 Spring Boot"));
    }

    @Test
    void mapsValidationAndModelFailures() throws Exception {
        ResumeJobMatchService service = mock(ResumeJobMatchService.class);
        ResumeJobMatchRequestLogQueryService logQueryService = mock(ResumeJobMatchRequestLogQueryService.class);
        when(service.match(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new IllegalArgumentException("岗位描述不能为空"))
                .thenThrow(new IllegalStateException("模型返回的简历证据不在已确认事实中"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new ResumeJobMatchController(service, logQueryService,
                        mock(ResumeAnalysisResultService.class))).build();

        String request = "{\"targetRole\":\"Java 后端工程师\",\"jobDescription\":\"x\",\"confirmedFacts\":[]}";
        mockMvc.perform(post("/api/interviews/job-matches")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("岗位描述不能为空"));
        mockMvc.perform(post("/api/interviews/job-matches")
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("模型返回的简历证据不在已确认事实中"));
    }

    @Test
    void returnsMetadataOnlyJobMatchLogs() throws Exception {
        ResumeJobMatchService service = mock(ResumeJobMatchService.class);
        ResumeJobMatchRequestLogQueryService logQueryService = mock(ResumeJobMatchRequestLogQueryService.class);
        when(logQueryService.findRecent(20)).thenReturn(List.of(new ResumeJobMatchRequestLog(
                1L, "request-1", "JOB_MATCH_ADVISOR", "job-match-v1", "qwen3.7-plus",
                ResumeJobMatchRequestLog.Status.SUCCEEDED, 100, 20, 120, 500, null,
                LocalDateTime.of(2026, 8, 31, 10, 30))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new ResumeJobMatchController(service, logQueryService,
                        mock(ResumeAnalysisResultService.class))).build();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/interviews/job-matches/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].roleName").value("JOB_MATCH_ADVISOR"))
                .andExpect(jsonPath("$[0].totalTokens").value(120))
                .andExpect(jsonPath("$[0].jobDescription").doesNotExist())
                .andExpect(jsonPath("$[0].resumeEvidence").doesNotExist());
    }
}
