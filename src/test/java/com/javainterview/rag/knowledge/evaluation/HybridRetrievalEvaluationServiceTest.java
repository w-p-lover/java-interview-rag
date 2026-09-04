package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.vector.MarkdownAdjacentChunkExpander;
import com.javainterview.rag.knowledge.vector.MarkdownHybridSearchService;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import com.javainterview.rag.knowledge.vector.MarkdownSparseSearchService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HybridRetrievalEvaluationServiceTest {

    @Test
    void comparesAdjacentContextWithoutRepeatingTheInitialHybridSearch() {
        FixedRagEvaluationDataset dataset = mock(FixedRagEvaluationDataset.class);
        MarkdownHybridSearchService hybridSearchService = mock(MarkdownHybridSearchService.class);
        MarkdownAdjacentChunkExpander contextExpander = mock(MarkdownAdjacentChunkExpander.class);
        RagEvaluationCase evaluationCase = new RagEvaluationCase(
                "adjacent", "相邻上下文问题", RagEvaluationCaseType.EXACT_TERM,
                List.of("java/map.md"), List.of(List.of("Java", "Map")), List.of(),
                List.of("首段事实", "相邻段事实"),
                List.of(new RagEvaluationChunkExpectation("java/map.md", 0),
                        new RagEvaluationChunkExpectation("java/map.md", 1)),
                List.of());
        MarkdownSearchResult seed = result(0, "首段事实");
        MarkdownSearchResult neighbor = result(1, "相邻段事实");
        when(dataset.findAll()).thenReturn(List.of(evaluationCase));
        when(hybridSearchService.search("相邻上下文问题", 5, 0.60)).thenReturn(List.of(seed));
        when(contextExpander.expandDetailed(List.of(seed))).thenReturn(
                new MarkdownAdjacentChunkExpander.ExpansionResult(List.of(seed, neighbor), 1));

        HybridRetrievalEvaluationService service = new HybridRetrievalEvaluationService(
                dataset, mock(MarkdownSimilaritySearchService.class),
                mock(MarkdownSparseSearchService.class), hybridSearchService,
                null, null, contextExpander);

        RagAdjacentContextEvaluationReport report = service.compareAdjacentContext();

        assertEquals("same-source-version-neighbor-window-1-v1", report.candidateStrategy());
        assertEquals(1, report.expandedCaseCount());
        assertEquals(1, report.additionalVectorQueryCount());
        assertEquals(1, report.addedContextCount());
        assertEquals(1, report.addedContexts().get("adjacent").size());
        assertEquals(0.5, report.baselineContext().chunkRecallRate());
        assertEquals(1.0, report.candidateContext().chunkRecallRate());
        assertEquals(0.5, report.baselineContext().answerPointEvidenceRecallRate());
        assertEquals(1.0, report.candidateContext().answerPointEvidenceRecallRate());
        assertEquals(1.0, report.baseline().advancedMetrics().precisionAt5());
        assertEquals(1.0, report.candidate().advancedMetrics().precisionAt5());
        verify(hybridSearchService).search("相邻上下文问题", 5, 0.60);
        verify(contextExpander).expandDetailed(List.of(seed));
    }

    private MarkdownSearchResult result(int chunkOrder, String content) {
        return new MarkdownSearchResult(content, "java/map.md", "version-1", chunkOrder,
                List.of("Java", "Map"), 0.9);
    }
}
