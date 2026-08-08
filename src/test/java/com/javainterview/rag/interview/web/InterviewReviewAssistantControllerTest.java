package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.tool.InterviewAssistantRequestLog;
import com.javainterview.rag.interview.tool.InterviewAssistantRequestLogQueryService;
import com.javainterview.rag.interview.tool.InterviewReviewAssistantService;
import com.javainterview.rag.interview.tool.InterviewReviewAssistantResult;
import com.javainterview.rag.interview.tool.InterviewReviewScheduleConfirmation;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InterviewReviewAssistantControllerTest {

    @Test
    void returnsAssistantAnswer() throws Exception {
        InterviewReviewAssistantService service = mock(InterviewReviewAssistantService.class);
        when(service.respond("session-1", "查询本场任务", null))
                .thenReturn(new InterviewReviewAssistantResult(
                        "request-1", "已查询到 1 条任务。"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new InterviewReviewAssistantController(
                        service, mock(InterviewAssistantRequestLogQueryService.class))).build();

        mockMvc.perform(post("/api/interviews/review-assistant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"session-1\",\"message\":\"查询本场任务\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("request-1"))
                .andExpect(jsonPath("$.answer").value("已查询到 1 条任务。"));

        verify(service).respond("session-1", "查询本场任务", null);
    }

    @Test
    void forwardsExactScheduleConfirmation() throws Exception {
        InterviewReviewAssistantService service = mock(InterviewReviewAssistantService.class);
        InterviewReviewScheduleConfirmation confirmation =
                new InterviewReviewScheduleConfirmation(
                        "session-1", "task-1", LocalDate.of(2026, 8, 12));
        when(service.respond("session-1", "确认改期", confirmation))
                .thenReturn(new InterviewReviewAssistantResult(
                        "request-2", "已把计划日期设置为 2026-08-12。"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new InterviewReviewAssistantController(
                        service, mock(InterviewAssistantRequestLogQueryService.class))).build();

        mockMvc.perform(post("/api/interviews/review-assistant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId":"session-1",
                                  "message":"确认改期",
                                  "scheduleConfirmation":{
                                    "sessionId":"session-1",
                                    "taskId":"task-1",
                                    "plannedReviewDate":"2026-08-12"
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("request-2"));

        verify(service).respond("session-1", "确认改期", confirmation);
    }

    @Test
    void rejectsBlankMessage() throws Exception {
        InterviewReviewAssistantService service = mock(InterviewReviewAssistantService.class);
        when(service.respond("session-1", " ", null)).thenThrow(
                new IllegalArgumentException("复习助手消息不能为空"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new InterviewReviewAssistantController(
                        service, mock(InterviewAssistantRequestLogQueryService.class))).build();

        mockMvc.perform(post("/api/interviews/review-assistant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessionId\":\"session-1\",\"message\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("复习助手消息不能为空"));
    }

    @Test
    void rejectsMissingSessionId() throws Exception {
        InterviewReviewAssistantService service = mock(InterviewReviewAssistantService.class);
        when(service.respond(null, "查询本场任务", null)).thenThrow(
                new IllegalArgumentException("复习助手会话 ID 不能为空"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new InterviewReviewAssistantController(
                        service, mock(InterviewAssistantRequestLogQueryService.class))).build();

        mockMvc.perform(post("/api/interviews/review-assistant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"查询本场任务\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("复习助手会话 ID 不能为空"));
    }

    @Test
    void returnsRecentAssistantRequestLogs() throws Exception {
        InterviewAssistantRequestLogQueryService queryService =
                mock(InterviewAssistantRequestLogQueryService.class);
        when(queryService.findRecent(20)).thenReturn(List.of(
                new InterviewAssistantRequestLog(
                        1L,
                        "request-1",
                        "qwen3.7-plus",
                        InterviewAssistantRequestLog.Status.SUCCEEDED,
                        1,
                        120,
                        30,
                        150,
                        800,
                        true,
                        null,
                        LocalDateTime.of(2026, 8, 5, 10, 30))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new InterviewReviewAssistantController(
                        mock(InterviewReviewAssistantService.class), queryService)).build();

        mockMvc.perform(get("/api/interviews/review-assistant/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].requestId").value("request-1"))
                .andExpect(jsonPath("$[0].toolExecutionCount").value(1))
                .andExpect(jsonPath("$[0].totalTokens").value(150))
                .andExpect(jsonPath("$[0].finalResponsePresent").value(true));

        verify(queryService).findRecent(20);
    }
}
