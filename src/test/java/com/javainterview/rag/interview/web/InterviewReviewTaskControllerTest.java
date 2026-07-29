package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.review.InterviewReviewKnowledgeSource;
import com.javainterview.rag.interview.review.InterviewReviewTask;
import com.javainterview.rag.interview.review.InterviewReviewTaskService;
import com.javainterview.rag.interview.review.InterviewReviewTaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InterviewReviewTaskControllerTest {

    @Test
    void generatesAndFindsReviewTasksForACompletedSession() throws Exception {
        InterviewReviewTaskService service = mock(InterviewReviewTaskService.class);
        when(service.generate("session-1")).thenReturn(List.of(task()));
        when(service.findBySessionId("session-1")).thenReturn(List.of(task()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new InterviewReviewTaskController(service)).build();

        mockMvc.perform(post("/api/interviews/sessions/session-1/review-tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].questionIndex").value(0))
                .andExpect(jsonPath("$[0].gaps[0]").value("缺少失败补偿细节"))
                .andExpect(jsonPath("$[0].knowledgeSources[0].source")
                        .value("rag/vector-lifecycle.md"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));

        mockMvc.perform(get("/api/interviews/sessions/session-1/review-tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("task-1"));
    }

    @Test
    void mapsIncompleteAndMissingSessionsToHttpStatus() throws Exception {
        InterviewReviewTaskService service = mock(InterviewReviewTaskService.class);
        when(service.generate("in-progress")).thenThrow(
                new IllegalArgumentException("面试会话尚未完成，不能生成复习任务"));
        when(service.findBySessionId("missing")).thenThrow(
                new NoSuchElementException("面试会话不存在"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new InterviewReviewTaskController(service)).build();

        mockMvc.perform(post("/api/interviews/sessions/in-progress/review-tasks"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("面试会话尚未完成，不能生成复习任务"));

        mockMvc.perform(get("/api/interviews/sessions/missing/review-tasks"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("面试会话不存在"));
    }

    private InterviewReviewTask task() {
        return new InterviewReviewTask(
                "task-1",
                "session-1",
                0,
                "如何保证向量更新一致性？",
                "向量生命周期",
                List.of("缺少失败补偿细节"),
                "补充异常分支和恢复方式。",
                true,
                List.of(new InterviewReviewKnowledgeSource(
                        "rag/vector-lifecycle.md",
                        "version-1",
                        2,
                        List.of("向量生命周期", "更新"),
                        0.82)),
                InterviewReviewTaskStatus.PENDING,
                LocalDateTime.of(2026, 7, 29, 9, 3)
        );
    }
}
