package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.rag.RagRetrievalSource;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class RagRetrievalEvaluationService {

    private static final int EVALUATION_TOP_K = 5;
    private static final double EVALUATION_SIMILARITY_THRESHOLD = 0.0;
    private static final double ANSWER_SIMILARITY_THRESHOLD = 0.60;
    private static final String QUERY_DECOMPOSITION_STRATEGY = "deterministic-clause-rrf-v1";

    private final FixedRagEvaluationDataset dataset;
    private final FixedRagStructureEvaluationDataset structureDataset;
    private final MarkdownSimilaritySearchService searchService;
    private final RagEvaluationQueryPlanner queryPlanner;
    private final RagEvaluationResultFusion resultFusion;

    public RagRetrievalEvaluationService(FixedRagEvaluationDataset dataset,
                                         MarkdownSimilaritySearchService searchService) {
        this(dataset, new FixedRagStructureEvaluationDataset(), searchService,
                new RagEvaluationQueryPlanner(), new RagEvaluationResultFusion());
    }

    public RagRetrievalEvaluationService(FixedRagEvaluationDataset dataset,
                                         FixedRagStructureEvaluationDataset structureDataset,
                                         MarkdownSimilaritySearchService searchService) {
        this(dataset, structureDataset, searchService,
                new RagEvaluationQueryPlanner(), new RagEvaluationResultFusion());
    }

    RagRetrievalEvaluationService(FixedRagEvaluationDataset dataset,
                                  FixedRagStructureEvaluationDataset structureDataset,
                                  MarkdownSimilaritySearchService searchService,
                                  RagEvaluationQueryPlanner queryPlanner,
                                  RagEvaluationResultFusion resultFusion) {
        this.dataset = Objects.requireNonNull(dataset, "固定评估题集不能为空");
        this.structureDataset = Objects.requireNonNull(structureDataset, "固定结构评估题集不能为空");
        this.searchService = Objects.requireNonNull(searchService, "相似度检索服务不能为空");
        this.queryPlanner = Objects.requireNonNull(queryPlanner, "评估查询规划器不能为空");
        this.resultFusion = Objects.requireNonNull(resultFusion, "评估结果融合器不能为空");
    }

    public RagRetrievalEvaluationReport evaluate() {
        return evaluateWith(query -> searchService.search(query,
                EVALUATION_TOP_K, EVALUATION_SIMILARITY_THRESHOLD));
    }

    public RagRetrievalEvaluationReport evaluateWith(Function<String, List<MarkdownSearchResult>> retriever) {
        return evaluateWith(retriever, ANSWER_SIMILARITY_THRESHOLD);
    }

    public RagRetrievalEvaluationReport evaluateWith(Function<String, List<MarkdownSearchResult>> retriever,
                                                      double answerSimilarityThreshold) {
        return evaluateWith(retriever, RagRetrievalRejectionPolicy.COSINE_THRESHOLD,
                answerSimilarityThreshold);
    }

    public RagRetrievalEvaluationReport evaluateWith(Function<String, List<MarkdownSearchResult>> retriever,
                                                      RagRetrievalRejectionPolicy rejectionPolicy) {
        Objects.requireNonNull(rejectionPolicy, "拒答策略不能为空");
        return evaluateWith(retriever, rejectionPolicy, rejectionPolicy.reportThreshold());
    }

    private RagRetrievalEvaluationReport evaluateWith(
            Function<String, List<MarkdownSearchResult>> retriever,
            RagRetrievalRejectionPolicy rejectionPolicy,
            double answerSimilarityThreshold) {
        Objects.requireNonNull(retriever, "检索器不能为空");
        if (!Double.isFinite(answerSimilarityThreshold) || answerSimilarityThreshold < 0.0) {
            throw new IllegalArgumentException("回答相似度阈值必须是非负有限数");
        }
        List<RagRetrievalEvaluationCaseResult> results = new ArrayList<>();
        List<Double> retrievalLatencies = new ArrayList<>();
        for (RagEvaluationCase evaluationCase : dataset.findAll()) {
            long startedAt = System.nanoTime();
            List<RagRetrievalSource> sources = retriever.apply(evaluationCase.question()).stream()
                    .map(RagRetrievalSource::from)
                    .toList();
            double latencyMs = (System.nanoTime() - startedAt) / 1_000_000.0;
            retrievalLatencies.add(latencyMs);
            results.add(evaluateCase(evaluationCase, sources, rejectionPolicy,
                    answerSimilarityThreshold, latencyMs));
        }
        return buildReport(results, answerSimilarityThreshold, retrievalLatencies);
    }

    public RagRetrievalEvaluationReport evaluateStructure() {
        List<RagRetrievalEvaluationCaseResult> results = structureDataset.findAll().stream()
                .map(evaluationCase -> evaluateCase(evaluationCase,
                        searchService.search(evaluationCase.question(),
                                        EVALUATION_TOP_K, EVALUATION_SIMILARITY_THRESHOLD).stream()
                                .map(RagRetrievalSource::from)
                                .toList()))
                .toList();
        return buildReport(results, ANSWER_SIMILARITY_THRESHOLD);
    }

    public RagRetrievalEvaluationComparisonReport compareQueryDecomposition() {
        List<RagEvaluationCase> evaluationCases = dataset.findAll();
        Map<String, List<String>> retrievalQueries = new LinkedHashMap<>();
        List<RagRetrievalEvaluationCaseResult> baselineResults = new ArrayList<>();
        List<RagRetrievalEvaluationCaseResult> candidateResults = new ArrayList<>();
        int decomposedCaseCount = 0;
        int additionalQueryCount = 0;

        for (RagEvaluationCase evaluationCase : evaluationCases) {
            List<String> queries = queryPlanner.plan(evaluationCase.question());
            retrievalQueries.put(evaluationCase.id(), queries);
            List<MarkdownSearchResult> baselineSearchResults = searchService.search(
                    queries.get(0), EVALUATION_TOP_K, EVALUATION_SIMILARITY_THRESHOLD);
            baselineResults.add(evaluateCase(evaluationCase, baselineSearchResults.stream()
                    .map(RagRetrievalSource::from)
                    .toList()));

            List<List<MarkdownSearchResult>> rankedResults = new ArrayList<>();
            rankedResults.add(baselineSearchResults);
            for (String additionalQuery : queries.subList(1, queries.size())) {
                rankedResults.add(searchService.search(additionalQuery,
                        EVALUATION_TOP_K, EVALUATION_SIMILARITY_THRESHOLD));
            }
            if (queries.size() > 1) {
                decomposedCaseCount++;
                additionalQueryCount += queries.size() - 1;
            }
            candidateResults.add(evaluateCase(evaluationCase,
                    resultFusion.fuse(rankedResults, EVALUATION_TOP_K).stream()
                            .map(RagRetrievalSource::from)
                            .toList()));
        }

        return new RagRetrievalEvaluationComparisonReport(
                QUERY_DECOMPOSITION_STRATEGY,
                decomposedCaseCount,
                additionalQueryCount,
                retrievalQueries,
                buildReport(baselineResults, ANSWER_SIMILARITY_THRESHOLD),
                buildReport(candidateResults, ANSWER_SIMILARITY_THRESHOLD)
        );
    }

    private RagRetrievalEvaluationReport buildReport(List<RagRetrievalEvaluationCaseResult> results,
                                                      double answerSimilarityThreshold) {
        return buildReport(results, answerSimilarityThreshold, List.of());
    }

    private RagRetrievalEvaluationReport buildReport(List<RagRetrievalEvaluationCaseResult> results,
                                                      double answerSimilarityThreshold,
                                                      List<Double> retrievalLatencies) {
        List<RagRetrievalEvaluationCaseResult> answerableResults = results.stream()
                .filter(result -> !result.rejectionExpected())
                .toList();
        List<RagRetrievalEvaluationCaseResult> noAnswerResults = results.stream()
                .filter(RagRetrievalEvaluationCaseResult::rejectionExpected)
                .toList();
        int expectedSourceHitCount = (int) answerableResults.stream()
                .filter(RagRetrievalEvaluationCaseResult::expectedSourceMatched)
                .count();
        int hitAt1Count = (int) answerableResults.stream()
                .filter(RagRetrievalEvaluationCaseResult::hitAt1)
                .count();
        int expectedHeadingPathCount = answerableResults.stream()
                .mapToInt(result -> result.expectedHeadingPaths().size())
                .sum();
        int expectedHeadingPathHitCount = answerableResults.stream()
                .mapToInt(RagRetrievalEvaluationCaseResult::expectedHeadingPathHitCount)
                .sum();
        List<RagRetrievalEvaluationCaseResult> structureResults = answerableResults.stream()
                .filter(result -> !result.expectedStructures().isEmpty())
                .toList();
        int expectedStructureCount = structureResults.stream()
                .mapToInt(result -> result.expectedStructures().size())
                .sum();
        int expectedPageNumberHitCount = structureResults.stream()
                .mapToInt(RagRetrievalEvaluationCaseResult::expectedPageNumberHitCount)
                .sum();
        int expectedBlockTypeHitCount = structureResults.stream()
                .mapToInt(RagRetrievalEvaluationCaseResult::expectedBlockTypeHitCount)
                .sum();
        int expectedStructureHitCount = structureResults.stream()
                .mapToInt(RagRetrievalEvaluationCaseResult::expectedStructureHitCount)
                .sum();
        int correctRejectionCount = (int) noAnswerResults.stream()
                .filter(RagRetrievalEvaluationCaseResult::rejectionCorrect)
                .count();
        return new RagRetrievalEvaluationReport(
                EVALUATION_TOP_K,
                EVALUATION_SIMILARITY_THRESHOLD,
                answerSimilarityThreshold,
                results.size(),
                answerableResults.size(),
                noAnswerResults.size(),
                expectedSourceHitCount,
                rate(expectedSourceHitCount, answerableResults.size()),
                hitAt1Count,
                rate(hitAt1Count, answerableResults.size()),
                average(answerableResults.stream().mapToDouble(RagRetrievalEvaluationCaseResult::recallAt5).sum(),
                        answerableResults.size()),
                average(answerableResults.stream().mapToDouble(RagRetrievalEvaluationCaseResult::reciprocalRank).sum(),
                        answerableResults.size()),
                expectedHeadingPathCount,
                expectedHeadingPathHitCount,
                rate(expectedHeadingPathHitCount, expectedHeadingPathCount),
                structureResults.size(),
                expectedStructureCount,
                expectedPageNumberHitCount,
                rate(expectedPageNumberHitCount, expectedStructureCount),
                expectedStructureCount,
                expectedBlockTypeHitCount,
                rate(expectedBlockTypeHitCount, expectedStructureCount),
                expectedStructureCount,
                expectedStructureHitCount,
                rate(expectedStructureHitCount, expectedStructureCount),
                correctRejectionCount,
                rate(correctRejectionCount, noAnswerResults.size()),
                results,
                buildAdvancedMetrics(answerableResults, results, retrievalLatencies)
        );
    }

    private RagRetrievalEvaluationCaseResult evaluateCase(RagEvaluationCase evaluationCase,
                                                          List<RagRetrievalSource> actualSources) {
        return evaluateCase(evaluationCase, actualSources, ANSWER_SIMILARITY_THRESHOLD);
    }

    private RagRetrievalEvaluationCaseResult evaluateCase(RagEvaluationCase evaluationCase,
                                                          List<RagRetrievalSource> actualSources,
                                                          double answerSimilarityThreshold) {
        return evaluateCase(evaluationCase, actualSources,
                RagRetrievalRejectionPolicy.COSINE_THRESHOLD, answerSimilarityThreshold);
    }

    private RagRetrievalEvaluationCaseResult evaluateCase(RagEvaluationCase evaluationCase,
                                                          List<RagRetrievalSource> actualSources,
                                                          RagRetrievalRejectionPolicy rejectionPolicy,
                                                          double answerSimilarityThreshold) {
        return evaluateCase(evaluationCase, actualSources, rejectionPolicy, answerSimilarityThreshold, 0.0);
    }

    private RagRetrievalEvaluationCaseResult evaluateCase(RagEvaluationCase evaluationCase,
                                                          List<RagRetrievalSource> actualSources,
                                                          RagRetrievalRejectionPolicy rejectionPolicy,
                                                          double answerSimilarityThreshold,
                                                          double retrievalLatencyMs) {
        Set<String> actualLogicalSources = actualSources.stream()
                .map(RagRetrievalSource::source)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        int expectedSourceHitCount = (int) evaluationCase.expectedSources().stream()
                .distinct()
                .filter(actualLogicalSources::contains)
                .count();
        Integer firstRelevantRank = findFirstRelevantRank(evaluationCase.expectedSources(), actualSources);
        int headingPathHitCount = (int) evaluationCase.expectedHeadingPaths().stream()
                .distinct()
                .filter(expectedPath -> actualSources.stream()
                        .anyMatch(actual -> actual.headingPath().equals(expectedPath)))
                .count();
        int pageNumberHitCount = (int) evaluationCase.expectedStructures().stream()
                .distinct()
                .filter(expected -> actualSources.stream().anyMatch(actual ->
                        actual.source().equals(expected.source())
                                && Objects.equals(actual.pageNumber(), expected.pageNumber())))
                .count();
        int blockTypeHitCount = (int) evaluationCase.expectedStructures().stream()
                .distinct()
                .filter(expected -> actualSources.stream().anyMatch(actual ->
                        actual.source().equals(expected.source())
                                && actual.blockType() == expected.blockType()))
                .count();
        int structureHitCount = (int) evaluationCase.expectedStructures().stream()
                .distinct()
                .filter(expected -> actualSources.stream().anyMatch(actual ->
                        actual.source().equals(expected.source())
                                && actual.headingPath().equals(expected.headingPath())
                                && Objects.equals(actual.pageNumber(), expected.pageNumber())
                                && actual.blockType() == expected.blockType()))
                .count();
        long expectedStructureCount = evaluationCase.expectedStructures().stream().distinct().count();
        boolean rejectedByPolicy = rejectionPolicy.rejected(actualSources, answerSimilarityThreshold);
        List<RagEvaluationChunkExpectation> expectedChunks = evaluationCase.expectedChunks().stream()
                .distinct().toList();
        List<RagEvaluationChunkExpectation> hardNegativeChunks = evaluationCase.hardNegativeChunks().stream()
                .distinct().toList();
        int expectedChunkHitCount = (int) expectedChunks.stream()
                .filter(expected -> containsChunk(actualSources, expected))
                .count();
        int hardNegativeHitCount = (int) hardNegativeChunks.stream()
                .filter(expected -> containsChunk(actualSources, expected))
                .count();
        int relevantCount = (int) actualSources.stream()
                .limit(5)
                .filter(source -> isRelevant(evaluationCase, source))
                .count();
        return buildCaseResult(evaluationCase, actualSources, expectedSourceHitCount,
                firstRelevantRank, headingPathHitCount, pageNumberHitCount,
                blockTypeHitCount, structureHitCount, expectedStructureCount,
                rejectedByPolicy, expectedChunkHitCount, hardNegativeHitCount,
                relevantCount, retrievalLatencyMs);
    }

    private RagRetrievalEvaluationCaseResult buildCaseResult(RagEvaluationCase evaluationCase,
                                                              List<RagRetrievalSource> actualSources,
                                                              int expectedSourceHitCount,
                                                              Integer firstRelevantRank,
                                                              int headingPathHitCount,
                                                              int pageNumberHitCount,
                                                              int blockTypeHitCount,
                                                              int structureHitCount,
                                                              long expectedStructureCount,
                                                              boolean rejectedByPolicy,
                                                              int expectedChunkHitCount,
                                                              int hardNegativeHitCount,
                                                              int relevantCount,
                                                              double retrievalLatencyMs) {
        boolean rejectionExpected = evaluationCase.type() == RagEvaluationCaseType.NO_ANSWER;
        List<RagEvaluationChunkExpectation> expectedChunks = evaluationCase.expectedChunks().stream()
                .distinct().toList();
        List<RagEvaluationChunkExpectation> hardNegativeChunks = evaluationCase.hardNegativeChunks().stream()
                .distinct().toList();
        int returnedCount = Math.min(EVALUATION_TOP_K, actualSources.size());
        double precisionAt5 = rate(relevantCount, returnedCount);
        double ndcgAt5 = ndcgAt5(evaluationCase, actualSources);
        double chunkRecallAt5 = rate(expectedChunkHitCount, expectedChunks.size());
        double hardNegativeAvoidanceRate = hardNegativeChunks.isEmpty()
                ? 0.0 : 1.0 - rate(hardNegativeHitCount, hardNegativeChunks.size());
        double contextNoiseRate = returnedCount == 0 ? 0.0 : 1.0 - precisionAt5;
        return new RagRetrievalEvaluationCaseResult(
                evaluationCase.id(),
                evaluationCase.question(),
                evaluationCase.type(),
                evaluationCase.expectedSources(),
                evaluationCase.expectedHeadingPaths(),
                evaluationCase.expectedStructures(),
                actualSources,
                expectedSourceHitCount > 0,
                firstRelevantRank,
                firstRelevantRank != null && firstRelevantRank == 1,
                rate(expectedSourceHitCount, evaluationCase.expectedSources().stream().distinct().count()),
                firstRelevantRank == null ? 0.0 : 1.0 / firstRelevantRank,
                headingPathHitCount,
                rate(headingPathHitCount, evaluationCase.expectedHeadingPaths().stream().distinct().count()),
                pageNumberHitCount,
                rate(pageNumberHitCount, expectedStructureCount),
                blockTypeHitCount,
                rate(blockTypeHitCount, expectedStructureCount),
                structureHitCount,
                rate(structureHitCount, expectedStructureCount),
                rejectionExpected,
                rejectedByPolicy,
                rejectionExpected && rejectedByPolicy,
                expectedChunks,
                hardNegativeChunks,
                chunkRecallAt5,
                precisionAt5,
                ndcgAt5,
                contextNoiseRate,
                retrievalLatencyMs,
                hardNegativeAvoidanceRate
        );
    }

    private RagRetrievalEvaluationMetrics buildAdvancedMetrics(
            List<RagRetrievalEvaluationCaseResult> answerableResults,
            List<RagRetrievalEvaluationCaseResult> results,
            List<Double> retrievalLatencies) {
        List<RagRetrievalEvaluationCaseResult> chunkResults = answerableResults.stream()
                .filter(result -> !result.expectedChunks().isEmpty())
                .toList();
        List<RagRetrievalEvaluationCaseResult> hardNegativeResults = answerableResults.stream()
                .filter(result -> !result.hardNegativeChunks().isEmpty())
                .toList();
        return new RagRetrievalEvaluationMetrics(
                average(answerableResults.stream()
                        .mapToDouble(RagRetrievalEvaluationCaseResult::precisionAt5).sum(),
                        answerableResults.size()),
                average(answerableResults.stream()
                        .mapToDouble(RagRetrievalEvaluationCaseResult::ndcgAt5).sum(),
                        answerableResults.size()),
                average(chunkResults.stream()
                        .mapToDouble(RagRetrievalEvaluationCaseResult::chunkRecallAt5).sum(),
                        chunkResults.size()),
                average(hardNegativeResults.stream()
                        .mapToDouble(RagRetrievalEvaluationCaseResult::hardNegativeAvoidanceRate).sum(),
                        hardNegativeResults.size()),
                average(answerableResults.stream()
                        .mapToDouble(RagRetrievalEvaluationCaseResult::contextNoiseRate).sum(),
                        answerableResults.size()),
                percentile95(retrievalLatencies.isEmpty() ? results.stream()
                        .map(RagRetrievalEvaluationCaseResult::retrievalLatencyMs).toList() : retrievalLatencies)
        );
    }

    private boolean containsChunk(List<RagRetrievalSource> actualSources,
                                  RagEvaluationChunkExpectation expected) {
        return actualSources.stream().limit(EVALUATION_TOP_K)
                .anyMatch(actual -> actual.source().equals(expected.source())
                        && actual.chunkOrder() == expected.chunkOrder());
    }

    private boolean isRelevant(RagEvaluationCase evaluationCase, RagRetrievalSource source) {
        if (!evaluationCase.expectedChunks().isEmpty()) {
            return evaluationCase.expectedChunks().stream().anyMatch(expected ->
                    expected.source().equals(source.source()) && expected.chunkOrder() == source.chunkOrder());
        }
        return evaluationCase.expectedSources().contains(source.source());
    }

    private double ndcgAt5(RagEvaluationCase evaluationCase, List<RagRetrievalSource> actualSources) {
        int retrievedCount = Math.min(EVALUATION_TOP_K, actualSources.size());
        if (retrievedCount == 0) {
            return 0.0;
        }
        double dcg = 0.0;
        for (int index = 0; index < retrievedCount; index++) {
            if (isRelevant(evaluationCase, actualSources.get(index))) {
                dcg += 1.0 / (Math.log(index + 2) / Math.log(2));
            }
        }
        int idealRelevantCount = Math.min(retrievedCount, evaluationCase.expectedChunks().isEmpty()
                ? evaluationCase.expectedSources().size() : evaluationCase.expectedChunks().size());
        double idcg = 0.0;
        for (int index = 0; index < idealRelevantCount; index++) {
            idcg += 1.0 / (Math.log(index + 2) / Math.log(2));
        }
        return idcg == 0.0 ? 0.0 : dcg / idcg;
    }

    private double percentile95(List<Double> values) {
        if (values.isEmpty()) {
            return 0.0;
        }
        List<Double> sorted = values.stream().sorted().toList();
        int index = Math.min(sorted.size() - 1, Math.max(0, (int) Math.ceil(sorted.size() * 0.95) - 1));
        return sorted.get(index);
    }

    private Integer findFirstRelevantRank(List<String> expectedSources, List<RagRetrievalSource> actualSources) {
        for (int index = 0; index < actualSources.size(); index++) {
            if (expectedSources.contains(actualSources.get(index).source())) {
                return index + 1;
            }
        }
        return null;
    }

    private double rate(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }

    private double average(double total, int count) {
        return count == 0 ? 0.0 : total / count;
    }
}
