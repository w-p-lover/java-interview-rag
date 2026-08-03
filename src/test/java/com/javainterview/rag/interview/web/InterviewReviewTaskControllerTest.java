package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.review.InterviewReviewKnowledgeSource;
import com.javainterview.rag.interview.review.InterviewReviewTask;
import com.javainterview.rag.interview.review.InterviewReviewAttempt;
import com.javainterview.rag.interview.review.InterviewReviewSessionHistory;
import com.javainterview.rag.interview.review.InterviewReviewTaskHistory;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @Test
    void completesReviewTaskAndMapsMissingTaskToNotFound() throws Exception {
        InterviewReviewTaskService service = mock(InterviewReviewTaskService.class);
        when(service.complete("session-1", "task-1")).thenReturn(completedTask());
        when(service.complete("session-1", "missing")).thenThrow(
                new NoSuchElementException("复习任务不存在"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new InterviewReviewTaskController(service)).build();

        mockMvc.perform(patch("/api/interviews/sessions/session-1/review-tasks/task-1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty());

        mockMvc.perform(patch("/api/interviews/sessions/session-1/review-tasks/missing/complete"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("复习任务不存在"));
    }

    @Test
    void listsAndSubmitsReviewAttempts() throws Exception {
        InterviewReviewTaskService service = mock(InterviewReviewTaskService.class);
        when(service.findAttempts("session-1", "task-1")).thenReturn(List.of(attempt()));
        when(service.submitAttempt("session-1", "task-1", "补充失败补偿", 1))
                .thenReturn(attempt());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new InterviewReviewTaskController(service)).build();

        mockMvc.perform(get("/api/interviews/sessions/session-1/review-tasks/task-1/attempts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].attemptNumber").value(1))
                .andExpect(jsonPath("$[0].scoreDeltaFromOriginal").value(5));

        mockMvc.perform(post("/api/interviews/sessions/session-1/review-tasks/task-1/attempts")
                        .contentType("application/json")
                        .content("{\"answer\":\"补充失败补偿\",\"expectedAttemptNumber\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promptVersion").value("prompt-v2"));
    }

    @Test
    void returnsPersistedReviewHistoryForOneSession() throws Exception {
        InterviewReviewTaskService service = mock(InterviewReviewTaskService.class);
        InterviewReviewTaskHistory taskHistory = new InterviewReviewTaskHistory(
                "task-1", 0, 1, 80, 85, 85, 85, 5,
                List.of(), LocalDateTime.of(2026, 7, 30, 9, 30), List.of(attempt()));
        when(service.findAttemptHistory("session-1")).thenReturn(
                new InterviewReviewSessionHistory("session-1", 1, 1, 1, 1,
                        List.of(taskHistory)));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new InterviewReviewTaskController(service)).build();

        mockMvc.perform(get("/api/interviews/sessions/session-1/review-tasks/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAttemptCount").value(1))
                .andExpect(jsonPath("$.tasks[0].bestOverallScore").value(85))
                .andExpect(jsonPath("$.tasks[0].unresolvedGaps").isEmpty())
                .andExpect(jsonPath("$.tasks[0].attempts[0].attemptNumber").value(1));
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
                LocalDateTime.of(2026, 7, 29, 9, 3),
                null
        );
    }

    private InterviewReviewTask completedTask() {
        InterviewReviewTask task = task();
        return new InterviewReviewTask(
                task.id(),
                task.sessionId(),
                task.questionIndex(),
                task.question(),
                task.objective(),
                task.gaps(),
                task.improvementSuggestion(),
                task.knowledgeGrounded(),
                task.knowledgeSources(),
                InterviewReviewTaskStatus.COMPLETED,
                task.createdAt(),
                LocalDateTime.of(2026, 7, 30, 9, 30)
        );
    }

    private InterviewReviewAttempt attempt() {
        return new InterviewReviewAttempt(
                "attempt-1", "session-1", "task-1", 0, 1, "补充失败补偿",
                new com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluation(
                        85, 85, 85, 85, List.of("补充了失败补偿"), List.of(),
                        "继续补充监控信号。", true),
                true,
                List.of(new InterviewReviewKnowledgeSource(
                        "rag/vector-lifecycle.md", "version-1", 2,
                        List.of("向量生命周期"), 0.82)),
                "prompt-v2", "rubric-v1", "model-test", 80, null, 5, null,
                LocalDateTime.of(2026, 7, 30, 9, 30));
    }
}
