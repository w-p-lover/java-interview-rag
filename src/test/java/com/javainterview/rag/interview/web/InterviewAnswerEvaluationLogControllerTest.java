package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluationLog;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluationLogQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InterviewAnswerEvaluationLogControllerTest {

    @Test
    void returnsBaselineLogSummaryByDefault() throws Exception {
        InterviewAnswerEvaluationLogQueryService service = mock(InterviewAnswerEvaluationLogQueryService.class);
        when(service.findRecent(InterviewAnswerEvaluationLog.Mode.BASELINE, 20)).thenReturn(
                new InterviewAnswerEvaluationLogQueryService.Report(
                        InterviewAnswerEvaluationLog.Mode.BASELINE,
                        1,
                        List.of(new InterviewAnswerEvaluationLogQueryService.VersionSummary(
                                "prompt-v2", "rubric-v1", "model-test",
                                1, 1, 0, 1, 80, 80, 80.0,
                                120.0, 120, Map.of())),
                        List.of()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new InterviewAnswerEvaluationLogController(service)).build();

        mockMvc.perform(get("/api/interviews/evaluations/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("BASELINE"))
                .andExpect(jsonPath("$.returnedCount").value(1))
                .andExpect(jsonPath("$.versionSummaries[0].promptVersion").value("prompt-v2"))
                .andExpect(jsonPath("$.versionSummaries[0].averageOverallScore").value(80.0));

        verify(service).findRecent(InterviewAnswerEvaluationLog.Mode.BASELINE, 20);
    }

    @Test
    void rejectsUnknownEvaluationMode() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new InterviewAnswerEvaluationLogController(mock(InterviewAnswerEvaluationLogQueryService.class)))
                .build();

        mockMvc.perform(get("/api/interviews/evaluations/logs").param("mode", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("评价模式必须是 INTERVIEW、REVIEW 或 BASELINE"));
    }
}
