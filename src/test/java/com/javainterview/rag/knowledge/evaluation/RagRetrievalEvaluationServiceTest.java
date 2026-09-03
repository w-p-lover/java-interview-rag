package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.document.KnowledgeBlockType;
import com.javainterview.rag.knowledge.document.KnowledgeDocumentOutline;
import com.javainterview.rag.knowledge.document.KnowledgeDocumentSummary;
import com.javainterview.rag.knowledge.document.MarkdownImportRegistry;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagRetrievalEvaluationServiceTest {

    @Test
    void classifiesCorpusParsingAndCandidateRecallFailuresFromImportState() {
        FixedRagEvaluationDataset dataset = mock(FixedRagEvaluationDataset.class);
        MarkdownImportRegistry registry = mock(MarkdownImportRegistry.class);
        RagEvaluationCase corpusAbsent = evaluationCase(
                "corpus-absent", "语料缺失", RagEvaluationCaseType.EXACT_TERM,
                List.of("java/missing.md"), List.of(List.of("Java")));
        RagEvaluationCase chunkLost = evaluationCase(
                "chunk-lost", "切分丢失", RagEvaluationCaseType.EXACT_TERM,
                List.of("java/empty.md"), List.of(List.of("Java")));
        RagEvaluationCase notRecalled = evaluationCase(
                "not-recalled", "候选未召回", RagEvaluationCaseType.EXACT_TERM,
                List.of("java/indexed.md"), List.of(List.of("Java")));
        when(dataset.findAll()).thenReturn(List.of(corpusAbsent, chunkLost, notRecalled));
        when(registry.findCurrentDocuments()).thenReturn(List.of(
                document("java/empty.md", 0), document("java/indexed.md", 2)));
        RagRetrievalEvaluationService service = new RagRetrievalEvaluationService(
                dataset, mock(FixedRagStructureEvaluationDataset.class),
                mock(MarkdownSimilaritySearchService.class), registry);

        RagRetrievalEvaluationReport report = service.evaluateWith(question -> List.of());

        assertEquals(RagRetrievalFailureLayer.CORPUS_ABSENT,
                report.cases().get(0).failureLayer());
        assertEquals(RagRetrievalFailureLayer.PARSING_OR_CHUNK_LOSS,
                report.cases().get(1).failureLayer());
        assertEquals(RagRetrievalFailureLayer.CANDIDATE_NOT_RECALLED,
                report.cases().get(2).failureLayer());
        verify(registry, times(1)).findCurrentDocuments();
    }

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
        assertEquals(RagRetrievalFailureLayer.RANKING_TOO_LOW, first.failureLayer());

        RagRetrievalEvaluationCaseResult third = report.cases().get(2);
        assertNull(third.firstRelevantRank());
        assertTrue(third.rejectionExpected());
        assertTrue(third.rejectedAtAnswerThreshold());
        assertTrue(third.rejectionCorrect());
        assertEquals(RagRetrievalFailureLayer.NONE, third.failureLayer());

        RagRetrievalEvaluationCaseResult fourth = report.cases().get(3);
        assertTrue(fourth.rejectionExpected());
        assertFalse(fourth.rejectedAtAnswerThreshold());
        assertFalse(fourth.rejectionCorrect());
        assertEquals(RagRetrievalFailureLayer.UNCLASSIFIED, fourth.failureLayer());
    }

    @Test
    void appliesCandidatePresencePolicyWithoutComparingBm25ScoreToCosineThreshold() {
        FixedRagEvaluationDataset dataset = mock(FixedRagEvaluationDataset.class);
        RagEvaluationCase rejected = evaluationCase(
                "empty", "没有候选的问题", RagEvaluationCaseType.NO_ANSWER, List.of(), List.of());
        RagEvaluationCase candidatePresent = evaluationCase(
                "candidate", "只有低分候选的问题", RagEvaluationCaseType.NO_ANSWER, List.of(), List.of());
        when(dataset.findAll()).thenReturn(List.of(rejected, candidatePresent));

        RagRetrievalEvaluationReport report = new RagRetrievalEvaluationService(dataset,
                mock(MarkdownSimilaritySearchService.class)).evaluateWith(question ->
                        question.equals("没有候选的问题")
                                ? List.of()
                                : List.of(result("低分 BM25 候选", "java/other.md",
                                List.of("Java"), 0.01)),
                RagRetrievalRejectionPolicy.CANDIDATE_PRESENCE);

        assertEquals(0.0, report.answerSimilarityThreshold());
        assertEquals(1, report.correctRejectionCount());
        assertEquals(0.5, report.rejectionAccuracy());
        assertTrue(report.cases().get(0).rejectedAtAnswerThreshold());
        assertFalse(report.cases().get(1).rejectedAtAnswerThreshold());
    }

    @Test
    void calculatesChunkHardNegativePrecisionNdcgNoiseAndLatencyMetrics() {
        FixedRagEvaluationDataset dataset = mock(FixedRagEvaluationDataset.class);
        RagEvaluationChunkExpectation expectedFirst = new RagEvaluationChunkExpectation("java/map.md", 0);
        RagEvaluationChunkExpectation expectedSecond = new RagEvaluationChunkExpectation("java/map.md", 1);
        RagEvaluationChunkExpectation hardNegative = new RagEvaluationChunkExpectation("java/other.md", 0);
        RagEvaluationCase evaluationCase = new RagEvaluationCase(
                "chunk-metrics", "Chunk 评估问题", RagEvaluationCaseType.EXACT_TERM,
                List.of("java/map.md"), List.of(List.of("Java", "Map")), List.of(),
                List.of("回答要点"), List.of(expectedFirst, expectedSecond), List.of(hardNegative));
        when(dataset.findAll()).thenReturn(List.of(evaluationCase));

        RagRetrievalEvaluationReport report = new RagRetrievalEvaluationService(dataset,
                mock(MarkdownSimilaritySearchService.class)).evaluateWith(question -> List.of(
                        result("命中一", "java/map.md", List.of("Java", "Map"), 0.9),
                        result("Hard Negative", "java/other.md", List.of("Java"), 0.8),
                        result("未命中 Chunk", "java/map.md", List.of("Java", "Map"), 0.7)
                ));

        assertEquals(0.5, report.advancedMetrics().chunkRecallAt5());
        assertEquals(2.0 / 3.0, report.advancedMetrics().precisionAt5());
        assertEquals(0.0, report.advancedMetrics().hardNegativeAvoidanceRate());
        assertEquals(1.0 / 3.0, report.advancedMetrics().contextNoiseRate(), 0.000001);
        assertTrue(report.advancedMetrics().ndcgAt5() > 0.0);
        assertTrue(report.advancedMetrics().retrievalLatencyP95Ms() >= 0.0);
        assertEquals(0.5, report.cases().get(0).chunkRecallAt5());
        assertEquals(0.0, report.cases().get(0).hardNegativeAvoidanceRate());
    }

    @Test
    void comparesQueryDecompositionWithoutRepeatingOriginalSearch() {
        FixedRagEvaluationDataset dataset = mock(FixedRagEvaluationDataset.class);
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        String question = "单线程映射与高并发映射应如何选择，并分别关注哪些查询和竞争成本？";
        RagEvaluationCase evaluationCase = evaluationCase(
                "cross-map", question, RagEvaluationCaseType.CROSS_SECTION,
                List.of("java/collections.md", "java/concurrent-collections.md"),
                List.of(List.of("Java 集合", "HashMap"),
                        List.of("Java 并发集合", "ConcurrentHashMap")));
        when(dataset.findAll()).thenReturn(List.of(evaluationCase));
        when(searchService.search(question,
                5, 0.0)).thenReturn(List.of(
                result("并发映射", "java/concurrent-collections.md",
                        List.of("Java 并发集合", "ConcurrentHashMap"), 0.81)
        ));
        when(searchService.search("单线程映射", 5, 0.0)).thenReturn(List.of(
                result("普通映射", "java/collections.md", List.of("Java 集合", "HashMap"), 0.72)
        ));
        when(searchService.search("高并发映射应如何选择，并分别关注哪些查询和竞争成本", 5, 0.0)).thenReturn(List.of(
                result("并发映射", "java/concurrent-collections.md",
                        List.of("Java 并发集合", "ConcurrentHashMap"), 0.75)
        ));
        when(searchService.search("单线程映射与高并发映射应如何选择", 5, 0.0)).thenReturn(List.of(
                result("普通映射", "java/collections.md", List.of("Java 集合", "HashMap"), 0.79)
        ));

        RagRetrievalEvaluationComparisonReport comparison =
                new RagRetrievalEvaluationService(dataset, searchService).compareQueryDecomposition();

        assertEquals("deterministic-clause-rrf-v1", comparison.candidateStrategy());
        assertEquals(1, comparison.decomposedCaseCount());
        assertEquals(3, comparison.additionalQueryCount());
        assertEquals(4, comparison.retrievalQueries().get("cross-map").size());
        assertEquals(0.5, comparison.baseline().meanRecallAt5());
        assertEquals(1.0, comparison.candidate().meanRecallAt5());
        assertEquals(0.5, comparison.baseline().headingPathRecallRate());
        assertEquals(1.0, comparison.candidate().headingPathRecallRate());
        verify(searchService, times(1)).search(question, 5, 0.0);
    }

    @Test
    void calculatesPageBlockTypeAndCombinedStructureMetrics() {
        FixedRagEvaluationDataset dataset = mock(FixedRagEvaluationDataset.class);
        FixedRagStructureEvaluationDataset structureDataset = mock(FixedRagStructureEvaluationDataset.class);
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        RagEvaluationCase codeCase = structureCase(
                "code", "代码问题", List.of("1. HashMap"), 1, KnowledgeBlockType.CODE);
        RagEvaluationCase tableCase = structureCase(
                "table", "表格问题", List.of("1. HashMap"), 1, KnowledgeBlockType.TABLE);
        when(structureDataset.findAll()).thenReturn(List.of(codeCase, tableCase));
        when(searchService.search("代码问题", 5, 0.0)).thenReturn(List.of(
                result("代码", FixedRagStructureEvaluationDataset.STRUCTURED_PDF,
                        List.of("1. HashMap"), 0.91, 1, KnowledgeBlockType.CODE)
        ));
        when(searchService.search("表格问题", 5, 0.0)).thenReturn(List.of(
                result("错误页的表格", FixedRagStructureEvaluationDataset.STRUCTURED_PDF,
                        List.of("1. HashMap"), 0.88, 2, KnowledgeBlockType.TABLE)
        ));

        RagRetrievalEvaluationReport report = new RagRetrievalEvaluationService(
                dataset, structureDataset, searchService).evaluateStructure();

        assertEquals(2, report.structureCaseCount());
        assertEquals(2, report.expectedPageNumberCount());
        assertEquals(1, report.expectedPageNumberHitCount());
        assertEquals(0.5, report.pageNumberRecallRate());
        assertEquals(2, report.expectedBlockTypeCount());
        assertEquals(2, report.expectedBlockTypeHitCount());
        assertEquals(1.0, report.blockTypeRecallRate());
        assertEquals(2, report.expectedStructureCount());
        assertEquals(1, report.expectedStructureHitCount());
        assertEquals(0.5, report.structureRecallRate());
        assertEquals(0.0, report.cases().get(1).pageNumberRecall());
        assertEquals(1.0, report.cases().get(1).blockTypeRecall());
        assertEquals(0.0, report.cases().get(1).structureRecall());
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

    private MarkdownSearchResult result(String content, String source, List<String> headingPath,
                                        double similarity, int pageNumber, KnowledgeBlockType blockType) {
        return new MarkdownSearchResult(content, source, "version-1", 0, headingPath, similarity,
                pageNumber, blockType);
    }

    private RagEvaluationCase structureCase(String id, String question, List<String> headingPath,
                                            int pageNumber, KnowledgeBlockType blockType) {
        RagEvaluationStructureExpectation expectation = new RagEvaluationStructureExpectation(
                FixedRagStructureEvaluationDataset.STRUCTURED_PDF, headingPath, pageNumber, blockType);
        return new RagEvaluationCase(id, question, RagEvaluationCaseType.DOCUMENT_STRUCTURE,
                List.of(FixedRagStructureEvaluationDataset.STRUCTURED_PDF), List.of(headingPath),
                List.of(expectation), List.of("回答要点"));
    }

    private KnowledgeDocumentSummary document(String source, int chunkCount) {
        return new KnowledgeDocumentSummary(source, "version-1", 100, LocalDateTime.now(),
                new KnowledgeDocumentOutline(1, chunkCount, List.of()));
    }
}
