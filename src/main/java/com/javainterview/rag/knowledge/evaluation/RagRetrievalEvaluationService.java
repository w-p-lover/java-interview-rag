package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.rag.RagRetrievalSource;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class RagRetrievalEvaluationService {

    private static final int EVALUATION_TOP_K = 5;
    private static final double EVALUATION_SIMILARITY_THRESHOLD = 0.0;
    private static final double ANSWER_SIMILARITY_THRESHOLD = 0.60;

    private final FixedRagEvaluationDataset dataset;
    private final MarkdownSimilaritySearchService searchService;

    public RagRetrievalEvaluationService(FixedRagEvaluationDataset dataset,
                                         MarkdownSimilaritySearchService searchService) {
        this.dataset = Objects.requireNonNull(dataset, "固定评估题集不能为空");
        this.searchService = Objects.requireNonNull(searchService, "相似度检索服务不能为空");
    }

    public RagRetrievalEvaluationReport evaluate() {
        List<RagRetrievalEvaluationCaseResult> results = dataset.findAll().stream()
                .map(this::evaluateCase)
                .toList();
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
                correctRejectionCount,
                rate(correctRejectionCount, noAnswerResults.size()),
                results
        );
    }

    private RagRetrievalEvaluationCaseResult evaluateCase(RagEvaluationCase evaluationCase) {
        List<RagRetrievalSource> actualSources = searchService.search(evaluationCase.question(),
                        EVALUATION_TOP_K, EVALUATION_SIMILARITY_THRESHOLD)
                .stream()
                .map(RagRetrievalSource::from)
                .toList();
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
                actualSources,
                expectedSourceHitCount > 0,
                firstRelevantRank,
                firstRelevantRank != null && firstRelevantRank == 1,
                rate(expectedSourceHitCount, evaluationCase.expectedSources().stream().distinct().count()),
                firstRelevantRank == null ? 0.0 : 1.0 / firstRelevantRank,
                headingPathHitCount,
                rate(headingPathHitCount, evaluationCase.expectedHeadingPaths().stream().distinct().count()),
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
