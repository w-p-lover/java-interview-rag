package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.tool.InterviewReviewAssistantService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InterviewReviewAssistantControllerTest {

    @Test
    void returnsAssistantAnswer() throws Exception {
        InterviewReviewAssistantService service = mock(InterviewReviewAssistantService.class);
        when(service.respond("查询会话 session-1 的任务"))
                .thenReturn("已查询到 1 条任务。");
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new InterviewReviewAssistantController(service)).build();

        mockMvc.perform(post("/api/interviews/review-assistant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"查询会话 session-1 的任务\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("已查询到 1 条任务。"));

        verify(service).respond("查询会话 session-1 的任务");
    }

    @Test
    void rejectsBlankMessage() throws Exception {
        InterviewReviewAssistantService service = mock(InterviewReviewAssistantService.class);
        when(service.respond(" ")).thenThrow(
                new IllegalArgumentException("复习助手消息不能为空"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new InterviewReviewAssistantController(service)).build();

        mockMvc.perform(post("/api/interviews/review-assistant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("复习助手消息不能为空"));
    }
}
