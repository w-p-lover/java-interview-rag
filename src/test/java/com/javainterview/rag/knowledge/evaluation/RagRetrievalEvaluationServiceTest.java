package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RagRetrievalEvaluationServiceTest {

    @Test
    void calculatesRankingHeadingPathAndRejectionMetrics() {
        FixedRagEvaluationDataset dataset = mock(FixedRagEvaluationDataset.class);
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        RagEvaluationCase crossSection = evaluationCase(
                "cross", "跨章节问题", RagEvaluationCaseType.CROSS_SECTION,
                List.of("java/collections.md", "java/concurrent-collections.md"),
                List.of(List.of("Java 集合", "HashMap"), List.of("Java 并发集合", "ConcurrentHashMap")));
        RagEvaluationCase hitAtOne = evaluationCase(
                "hit-one", "首位命中问题", RagEvaluationCaseType.EXACT_TERM,
                List.of("mysql/transactions.md"), List.of(List.of("MySQL 事务", "MVCC")));
        RagEvaluationCase rejected = evaluationCase(
                "rejected", "应拒答问题", RagEvaluationCaseType.NO_ANSWER, List.of(), List.of());
        RagEvaluationCase notRejected = evaluationCase(
                "not-rejected", "误命中问题", RagEvaluationCaseType.NO_ANSWER, List.of(), List.of());
        when(dataset.findAll()).thenReturn(List.of(crossSection, hitAtOne, rejected, notRejected));

        when(searchService.search("跨章节问题", 5, 0.0)).thenReturn(List.of(
                result("无关", "java/other.md", List.of("Java"), 0.92),
                result("相关", "java/collections.md", List.of("Java 集合", "HashMap"), 0.88),
                result("重复来源", "java/collections.md", List.of("Java 集合", "扩容"), 0.80)
        ));
        when(searchService.search("首位命中问题", 5, 0.0)).thenReturn(List.of(
                result("相关", "mysql/transactions.md", List.of("MySQL 事务", "MVCC"), 0.91)
        ));
        when(searchService.search("应拒答问题", 5, 0.0)).thenReturn(List.of(
                result("低相关", "java/other.md", List.of("Java"), 0.59)
        ));
        when(searchService.search("误命中问题", 5, 0.0)).thenReturn(List.of(
                result("假阳性", "rag/rag-basics.md", List.of("RAG"), 0.60)
        ));

        RagRetrievalEvaluationReport report = new RagRetrievalEvaluationService(dataset, searchService).evaluate();

        assertEquals(4, report.totalCaseCount());
        assertEquals(2, report.answerableCaseCount());
        assertEquals(2, report.noAnswerCaseCount());
        assertEquals(2, report.expectedSourceHitCount());
        assertEquals(1.0, report.expectedSourceHitRate());
        assertEquals(1, report.hitAt1Count());
        assertEquals(0.5, report.hitAt1Rate());
        assertEquals(0.75, report.meanRecallAt5());
        assertEquals(0.75, report.meanReciprocalRank());
        assertEquals(3, report.expectedHeadingPathCount());
        assertEquals(2, report.expectedHeadingPathHitCount());
        assertEquals(2.0 / 3.0, report.headingPathRecallRate());
        assertEquals(1, report.correctRejectionCount());
        assertEquals(0.5, report.rejectionAccuracy());

        RagRetrievalEvaluationCaseResult first = report.cases().get(0);
        assertEquals(2, first.firstRelevantRank());
        assertFalse(first.hitAt1());
        assertEquals(0.5, first.recallAt5());
        assertEquals(0.5, first.reciprocalRank());
        assertEquals(1, first.expectedHeadingPathHitCount());
        assertEquals(0.5, first.headingPathRecall());
        assertFalse(first.rejectionExpected());
        assertFalse(first.rejectedAtAnswerThreshold());

        RagRetrievalEvaluationCaseResult third = report.cases().get(2);
        assertNull(third.firstRelevantRank());
        assertTrue(third.rejectionExpected());
        assertTrue(third.rejectedAtAnswerThreshold());
        assertTrue(third.rejectionCorrect());

        RagRetrievalEvaluationCaseResult fourth = report.cases().get(3);
        assertTrue(fourth.rejectionExpected());
        assertFalse(fourth.rejectedAtAnswerThreshold());
        assertFalse(fourth.rejectionCorrect());
    }

    private RagEvaluationCase evaluationCase(String id, String question, RagEvaluationCaseType type,
                                             List<String> expectedSources,
                                             List<List<String>> expectedHeadingPaths) {
        return new RagEvaluationCase(id, question, type, expectedSources, expectedHeadingPaths,
                List.of("回答要点"));
    }

    private MarkdownSearchResult result(String content, String source, List<String> headingPath,
                                        double similarity) {
        return new MarkdownSearchResult(content, source, "version-1", 0, headingPath, similarity);
    }
}
