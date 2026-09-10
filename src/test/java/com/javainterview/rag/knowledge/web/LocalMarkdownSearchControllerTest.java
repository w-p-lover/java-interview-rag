package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import com.javainterview.rag.knowledge.vector.MarkdownSparseSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LocalMarkdownSearchControllerTest {

    @Test
    void exposesTheSameSearchRouteForLocalProfileUsingSparseIndex() throws Exception {
        MarkdownSparseSearchService service = mock(MarkdownSparseSearchService.class);
        when(service.search("HashMap", 5)).thenReturn(List.of(new MarkdownSearchResult(
                "HashMap 使用数组与链表。", "smoke/local-demo.md", "version-1", 0,
                List.of("本地冒烟资料", "Java"), 2.4)));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new LocalMarkdownSearchController(service)).build();

        mockMvc.perform(get("/api/knowledge/search").param("query", "HashMap"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCount").value(1))
                .andExpect(jsonPath("$.results[0].source").value("smoke/local-demo.md"));
    }

    @Test
    void rejectsAnInvalidSimilarityThreshold() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new LocalMarkdownSearchController(mock(MarkdownSparseSearchService.class))).build();

        mockMvc.perform(get("/api/knowledge/search")
                        .param("query", "HashMap")
                        .param("similarityThreshold", "1.1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("相似度阈值必须在 0 到 1 之间"));
    }
}
