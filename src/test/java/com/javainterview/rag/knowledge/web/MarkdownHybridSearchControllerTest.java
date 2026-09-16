package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.vector.MarkdownHybridSearchService;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import com.javainterview.rag.knowledge.vector.KnowledgeRetrievalUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MarkdownHybridSearchControllerTest {

    @Test
    void returnsHybridEvidenceAndFusionDiagnostics() throws Exception {
        MarkdownHybridSearchService service = mock(MarkdownHybridSearchService.class);
        MarkdownSearchResult result = new MarkdownSearchResult(
                "HashMap treeifyBin", "java/collections.md", "version-1", 2,
                List.of("Java", "集合"), 0.91);
        when(service.searchDetailed(eq("treeifyBin"), eq(5), eq(0.0)))
                .thenReturn(new MarkdownHybridSearchService.MarkdownHybridSearchResult(
                        "treeifyBin", 5, 5, 5,
                        List.of(new MarkdownHybridSearchService.MarkdownHybridSearchHit(
                                result, 2, 1, 0.91, 1.8, 0.032)), 4));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                new MarkdownHybridSearchController(service)).build();

        mockMvc.perform(get("/api/knowledge/search/hybrid").param("query", "treeifyBin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.denseCandidateCount").value(5))
                .andExpect(jsonPath("$.sparseCandidateCount").value(5))
                .andExpect(jsonPath("$.hits[0].denseRank").value(2))
                .andExpect(jsonPath("$.hits[0].sparseRank").value(1))
                .andExpect(jsonPath("$.hits[0].rrfScore").value(0.032));
    }

    @Test
    void returnsServiceUnavailableWhenBothRetrieversFail() throws Exception {
        MarkdownHybridSearchService service = mock(MarkdownHybridSearchService.class);
        when(service.searchDetailed(eq("HashMap"), eq(5), eq(0.0)))
                .thenThrow(new KnowledgeRetrievalUnavailableException("Dense 与 Sparse 检索均不可用"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                        new MarkdownHybridSearchController(service))
                .setControllerAdvice(new KnowledgeRetrievalExceptionHandler())
                .build();

        mockMvc.perform(get("/api/knowledge/search/hybrid").param("query", "HashMap"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Dense 与 Sparse 检索均不可用"));
    }
}
