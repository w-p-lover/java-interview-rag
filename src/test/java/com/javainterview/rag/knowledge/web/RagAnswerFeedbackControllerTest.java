package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.feedback.RagAnswerFeedback;
import com.javainterview.rag.knowledge.feedback.RagAnswerFeedbackStore;
import com.javainterview.rag.knowledge.rag.RagRetrievalLogStore;
import com.javainterview.rag.knowledge.review.KnowledgeReviewTask;
import com.javainterview.rag.knowledge.review.KnowledgeReviewTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RagAnswerFeedbackControllerTest {

    @Test
    void createsReviewTaskOnlyForEvaluatedNegativeFeedback() throws Exception {
        RagAnswerFeedbackStore feedbackStore = mock(RagAnswerFeedbackStore.class);
        RagRetrievalLogStore retrievalLogStore = mock(RagRetrievalLogStore.class);
        KnowledgeReviewTaskService reviewTaskService = mock(KnowledgeReviewTaskService.class);
        when(feedbackStore.findById(7L)).thenReturn(Optional.of(new RagAnswerFeedback(
                7L, "hash", true, "HYBRID", "NOT_HELPFUL", "EVALUATION", null,
                "EVIDENCE_INSUFFICIENT", LocalDateTime.now(), LocalDateTime.now())));
        when(reviewTaskService.create(any(), any(), any(), any())).thenReturn(
                new KnowledgeReviewTask("task-1", "HashMap 是什么", "补充树化条件",
                        com.javainterview.rag.knowledge.rag.RagAnswerClaimEvidenceStatus.EVIDENCE_INSUFFICIENT,
                        java.util.List.of(1), com.javainterview.rag.knowledge.review.KnowledgeReviewTaskStatus.PENDING,
                        null, LocalDateTime.now(), null));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new RagAnswerFeedbackController(feedbackStore, retrievalLogStore, reviewTaskService)).build();

        mockMvc.perform(post("/api/knowledge/answer/feedback/7/review-task")
                        .contentType("application/json")
                        .content("{\"question\":\"HashMap 是什么\",\"claim\":\"补充树化条件\",\"evidenceStatus\":\"EVIDENCE_INSUFFICIENT\",\"sourceNumbers\":[1]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("task-1"));
    }
}
