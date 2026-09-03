package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.rag.RagAnswerResult;
import com.javainterview.rag.knowledge.rag.RagAnswerClaimEvidenceStatus;
import com.javainterview.rag.knowledge.rag.RagAnswerEvidenceValidation;
import com.javainterview.rag.knowledge.rag.RagAnswerEvidenceValidator;
import com.javainterview.rag.knowledge.rag.RagAnswerService;
import com.javainterview.rag.knowledge.rag.RagRetrievalSource;

import java.util.List;
import java.util.Objects;

public class RagAnswerEvaluationService {

    private static final String MATCHING_METHOD = "normalized-literal-containment-v1";

    private final FixedRagEvaluationDataset dataset;
    private final RagAnswerService answerService;
    private final RagAnswerEvidenceValidator evidenceValidator = new RagAnswerEvidenceValidator();

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
        RagAnswerEvaluationMetrics metrics = aggregateMetrics(results);
        return new RagAnswerEvaluationReport(MATCHING_METHOD, results.size(), groundedCaseCount,
                expectedSourceHitCount, totalAnswerPointCount, matchedAnswerPointCount,
                answerPointCoverageRate, metrics, results);
    }

    private RagAnswerEvaluationCaseResult evaluateCase(RagEvaluationCase evaluationCase) {
        RagAnswerResult answerResult = answerService.answer(evaluationCase.question());
        List<RagRetrievalSource> actualSources = answerResult.sources().stream()
                .map(RagRetrievalSource::from)
                .toList();
        boolean expectedSourceMatched = evaluationCase.type() == RagEvaluationCaseType.NO_ANSWER
                ? actualSources.isEmpty()
                : actualSources.stream()
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
        RagAnswerEvidenceValidation evidence = answerResult.claimEvidence().isEmpty()
                ? answerResult.grounded()
                        ? evidenceValidator.validate(answerResult.answer(), answerResult.sources())
                        : new RagAnswerEvidenceValidation(false, List.of())
                : new RagAnswerEvidenceValidation(answerResult.grounded(), answerResult.claimEvidence());
        int citedExpectedAnswerPointCount = (int) matchedAnswerPoints.stream()
                .filter(point -> evidence.claims().stream().anyMatch(claim ->
                        claim.status() == RagAnswerClaimEvidenceStatus.SUPPORTED
                                && containsNormalized(normalize(claim.claim()), point)))
                .count();
        RagAnswerEvaluationMetrics metrics = metrics(evidence, citedExpectedAnswerPointCount,
                evaluationCase.expectedAnswerPoints().size());
        return new RagAnswerEvaluationCaseResult(evaluationCase.id(), evaluationCase.question(),
                answerResult.answer(), answerResult.grounded(), evaluationCase.expectedSources(), actualSources,
                expectedSourceMatched, evaluationCase.expectedAnswerPoints(), matchedAnswerPoints,
                unmatchedAnswerPoints, coverageRate, evidence.claims(), metrics);
    }

    private RagAnswerEvaluationMetrics metrics(RagAnswerEvidenceValidation evidence,
                                               int citedExpectedAnswerPointCount,
                                               int expectedAnswerPointCount) {
        int claimCount = evidence.claims().size();
        int citedClaimCount = (int) evidence.claims().stream()
                .filter(claim -> !claim.sourceNumbers().isEmpty())
                .count();
        int supportedClaimCount = countClaims(evidence, RagAnswerClaimEvidenceStatus.SUPPORTED);
        int insufficientClaimCount = countClaims(evidence, RagAnswerClaimEvidenceStatus.EVIDENCE_INSUFFICIENT);
        int conflictingClaimCount = countClaims(evidence, RagAnswerClaimEvidenceStatus.EVIDENCE_CONFLICTING);
        int unsupportedClaimCount = claimCount - supportedClaimCount;
        return new RagAnswerEvaluationMetrics(claimCount, citedClaimCount, supportedClaimCount,
                insufficientClaimCount, conflictingClaimCount, citedExpectedAnswerPointCount,
                ratio(supportedClaimCount, citedClaimCount),
                ratio(citedExpectedAnswerPointCount, expectedAnswerPointCount),
                ratio(unsupportedClaimCount, claimCount),
                ratio(conflictingClaimCount, claimCount));
    }

    private RagAnswerEvaluationMetrics aggregateMetrics(List<RagAnswerEvaluationCaseResult> results) {
        int claimCount = results.stream().mapToInt(result -> result.metrics().claimCount()).sum();
        int citedClaimCount = results.stream().mapToInt(result -> result.metrics().citedClaimCount()).sum();
        int supportedClaimCount = results.stream().mapToInt(result -> result.metrics().supportedClaimCount()).sum();
        int insufficientClaimCount = results.stream().mapToInt(result -> result.metrics().insufficientClaimCount()).sum();
        int conflictingClaimCount = results.stream().mapToInt(result -> result.metrics().conflictingClaimCount()).sum();
        int citedPointCount = results.stream().mapToInt(result -> result.metrics().citedExpectedAnswerPointCount()).sum();
        int expectedPointCount = results.stream().mapToInt(result -> result.expectedAnswerPoints().size()).sum();
        return new RagAnswerEvaluationMetrics(claimCount, citedClaimCount, supportedClaimCount,
                insufficientClaimCount, conflictingClaimCount, citedPointCount,
                ratio(supportedClaimCount, citedClaimCount), ratio(citedPointCount, expectedPointCount),
                ratio(claimCount - supportedClaimCount, claimCount),
                ratio(conflictingClaimCount, claimCount));
    }

    private int countClaims(RagAnswerEvidenceValidation evidence, RagAnswerClaimEvidenceStatus status) {
        return (int) evidence.claims().stream().filter(claim -> claim.status() == status).count();
    }

    private double ratio(int numerator, int denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
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
