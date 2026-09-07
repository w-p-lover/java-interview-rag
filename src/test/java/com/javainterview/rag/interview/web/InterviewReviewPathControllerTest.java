package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.review.InterviewReviewPathResult;
import com.javainterview.rag.interview.review.InterviewReviewPathService;
import com.javainterview.rag.interview.review.InterviewReviewTaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InterviewReviewPathControllerTest {

    @Test
    void returnsDeterministicReviewPath() throws Exception {
        InterviewReviewPathService service = mock(InterviewReviewPathService.class);
        when(service.recommend(anyList())).thenReturn(new InterviewReviewPathResult(
                LocalDate.of(2026, 9, 7), 2, 1,
                List.of(new InterviewReviewPathResult.Recommendation(
                        "session-1", "task-1", 0, "如何定位慢查询？", "MySQL 调优",
                        List.of("缺少索引边界"), "补充执行计划。", InterviewReviewTaskStatus.PENDING,
                        null, 0, null, null, null, LocalDate.of(2026, 9, 7), 1,
                        "尚未练习，今天开始第一次复习"))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new InterviewReviewPathController(service)).build();

        mockMvc.perform(get("/api/interviews/review-path")
                        .param("sessionIds", "session-1", "session-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionCount").value(2))
                .andExpect(jsonPath("$.recommendations[0].taskId").value("task-1"))
                .andExpect(jsonPath("$.recommendations[0].intervalDays").value(1));
    }
}
