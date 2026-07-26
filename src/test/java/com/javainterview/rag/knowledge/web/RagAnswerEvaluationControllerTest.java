package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.evaluation.RagAnswerEvaluationCaseResult;
import com.javainterview.rag.knowledge.evaluation.RagAnswerEvaluationReport;
import com.javainterview.rag.knowledge.evaluation.RagAnswerEvaluationService;
import com.javainterview.rag.knowledge.rag.RagRetrievalSource;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RagAnswerEvaluationControllerTest {

    @Test
    void returnsTheBatchAnswerPointEvaluationReport() throws Exception {
        RagAnswerEvaluationService service = mock(RagAnswerEvaluationService.class);
        when(service.evaluate()).thenReturn(new RagAnswerEvaluationReport(
                "normalized-literal-containment-v1", 1, 1, 1, 2, 1, 0.5,
                List.of(new RagAnswerEvaluationCaseResult(
                        "hashmap", "HashMap 问题", "平均查询复杂度为 O(1)。", true,
                        List.of("java/collections.md"),
                        List.of(new RagRetrievalSource("java/collections.md", "version-1", 0,
                                List.of("Java", "集合"), 0.91)), true,
                        List.of("平均查询复杂度为 O(1)", "数组容量至少为 64"),
                        List.of("平均查询复杂度为 O(1)"), List.of("数组容量至少为 64"), 0.5
                ))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new RagAnswerEvaluationController(service)).build();

        mockMvc.perform(post("/api/knowledge/evaluations/answers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matchingMethod").value("normalized-literal-containment-v1"))
                .andExpect(jsonPath("$.matchedAnswerPointCount").value(1))
                .andExpect(jsonPath("$.cases[0].evaluationCaseId").value("hashmap"))
                .andExpect(jsonPath("$.cases[0].expectedSourceMatched").value(true))
                .andExpect(jsonPath("$.cases[0].matchedAnswerPoints[0]")
                        .value("平均查询复杂度为 O(1)"))
                .andExpect(jsonPath("$.cases[0].unmatchedAnswerPoints[0]")
                        .value("数组容量至少为 64"));
    }
}
