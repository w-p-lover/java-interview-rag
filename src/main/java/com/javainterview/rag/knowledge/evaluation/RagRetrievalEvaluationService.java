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
        List<RagRetrievalEvaluationCaseResult> results = dataset.findAll().stream()
                .map(evaluationCase -> evaluateCase(evaluationCase,
                        searchService.search(evaluationCase.question(),
                                EVALUATION_TOP_K, EVALUATION_SIMILARITY_THRESHOLD).stream()
                                .map(RagRetrievalSource::from)
                                .toList()))
                .toList();
        return buildReport(results);
    }

    public RagRetrievalEvaluationReport evaluateStructure() {
        List<RagRetrievalEvaluationCaseResult> results = structureDataset.findAll().stream()
                .map(evaluationCase -> evaluateCase(evaluationCase,
                        searchService.search(evaluationCase.question(),
                                        EVALUATION_TOP_K, EVALUATION_SIMILARITY_THRESHOLD).stream()
                                .map(RagRetrievalSource::from)
                                .toList()))
                .toList();
        return buildReport(results);
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
                buildReport(baselineResults),
                buildReport(candidateResults)
        );
    }

    private RagRetrievalEvaluationReport buildReport(List<RagRetrievalEvaluationCaseResult> results) {
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
                ANSWER_SIMILARITY_THRESHOLD,
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
                results
        );
    }

    private RagRetrievalEvaluationCaseResult evaluateCase(RagEvaluationCase evaluationCase,
                                                          List<RagRetrievalSource> actualSources) {
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
        boolean rejectionExpected = evaluationCase.type() == RagEvaluationCaseType.NO_ANSWER;
        boolean rejectedAtAnswerThreshold = actualSources.stream()
                .map(RagRetrievalSource::similarity)
                .filter(Objects::nonNull)
                .noneMatch(similarity -> similarity >= ANSWER_SIMILARITY_THRESHOLD);
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
                rejectedAtAnswerThreshold,
                rejectionExpected && rejectedAtAnswerThreshold
        );
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
