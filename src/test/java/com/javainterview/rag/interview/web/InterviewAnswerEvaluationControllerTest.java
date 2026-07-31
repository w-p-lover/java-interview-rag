package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.evaluation.FixedInterviewAnswerEvaluationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class InterviewAnswerEvaluationControllerTest {

    @Test
    void returnsFixedAnswerEvaluationReport() throws Exception {
        FixedInterviewAnswerEvaluationService service = mock(FixedInterviewAnswerEvaluationService.class);
        when(service.evaluate()).thenReturn(new FixedInterviewAnswerEvaluationService.Report(
                "prompt-v2", "rubric-v1", "model-test", 1, 1,
                List.of(new FixedInterviewAnswerEvaluationService.CaseResult(
                        "complete-answer", "SUCCEEDED", true, 65, 100,
                        88, true, true, null))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new InterviewAnswerEvaluationController(service)).build();

        mockMvc.perform(post("/api/interviews/evaluations/answers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promptVersion").value("prompt-v2"))
                .andExpect(jsonPath("$.passedCaseCount").value(1))
                .andExpect(jsonPath("$.cases[0].caseId").value("complete-answer"))
                .andExpect(jsonPath("$.cases[0].passed").value(true));
    }
}
