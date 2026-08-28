package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.evaluation.FixedRagStructureEvaluationDataset;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RagStructureEvaluationDatasetControllerTest {

    @Test
    void returnsTheFixedStructureEvaluationCases() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new RagStructureEvaluationDatasetController(new FixedRagStructureEvaluationDataset())
        ).build();

        mockMvc.perform(get("/api/knowledge/evaluation-cases/structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].type").value("DOCUMENT_STRUCTURE"))
                .andExpect(jsonPath("$[0].expectedStructures[0].pageNumber").value(1))
                .andExpect(jsonPath("$[0].expectedStructures[0].blockType").value("CODE"));
    }
}
