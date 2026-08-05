package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.tool.InterviewToolExecutionLog;
import com.javainterview.rag.interview.tool.InterviewToolExecutionLogQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InterviewToolExecutionLogControllerTest {

    @Test
    void returnsRecentLogsForOneSession() throws Exception {
        InterviewToolExecutionLogQueryService service =
                mock(InterviewToolExecutionLogQueryService.class);
        when(service.findRecent("session-1", 20)).thenReturn(List.of(
                new InterviewToolExecutionLog(
                        1L,
                        "execution-1",
                        "request-1",
                        "list_review_tasks",
                        "session-1",
                        InterviewToolExecutionLog.Status.SUCCEEDED,
                        2,
                        15,
                        null,
                        LocalDateTime.of(2026, 8, 5, 9, 0))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new InterviewToolExecutionLogController(service)).build();

        mockMvc.perform(get("/api/interviews/sessions/session-1/tool-executions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].executionId").value("execution-1"))
                .andExpect(jsonPath("$[0].assistantRequestId").value("request-1"))
                .andExpect(jsonPath("$[0].toolName").value("list_review_tasks"))
                .andExpect(jsonPath("$[0].resultCount").value(2));

        verify(service).findRecent("session-1", 20);
    }

    @Test
    void rejectsInvalidLimit() throws Exception {
        InterviewToolExecutionLogQueryService service =
                mock(InterviewToolExecutionLogQueryService.class);
        when(service.findRecent("session-1", 101))
                .thenThrow(new IllegalArgumentException(
                        "工具执行日志查询数量必须在 1 到 100 之间"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new InterviewToolExecutionLogController(service)).build();

        mockMvc.perform(get("/api/interviews/sessions/session-1/tool-executions")
                        .param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("工具执行日志查询数量必须在 1 到 100 之间"));
    }
}
