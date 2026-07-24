package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MarkdownSearchControllerTest {

    @Test
    void returnsSimilarityMatchesWithTheirRealSourceMetadata() throws Exception {
        MarkdownSimilaritySearchService service = mock(MarkdownSimilaritySearchService.class);
        when(service.search("HashMap 底层结构", 3, 0.65)).thenReturn(List.of(
                new MarkdownSearchResult(
                        "HashMap 在 Java 8 中使用数组、链表和红黑树。",
                        "java/collections.md",
                        "version-1",
                        2,
                        List.of("Java", "集合", "HashMap"),
                        0.91
                )
        ));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MarkdownSearchController(service)).build();

        mockMvc.perform(get("/api/knowledge/search")
                        .param("query", "HashMap 底层结构")
                        .param("topK", "3")
                        .param("similarityThreshold", "0.65"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("HashMap 底层结构"))
                .andExpect(jsonPath("$.resultCount").value(1))
                .andExpect(jsonPath("$.results[0].source").value("java/collections.md"))
                .andExpect(jsonPath("$.results[0].documentVersion").value("version-1"))
                .andExpect(jsonPath("$.results[0].chunkOrder").value(2))
                .andExpect(jsonPath("$.results[0].headingPath[2]").value("HashMap"))
                .andExpect(jsonPath("$.results[0].similarity").value(0.91));
    }

    @Test
    void returnsBadRequestForAnInvalidQuery() throws Exception {
        MarkdownSimilaritySearchService service = mock(MarkdownSimilaritySearchService.class);
        when(service.search(" ", 5, 0.0))
                .thenThrow(new IllegalArgumentException("检索问题不能为空"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MarkdownSearchController(service)).build();

        mockMvc.perform(get("/api/knowledge/search").param("query", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("检索问题不能为空"));
    }
}
