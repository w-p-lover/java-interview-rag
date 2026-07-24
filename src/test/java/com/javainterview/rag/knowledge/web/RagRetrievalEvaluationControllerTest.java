package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationCaseResult;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationReport;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationService;
import com.javainterview.rag.knowledge.rag.RagRetrievalSource;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RagRetrievalEvaluationControllerTest {

    @Test
    void returnsTheBatchRetrievalEvaluationReport() throws Exception {
        RagRetrievalEvaluationService service = mock(RagRetrievalEvaluationService.class);
        when(service.evaluate()).thenReturn(new RagRetrievalEvaluationReport(5, 0.0, 1, 1, 1.0,
                List.of(new RagRetrievalEvaluationCaseResult(
                        "java-collections-hashmap", "HashMap 问题", List.of("java/collections.md"),
                        List.of(new RagRetrievalSource("java/collections.md", "version-1", 0,
                                List.of("Java", "集合"), 0.91)), true
                ))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new RagRetrievalEvaluationController(service)).build();

        mockMvc.perform(get("/api/knowledge/evaluations/retrieval"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topK").value(5))
                .andExpect(jsonPath("$.expectedSourceHitCount").value(1))
                .andExpect(jsonPath("$.cases[0].evaluationCaseId").value("java-collections-hashmap"))
                .andExpect(jsonPath("$.cases[0].actualSources[0].source").value("java/collections.md"))
                .andExpect(jsonPath("$.cases[0].expectedSourceMatched").value(true));
    }
}
