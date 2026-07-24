package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RagRetrievalEvaluationServiceTest {

    @Test
    void evaluatesExpectedSourceHitsForAllFixedCases() {
        FixedRagEvaluationDataset dataset = new FixedRagEvaluationDataset();
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        when(searchService.search("HashMap 在什么条件下树化，平均查询复杂度是多少？", 5, 0.0))
                .thenReturn(List.of(
                        new MarkdownSearchResult("HashMap 内容", "java/collections.md", "version-1", 0,
                                List.of("Java", "集合"), 0.91),
                        new MarkdownSearchResult("无关内容", "java/other.md", "version-2", 1,
                                List.of("Java"), 0.70)
                ));
        when(searchService.search("ThreadPoolExecutor 接收到新任务后如何决定创建线程、入队和拒绝？", 5, 0.0))
                .thenReturn(List.of(new MarkdownSearchResult("错误来源", "java/collections.md", "version-3", 0,
                        List.of("Java", "集合"), 0.80)));

        RagRetrievalEvaluationService service = new RagRetrievalEvaluationService(dataset, searchService);

        RagRetrievalEvaluationReport report = service.evaluate();

        assertEquals(5, report.topK());
        assertEquals(0.0, report.similarityThreshold());
        assertEquals(6, report.totalCaseCount());
        assertEquals(1, report.expectedSourceHitCount());
        assertEquals(1.0 / 6.0, report.expectedSourceHitRate());
        assertTrue(report.cases().get(0).expectedSourceMatched());
        assertEquals("java/collections.md", report.cases().get(0).actualSources().get(0).source());
        assertFalse(report.cases().get(1).expectedSourceMatched());
        assertEquals(List.of(), report.cases().get(2).actualSources());
    }
}
