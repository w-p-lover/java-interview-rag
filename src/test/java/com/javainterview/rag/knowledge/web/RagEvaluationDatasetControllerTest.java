package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.evaluation.FixedRagEvaluationDataset;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RagEvaluationDatasetControllerTest {

    @Test
    void returnsTheFixedEvaluationCases() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new RagEvaluationDatasetController(new FixedRagEvaluationDataset())
        ).build();

        mockMvc.perform(get("/api/knowledge/evaluation-cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(26))
                .andExpect(jsonPath("$[0].id").value("java-collections-hashmap"))
                .andExpect(jsonPath("$[0].type").value("EXACT_TERM"))
                .andExpect(jsonPath("$[0].expectedSources[0]").value("java/collections.md"))
                .andExpect(jsonPath("$[0].expectedHeadingPaths[0][0]").value("Java 集合"))
                .andExpect(jsonPath("$[0].expectedAnswerPoints").isNotEmpty());
    }
}
