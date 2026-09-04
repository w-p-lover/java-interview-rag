package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.document.MarkdownImportRegistry;
import com.javainterview.rag.knowledge.document.MarkdownIndexingStateStore;
import com.javainterview.rag.knowledge.rag.RagRetrievalSource;
import com.javainterview.rag.knowledge.vector.MarkdownAdjacentChunkExpander;
import com.javainterview.rag.knowledge.vector.MarkdownHybridSearchService;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import com.javainterview.rag.knowledge.vector.MarkdownSparseSearchService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class HybridRetrievalEvaluationService {

    private static final int EVALUATION_TOP_K = 5;
    private static final double DENSE_CANDIDATE_THRESHOLD = 0.0;
    private static final double ANSWER_SIMILARITY_THRESHOLD = 0.60;
    private static final String ADJACENT_CONTEXT_STRATEGY = "same-source-version-neighbor-window-1-v1";

    private final FixedRagEvaluationDataset dataset;
    private final MarkdownSimilaritySearchService denseSearchService;
    private final MarkdownSparseSearchService sparseSearchService;
    private final MarkdownHybridSearchService hybridSearchService;
    private final MarkdownImportRegistry importRegistry;
    private final MarkdownIndexingStateStore indexingStateStore;
    private final MarkdownAdjacentChunkExpander contextExpander;

    public HybridRetrievalEvaluationService(FixedRagEvaluationDataset dataset,
                                            MarkdownSimilaritySearchService denseSearchService,
                                            MarkdownSparseSearchService sparseSearchService,
                                            MarkdownHybridSearchService hybridSearchService) {
        this(dataset, denseSearchService, sparseSearchService, hybridSearchService, null);
    }

    public HybridRetrievalEvaluationService(FixedRagEvaluationDataset dataset,
                                            MarkdownSimilaritySearchService denseSearchService,
                                            MarkdownSparseSearchService sparseSearchService,
                                            MarkdownHybridSearchService hybridSearchService,
                                            MarkdownImportRegistry importRegistry) {
        this(dataset, denseSearchService, sparseSearchService, hybridSearchService,
                importRegistry, null, null);
    }

    public HybridRetrievalEvaluationService(FixedRagEvaluationDataset dataset,
                                            MarkdownSimilaritySearchService denseSearchService,
                                            MarkdownSparseSearchService sparseSearchService,
                                            MarkdownHybridSearchService hybridSearchService,
                                            MarkdownImportRegistry importRegistry,
                                            MarkdownIndexingStateStore indexingStateStore) {
        this(dataset, denseSearchService, sparseSearchService, hybridSearchService,
                importRegistry, indexingStateStore, null);
    }

    public HybridRetrievalEvaluationService(FixedRagEvaluationDataset dataset,
                                            MarkdownSimilaritySearchService denseSearchService,
                                            MarkdownSparseSearchService sparseSearchService,
                                            MarkdownHybridSearchService hybridSearchService,
                                            MarkdownImportRegistry importRegistry,
                                            MarkdownIndexingStateStore indexingStateStore,
                                            MarkdownAdjacentChunkExpander contextExpander) {
        this.dataset = Objects.requireNonNull(dataset, "固定评估题集不能为空");
        this.denseSearchService = Objects.requireNonNull(denseSearchService, "向量检索服务不能为空");
        this.sparseSearchService = Objects.requireNonNull(sparseSearchService, "BM25 检索服务不能为空");
        this.hybridSearchService = Objects.requireNonNull(hybridSearchService, "混合检索服务不能为空");
        this.importRegistry = importRegistry;
        this.indexingStateStore = indexingStateStore;
        this.contextExpander = contextExpander;
    }

    public HybridRetrievalEvaluationReport evaluate() {
        RagRetrievalEvaluationService evaluator = new RagRetrievalEvaluationService(dataset,
                new FixedRagStructureEvaluationDataset(), denseSearchService, importRegistry,
                indexingStateStore);
        RagRetrievalEvaluationReport dense = evaluator.evaluate();
        RagRetrievalEvaluationReport sparse = evaluator.evaluateWith(query ->
                sparseSearchService.search(query, EVALUATION_TOP_K),
                RagRetrievalRejectionPolicy.CANDIDATE_PRESENCE);
        RagRetrievalEvaluationReport hybrid = evaluator.evaluateWith(query ->
                hybridSearchService.search(query, EVALUATION_TOP_K, DENSE_CANDIDATE_THRESHOLD),
                RagRetrievalRejectionPolicy.COSINE_THRESHOLD);
        return new HybridRetrievalEvaluationReport(dense, sparse, hybrid,
                "Dense/Hybrid 使用余弦阈值；Sparse 先执行 technical_anchor_coverage_v1，再使用候选存在性。"
                        + "BM25 分数与余弦不在同一尺度，三策略拒答指标不能直接按分数数值比较。");
    }

    public RagAdjacentContextEvaluationReport compareAdjacentContext() {
        if (contextExpander == null) {
            throw new IllegalStateException("相邻 Chunk 扩展评估服务未配置");
        }
        List<ContextCase> contextCases = new ArrayList<>();
        Map<String, List<MarkdownSearchResult>> baselineByQuestion = new LinkedHashMap<>();
        Map<String, List<MarkdownSearchResult>> candidateByQuestion = new LinkedHashMap<>();
        Map<String, List<RagRetrievalSource>> addedContexts = new LinkedHashMap<>();
        int additionalVectorQueryCount = 0;
        int addedContextCount = 0;

        for (RagEvaluationCase evaluationCase : dataset.findAll()) {
            List<MarkdownSearchResult> baseline = hybridSearchService.search(
                    evaluationCase.question(), EVALUATION_TOP_K, ANSWER_SIMILARITY_THRESHOLD);
            MarkdownAdjacentChunkExpander.ExpansionResult expansion = contextExpander.expandDetailed(baseline);
            List<MarkdownSearchResult> candidate = expansion.results();
            List<MarkdownSearchResult> added = addedResults(baseline, candidate);
            baselineByQuestion.put(evaluationCase.question(), baseline);
            candidateByQuestion.put(evaluationCase.question(), candidate);
            addedContexts.put(evaluationCase.id(), added.stream().map(RagRetrievalSource::from).toList());
            contextCases.add(new ContextCase(evaluationCase, baseline, candidate));
            additionalVectorQueryCount += expansion.vectorQueryCount();
            addedContextCount += added.size();
        }

        RagRetrievalEvaluationService evaluator = new RagRetrievalEvaluationService(dataset,
                new FixedRagStructureEvaluationDataset(), denseSearchService, importRegistry,
                indexingStateStore);
        RagRetrievalEvaluationReport baseline = evaluator.evaluateWith(baselineByQuestion::get,
                RagRetrievalRejectionPolicy.CANDIDATE_PRESENCE);
        RagRetrievalEvaluationReport candidate = evaluator.evaluateWith(question ->
                        candidateByQuestion.get(question).stream().limit(EVALUATION_TOP_K).toList(),
                RagRetrievalRejectionPolicy.CANDIDATE_PRESENCE);
        int expandedCaseCount = (int) addedContexts.values().stream().filter(values -> !values.isEmpty()).count();
        return new RagAdjacentContextEvaluationReport(
                ADJACENT_CONTEXT_STRATEGY,
                expandedCaseCount,
                additionalVectorQueryCount,
                addedContextCount,
                addedContexts,
                contextMetrics(contextCases, false),
                contextMetrics(contextCases, true),
                baseline,
                candidate
        );
    }

    private List<MarkdownSearchResult> addedResults(List<MarkdownSearchResult> baseline,
                                                    List<MarkdownSearchResult> candidate) {
        Set<ChunkIdentity> baselineIdentities = baseline.stream()
                .map(ChunkIdentity::from)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return candidate.stream()
                .filter(result -> !baselineIdentities.contains(ChunkIdentity.from(result)))
                .toList();
    }

    private RagAdjacentContextMetrics contextMetrics(List<ContextCase> cases, boolean candidate) {
        List<ContextCase> answerableCases = cases.stream()
                .filter(contextCase -> contextCase.evaluationCase().type() != RagEvaluationCaseType.NO_ANSWER)
                .toList();
        int contextCount = 0;
        int relevantContextCount = 0;
        int expectedSourceCount = 0;
        int expectedSourceHitCount = 0;
        int expectedChunkCount = 0;
        int expectedChunkHitCount = 0;
        int expectedAnswerPointCount = 0;
        int expectedAnswerPointHitCount = 0;

        for (ContextCase contextCase : answerableCases) {
            RagEvaluationCase evaluationCase = contextCase.evaluationCase();
            List<MarkdownSearchResult> contexts = candidate
                    ? contextCase.candidate() : contextCase.baseline();
            contextCount += contexts.size();
            relevantContextCount += (int) contexts.stream()
                    .filter(context -> isRelevant(evaluationCase, context))
                    .count();
            List<String> expectedSources = evaluationCase.expectedSources().stream().distinct().toList();
            expectedSourceCount += expectedSources.size();
            expectedSourceHitCount += (int) expectedSources.stream()
                    .filter(source -> contexts.stream().anyMatch(context -> context.source().equals(source)))
                    .count();
            List<RagEvaluationChunkExpectation> expectedChunks = evaluationCase.expectedChunks().stream()
                    .distinct().toList();
            expectedChunkCount += expectedChunks.size();
            expectedChunkHitCount += (int) expectedChunks.stream()
                    .filter(expected -> contexts.stream().anyMatch(context -> sameChunk(context, expected)))
                    .count();
            expectedAnswerPointCount += evaluationCase.expectedAnswerPoints().size();
            expectedAnswerPointHitCount += (int) evaluationCase.expectedAnswerPoints().stream()
                    .filter(point -> contexts.stream().anyMatch(context -> containsNormalized(context.content(), point)))
                    .count();
        }
        return new RagAdjacentContextMetrics(
                contextCount,
                rate(contextCount, answerableCases.size()),
                relevantContextCount,
                contextCount == 0 ? 0.0 : 1.0 - rate(relevantContextCount, contextCount),
                expectedSourceCount,
                expectedSourceHitCount,
                rate(expectedSourceHitCount, expectedSourceCount),
                expectedChunkCount,
                expectedChunkHitCount,
                rate(expectedChunkHitCount, expectedChunkCount),
                expectedAnswerPointCount,
                expectedAnswerPointHitCount,
                rate(expectedAnswerPointHitCount, expectedAnswerPointCount)
        );
    }

    private boolean isRelevant(RagEvaluationCase evaluationCase, MarkdownSearchResult result) {
        if (!evaluationCase.expectedChunks().isEmpty()) {
            return evaluationCase.expectedChunks().stream().anyMatch(expected -> sameChunk(result, expected));
        }
        return evaluationCase.expectedSources().contains(result.source());
    }

    private boolean sameChunk(MarkdownSearchResult result, RagEvaluationChunkExpectation expected) {
        return result.source().equals(expected.source()) && result.chunkOrder() == expected.chunkOrder();
    }

    private boolean containsNormalized(String content, String expectedPoint) {
        String normalizedPoint = normalize(expectedPoint);
        return !normalizedPoint.isEmpty() && normalize(content).contains(normalizedPoint);
    }

    private String normalize(String value) {
        StringBuilder normalized = new StringBuilder();
        value.codePoints()
                .filter(Character::isLetterOrDigit)
                .map(Character::toLowerCase)
                .forEach(normalized::appendCodePoint);
        return normalized.toString();
    }

    private double rate(int numerator, int denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }

    private record ContextCase(RagEvaluationCase evaluationCase,
                               List<MarkdownSearchResult> baseline,
                               List<MarkdownSearchResult> candidate) {
    }

    private record ChunkIdentity(String source, String documentVersion, int chunkOrder) {

        private static ChunkIdentity from(MarkdownSearchResult result) {
            return new ChunkIdentity(result.source(), result.documentVersion(), result.chunkOrder());
        }
    }
}
