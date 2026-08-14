package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.evaluation.RagEvaluationCaseType;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationCaseResult;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationComparisonReport;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationReport;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationService;
import com.javainterview.rag.knowledge.rag.RagRetrievalSource;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RagRetrievalEvaluationControllerTest {

    @Test
    void returnsTheBatchRetrievalEvaluationReport() throws Exception {
        RagRetrievalEvaluationService service = mock(RagRetrievalEvaluationService.class);
        RagRetrievalEvaluationCaseResult caseResult = new RagRetrievalEvaluationCaseResult(
                "java-collections-hashmap", "HashMap 问题", RagEvaluationCaseType.EXACT_TERM,
                List.of("java/collections.md"), List.of(List.of("Java 集合", "HashMap")),
                List.of(new RagRetrievalSource("java/collections.md", "version-1", 0,
                        List.of("Java 集合", "HashMap"), 0.91)),
                true, 1, true, 1.0, 1.0, 1, 1.0,
                false, false, false);
        when(service.evaluate()).thenReturn(new RagRetrievalEvaluationReport(
                5, 0.0, 0.60, 1, 1, 0, 1, 1.0,
                1, 1.0, 1.0, 1.0, 1, 1, 1.0, 0, 0.0,
                List.of(caseResult)));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new RagRetrievalEvaluationController(service)).build();

        mockMvc.perform(get("/api/knowledge/evaluations/retrieval"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topK").value(5))
                .andExpect(jsonPath("$.answerSimilarityThreshold").value(0.60))
                .andExpect(jsonPath("$.hitAt1Rate").value(1.0))
                .andExpect(jsonPath("$.meanRecallAt5").value(1.0))
                .andExpect(jsonPath("$.meanReciprocalRank").value(1.0))
                .andExpect(jsonPath("$.headingPathRecallRate").value(1.0))
                .andExpect(jsonPath("$.expectedSourceHitCount").value(1))
                .andExpect(jsonPath("$.cases[0].evaluationCaseId").value("java-collections-hashmap"))
                .andExpect(jsonPath("$.cases[0].type").value("EXACT_TERM"))
                .andExpect(jsonPath("$.cases[0].actualSources[0].source").value("java/collections.md"))
                .andExpect(jsonPath("$.cases[0].firstRelevantRank").value(1))
                .andExpect(jsonPath("$.cases[0].hitAt1").value(true));
    }

    @Test
    void returnsTheQueryDecompositionComparison() throws Exception {
        RagRetrievalEvaluationService service = mock(RagRetrievalEvaluationService.class);
        RagRetrievalEvaluationReport report = new RagRetrievalEvaluationReport(
                5, 0.0, 0.60, 0, 0, 0, 0, 0.0,
                0, 0.0, 0.0, 0.0, 0, 0, 0.0, 0, 0.0, List.of());
        when(service.compareQueryDecomposition()).thenReturn(new RagRetrievalEvaluationComparisonReport(
                "deterministic-clause-rrf-v1", 1, 2,
                Map.of("cross-map", List.of("原问题", "拆分问题")), report, report));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new RagRetrievalEvaluationController(service)).build();

        mockMvc.perform(get("/api/knowledge/evaluations/retrieval/query-decomposition"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.candidateStrategy").value("deterministic-clause-rrf-v1"))
                .andExpect(jsonPath("$.decomposedCaseCount").value(1))
                .andExpect(jsonPath("$.additionalQueryCount").value(2))
                .andExpect(jsonPath("$.retrievalQueries.cross-map[1]").value("拆分问题"))
                .andExpect(jsonPath("$.baseline.topK").value(5))
                .andExpect(jsonPath("$.candidate.topK").value(5));
    }
}
