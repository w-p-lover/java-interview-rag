package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.rag.RagRetrievalLog;
import com.javainterview.rag.knowledge.rag.RagRetrievalLogStatus;
import com.javainterview.rag.knowledge.rag.RagRetrievalLogStore;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RagRetrievalLogControllerTest {

    @Test
    void returnsRecentRetrievalLogs() throws Exception {
        RagRetrievalLogStore store = mock(RagRetrievalLogStore.class);
        when(store.findRecent(20)).thenReturn(List.of(new RagRetrievalLog(5L, "HashMap", 5,
                0.60, "rag-answer-v1", RagRetrievalLogStatus.ANSWERED, true,
                List.of(), 12, null, LocalDateTime.of(2026, 7, 24, 10, 2))));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new RagRetrievalLogController(store)).build();

        mockMvc.perform(get("/api/knowledge/retrieval-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].status").value("ANSWERED"))
                .andExpect(jsonPath("$[0].elapsedMillis").value(12));

        verify(store).findRecent(20);
    }

    @Test
    void rejectsInvalidLogLimit() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new RagRetrievalLogController(mock(RagRetrievalLogStore.class)))
                .build();

        mockMvc.perform(get("/api/knowledge/retrieval-logs").param("limit", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("日志查询数量必须在 1 到 100 之间"));
    }
}
