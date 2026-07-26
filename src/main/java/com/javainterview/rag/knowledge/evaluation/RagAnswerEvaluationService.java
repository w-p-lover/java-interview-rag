package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.rag.RagAnswerResult;
import com.javainterview.rag.knowledge.rag.RagAnswerService;
import com.javainterview.rag.knowledge.rag.RagRetrievalSource;

import java.util.List;
import java.util.Objects;

public class RagAnswerEvaluationService {

    private static final String MATCHING_METHOD = "normalized-literal-containment-v1";

    private final FixedRagEvaluationDataset dataset;
    private final RagAnswerService answerService;

    public RagAnswerEvaluationService(FixedRagEvaluationDataset dataset, RagAnswerService answerService) {
        this.dataset = Objects.requireNonNull(dataset, "固定评估题集不能为空");
        this.answerService = Objects.requireNonNull(answerService, "RAG 回答服务不能为空");
    }

    public RagAnswerEvaluationReport evaluate() {
        List<RagAnswerEvaluationCaseResult> results = dataset.findAll().stream()
                .map(this::evaluateCase)
                .toList();
        int groundedCaseCount = (int) results.stream()
                .filter(RagAnswerEvaluationCaseResult::grounded)
                .count();
        int expectedSourceHitCount = (int) results.stream()
                .filter(RagAnswerEvaluationCaseResult::expectedSourceMatched)
                .count();
        int totalAnswerPointCount = results.stream()
                .mapToInt(result -> result.expectedAnswerPoints().size())
                .sum();
        int matchedAnswerPointCount = results.stream()
                .mapToInt(result -> result.matchedAnswerPoints().size())
                .sum();
        double answerPointCoverageRate = totalAnswerPointCount == 0
                ? 0.0 : (double) matchedAnswerPointCount / totalAnswerPointCount;
        return new RagAnswerEvaluationReport(MATCHING_METHOD, results.size(), groundedCaseCount,
                expectedSourceHitCount, totalAnswerPointCount, matchedAnswerPointCount,
                answerPointCoverageRate, results);
    }

    private RagAnswerEvaluationCaseResult evaluateCase(RagEvaluationCase evaluationCase) {
        RagAnswerResult answerResult = answerService.answer(evaluationCase.question());
        List<RagRetrievalSource> actualSources = answerResult.sources().stream()
                .map(RagRetrievalSource::from)
                .toList();
        boolean expectedSourceMatched = actualSources.stream()
                .map(RagRetrievalSource::source)
                .anyMatch(evaluationCase.expectedSources()::contains);
        String normalizedAnswer = normalize(answerResult.answer());
        List<String> matchedAnswerPoints = evaluationCase.expectedAnswerPoints().stream()
                .filter(point -> containsNormalized(normalizedAnswer, point))
                .toList();
        List<String> unmatchedAnswerPoints = evaluationCase.expectedAnswerPoints().stream()
                .filter(point -> !containsNormalized(normalizedAnswer, point))
                .toList();
        double coverageRate = (double) matchedAnswerPoints.size() / evaluationCase.expectedAnswerPoints().size();
        return new RagAnswerEvaluationCaseResult(evaluationCase.id(), evaluationCase.question(),
                answerResult.answer(), answerResult.grounded(), evaluationCase.expectedSources(), actualSources,
                expectedSourceMatched, evaluationCase.expectedAnswerPoints(), matchedAnswerPoints,
                unmatchedAnswerPoints, coverageRate);
    }

    private boolean containsNormalized(String normalizedAnswer, String answerPoint) {
        String normalizedPoint = normalize(answerPoint);
        return !normalizedPoint.isEmpty() && normalizedAnswer.contains(normalizedPoint);
    }

    private String normalize(String value) {
        StringBuilder normalized = new StringBuilder();
        value.codePoints()
                .filter(Character::isLetterOrDigit)
                .map(Character::toLowerCase)
                .forEach(normalized::appendCodePoint);
        return normalized.toString();
    }
}
