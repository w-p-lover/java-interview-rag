package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.evaluation.RagEvaluationCandidate;
import com.javainterview.rag.knowledge.evaluation.RagEvaluationCandidateService;
import com.javainterview.rag.knowledge.feedback.RagAnswerFeedback;
import com.javainterview.rag.knowledge.feedback.RagAnswerFeedbackStore;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RagEvaluationCandidateControllerTest {
    @Test
    void rejectsFeedbackThatWasNotApprovedForEvaluation() throws Exception {
        RagEvaluationCandidateService service = mock(RagEvaluationCandidateService.class);
        RagAnswerFeedbackStore feedbackStore = mock(RagAnswerFeedbackStore.class);
        when(feedbackStore.findById(7L)).thenReturn(Optional.of(new RagAnswerFeedback(
                7L, "hash", true, "HYBRID", "HELPFUL", "EVALUATION", null, null,
                LocalDateTime.now(), LocalDateTime.now())));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new RagEvaluationCandidateController(service, feedbackStore)).build();

        mockMvc.perform(post("/api/knowledge/evaluation-candidates?feedbackId=7")
                        .contentType("application/json")
                        .content("{\"question\":\"HashMap 何时树化\",\"expectedAnswerPoints\":[\"长度达到 8\"]}"))
                .andExpect(status().isBadRequest());
    }
}
