package com.javainterview.rag.knowledge.vector;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarkdownHybridSearchServiceTest {

    @Test
    void fusesDenseAndSparseRanksAndExposesBothEvidenceSources() {
        MarkdownSimilaritySearchService dense = mock(MarkdownSimilaritySearchService.class);
        MarkdownSparseSearchService sparse = mock(MarkdownSparseSearchService.class);
        MarkdownHybridSearchService service = new MarkdownHybridSearchService(dense, sparse);
        MarkdownSearchResult denseTop = result("dense-top", 0, 0.92);
        MarkdownSearchResult shared = result("shared", 1, 0.81);
        MarkdownSearchResult sparseTop = result("sparse-top", 2, 4.2);
        when(dense.search("HashMap", 8, 0.6)).thenReturn(List.of(denseTop, shared));
        when(sparse.search("HashMap", 8)).thenReturn(List.of(shared, sparseTop));

        MarkdownHybridSearchService.MarkdownHybridSearchResult response =
                service.searchDetailed("HashMap", 2, 0.6);

        assertEquals(2, response.denseCandidateCount());
        assertEquals(2, response.sparseCandidateCount());
        assertEquals("shared", response.hits().get(0).result().content());
        assertEquals(2, response.hits().get(0).denseRank());
        assertEquals(1, response.hits().get(0).sparseRank());
        assertEquals("dense-top", response.hits().get(1).result().content());
        assertEquals(null, response.hits().get(1).sparseRank());
        assertEquals(MarkdownHybridSearchService.RetrievalStrategy.HYBRID, response.strategy());
        assertEquals(false, response.degraded());
    }

    @Test
    void fallsBackToSparseWhenDenseSearchFails() {
        MarkdownSimilaritySearchService dense = mock(MarkdownSimilaritySearchService.class);
        MarkdownSparseSearchService sparse = mock(MarkdownSparseSearchService.class);
        MarkdownHybridSearchService service = new MarkdownHybridSearchService(dense, sparse);
        when(dense.search("HashMap", 8, 0.6))
                .thenThrow(new IllegalStateException("Qdrant unavailable"));
        when(sparse.search("HashMap", 8)).thenReturn(List.of(result("sparse-top", 2, 4.2)));

        MarkdownHybridSearchService.MarkdownHybridSearchResult response =
                service.searchDetailed("HashMap", 2, 0.6);

        assertEquals(MarkdownHybridSearchService.RetrievalStrategy.SPARSE_ONLY, response.strategy());
        assertTrue(response.degraded());
        assertEquals("IllegalStateException", response.degradationReason());
        assertEquals(0, response.denseCandidateCount());
        assertEquals(1, response.sparseCandidateCount());
        assertEquals(null, response.hits().get(0).denseRank());
        assertEquals(1, response.hits().get(0).sparseRank());
    }

    @Test
    void keepsSparseEvidenceGateWhenDenseSearchFails() {
        MarkdownSimilaritySearchService dense = mock(MarkdownSimilaritySearchService.class);
        MarkdownSparseSearchService sparse = mock(MarkdownSparseSearchService.class);
        MarkdownHybridSearchService service = new MarkdownHybridSearchService(dense, sparse);
        when(dense.search("unknown", 20, 0.6)).thenThrow(new IllegalStateException("timeout"));
        when(sparse.search("unknown", 20)).thenReturn(List.of());

        MarkdownHybridSearchService.MarkdownHybridSearchResult response =
                service.searchDetailed("unknown", 5, 0.6);

        assertTrue(response.degraded());
        assertTrue(response.results().isEmpty());
    }

    @Test
    void rejectsInvalidHybridParameters() {
        MarkdownHybridSearchService service = new MarkdownHybridSearchService(
                mock(MarkdownSimilaritySearchService.class), mock(MarkdownSparseSearchService.class));

        assertEquals("检索问题不能为空", assertThrows(IllegalArgumentException.class,
                () -> service.search(" ", 5, 0.0)).getMessage());
        assertEquals("topK 必须在 1 到 20 之间", assertThrows(IllegalArgumentException.class,
                () -> service.search("HashMap", 21, 0.0)).getMessage());
        assertEquals("Dense 相似度阈值必须在 0 到 1 之间", assertThrows(IllegalArgumentException.class,
                () -> service.search("HashMap", 5, 1.1)).getMessage());
    }

    private MarkdownSearchResult result(String content, int order, double score) {
        return new MarkdownSearchResult(content, "java/collections.md", "version-1", order,
                List.of("Java", "集合"), score);
    }
}
