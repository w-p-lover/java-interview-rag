package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.consistency.KnowledgeIndexConsistencyReport;
import com.javainterview.rag.knowledge.consistency.KnowledgeIndexConsistencyService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.NoSuchElementException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class KnowledgeIndexConsistencyControllerTest {

    @Test
    void returnsStructuredConsistencyReport() throws Exception {
        KnowledgeIndexConsistencyService service = mock(KnowledgeIndexConsistencyService.class);
        when(service.inspect("java/basic.md")).thenReturn(new KnowledgeIndexConsistencyReport(
                "java/basic.md", "version-1", 2, true,
                new KnowledgeIndexConsistencyReport.IndexingStateCheck(false, null, null, null, null),
                consistentStore(), consistentStore()));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new KnowledgeIndexConsistencyController(service)).build();

        mockMvc.perform(get("/api/knowledge/documents/consistency")
                        .param("source", "java/basic.md"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consistent").value(true))
                .andExpect(jsonPath("$.expectedChunkCount").value(2))
                .andExpect(jsonPath("$.indexingState.tracked").value(false))
                .andExpect(jsonPath("$.dense.status").value("CONSISTENT"))
                .andExpect(jsonPath("$.sparse.currentVersionChunkCount").value(2));
    }

    @Test
    void mapsInvalidAndMissingSources() throws Exception {
        KnowledgeIndexConsistencyService service = mock(KnowledgeIndexConsistencyService.class);
        when(service.inspect(" ")).thenThrow(new IllegalArgumentException("文档来源不能为空"));
        when(service.inspect("java/missing.md")).thenThrow(new NoSuchElementException("资料不存在"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new KnowledgeIndexConsistencyController(service)).build();

        mockMvc.perform(get("/api/knowledge/documents/consistency").param("source", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("文档来源不能为空"));
        mockMvc.perform(get("/api/knowledge/documents/consistency")
                        .param("source", "java/missing.md"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("资料不存在"));
    }

    private KnowledgeIndexConsistencyReport.StoreCheck consistentStore() {
        return new KnowledgeIndexConsistencyReport.StoreCheck(
                KnowledgeIndexConsistencyReport.StoreStatus.CONSISTENT,
                2L, 2L, 0L, 2L, true, true, null);
    }
}
