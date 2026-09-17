package com.javainterview.rag.interview.resume;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class FixedResumeEvidenceAuditEvaluationService {

    private final FixedResumeEvidenceAuditDataset dataset;
    private final ResumeEvidenceAuditService auditService;

    public FixedResumeEvidenceAuditEvaluationService(
            FixedResumeEvidenceAuditDataset dataset,
            ResumeEvidenceAuditService auditService) {
        this.dataset = Objects.requireNonNull(dataset, "固定简历审计评估样本不能为空");
        this.auditService = Objects.requireNonNull(auditService, "简历证据审计服务不能为空");
    }

    public Report evaluate() {
        String runId = UUID.randomUUID().toString();
        List<CaseResult> caseResults = dataset.findAll().stream()
                .map(this::evaluateCase)
                .toList();
        return report(runId, datasetVersion(), roleName(), promptVersion(), modelName(), caseResults);
    }

    List<FixedResumeEvidenceAuditDataset.Case> evaluationCases() {
        return dataset.findAll();
    }

    String datasetVersion() {
        return FixedResumeEvidenceAuditDataset.VERSION;
    }

    String roleName() {
        return auditService.roleName();
    }

    String promptVersion() {
        return auditService.promptVersion();
    }

    String modelName() {
        return auditService.modelName();
    }

    Report report(
            String runId,
            String datasetVersion,
            String role,
            String promptVersion,
            String modelName,
            List<CaseResult> caseResults) {
        Metrics metrics = calculateMetrics(caseResults);
        List<FactTypeMetric> factTypeMetrics = aggregateFactTypeMetrics(caseResults);
        return new Report(
                runId,
                datasetVersion,
                role,
                promptVersion,
                modelName,
                caseResults.size(),
                (int) caseResults.stream().filter(CaseResult::passed).count(),
                metrics,
                factTypeMetrics,
                "未配置模型单价，不估算费用；真实费用以模型服务商账单为准。",
                caseResults);
    }

    CaseResult evaluateCase(FixedResumeEvidenceAuditDataset.Case evaluationCase) {
        List<FactKey> expectedFacts = evaluationCase.expectedFacts().stream()
                .map(fact -> new FactKey(fact.type(), fact.evidence()))
                .distinct()
                .toList();
        ResumeEvidenceAuditService.EvaluationAttempt attempt =
                auditService.auditForEvaluation(evaluationCase.resumeText());
        if (!attempt.succeeded()) {
            return CaseResult.failed(
                    evaluationCase,
                    attempt.structuredOutputAvailable(),
                    attempt.candidateFactCount(),
                    attempt.legalEvidenceCount(),
                    attempt.promptTokens(),
                    attempt.completionTokens(),
                    attempt.totalTokens(),
                    attempt.elapsedMillis(),
                    attempt.failure().getClass().getSimpleName(),
                    calculateFactTypeMetrics(expectedFacts, List.of()));
        }

        ResumeEvidenceAuditResult result = attempt.result();
        List<FactKey> actualFacts = result.confirmedFacts().stream()
                .map(fact -> new FactKey(fact.type(), fact.evidence()))
                .distinct()
                .toList();
        List<FactTypeMetric> factTypeMetrics = calculateFactTypeMetrics(expectedFacts, actualFacts);
        int matchedFactCount = factTypeMetrics.stream()
                .mapToInt(FactTypeMetric::matchedExpectedFactCount)
                .sum();

        int matchedMissingSignals = countMatchedSignals(
                evaluationCase.expectedMissingSignals(), result.missingInformation());
        int matchedRiskSignals = countMatchedSignals(
                evaluationCase.expectedRiskSignals(), result.risks());
        int unmatchedConfirmedFactCount = actualFacts.size() - matchedFactCount;
        boolean passed = matchedFactCount == expectedFacts.size()
                && unmatchedConfirmedFactCount == 0
                && matchedMissingSignals == evaluationCase.expectedMissingSignals().size()
                && matchedRiskSignals == evaluationCase.expectedRiskSignals().size()
                && attempt.candidateFactCount() == attempt.legalEvidenceCount();

        return new CaseResult(
                evaluationCase.id(),
                "SUCCEEDED",
                passed,
                true,
                expectedFacts.size(),
                matchedFactCount,
                actualFacts.size(),
                unmatchedConfirmedFactCount,
                evaluationCase.expectedMissingSignals().size(),
                matchedMissingSignals,
                evaluationCase.expectedRiskSignals().size(),
                matchedRiskSignals,
                attempt.candidateFactCount(),
                attempt.legalEvidenceCount(),
                attempt.candidateFactCount() >= ResumeEvidenceAuditDraft.MAX_FACT_COUNT,
                attempt.promptTokens(),
                attempt.completionTokens(),
                attempt.totalTokens(),
                attempt.elapsedMillis(),
                null,
                factTypeMetrics);
    }

    private List<FactTypeMetric> calculateFactTypeMetrics(
            List<FactKey> expectedFacts,
            List<FactKey> actualFacts) {
        return Arrays.stream(ResumeEvidenceAuditResult.FactType.values())
                .map(type -> {
                    List<FactKey> expectedByType = expectedFacts.stream()
                            .filter(fact -> fact.type() == type)
                            .toList();
                    List<FactKey> actualByType = actualFacts.stream()
                            .filter(fact -> fact.type() == type)
                            .toList();
                    int matchedCount = countMatchedFacts(expectedByType, actualByType);
                    int unmatchedCount = actualByType.size() - matchedCount;
                    return new FactTypeMetric(
                            type,
                            expectedByType.size(),
                            matchedCount,
                            actualByType.size(),
                            unmatchedCount,
                            rate(matchedCount, expectedByType.size()),
                            rate(matchedCount, actualByType.size()));
                })
                .toList();
    }

    private List<FactTypeMetric> aggregateFactTypeMetrics(List<CaseResult> results) {
        return Arrays.stream(ResumeEvidenceAuditResult.FactType.values())
                .map(type -> {
                    List<FactTypeMetric> metrics = results.stream()
                            .flatMap(result -> result.factTypeMetrics().stream())
                            .filter(metric -> metric.type() == type)
                            .toList();
                    int expectedCount = metrics.stream()
                            .mapToInt(FactTypeMetric::expectedFactCount)
                            .sum();
                    int matchedCount = metrics.stream()
                            .mapToInt(FactTypeMetric::matchedExpectedFactCount)
                            .sum();
                    int actualCount = metrics.stream()
                            .mapToInt(FactTypeMetric::actualConfirmedFactCount)
                            .sum();
                    int unmatchedCount = metrics.stream()
                            .mapToInt(FactTypeMetric::unmatchedConfirmedFactCount)
                            .sum();
                    return new FactTypeMetric(
                            type,
                            expectedCount,
                            matchedCount,
                            actualCount,
                            unmatchedCount,
                            rate(matchedCount, expectedCount),
                            rate(matchedCount, actualCount));
                })
                .toList();
    }

    private int countMatchedFacts(List<FactKey> expectedFacts, List<FactKey> actualFacts) {
        List<FactKey> unmatchedActualFacts = new ArrayList<>(actualFacts);
        int matchedCount = 0;
        for (FactKey expectedFact : expectedFacts) {
            int matchedIndex = unmatchedActualFacts.indexOf(expectedFact);
            if (matchedIndex < 0) {
                matchedIndex = findContainedEvidence(expectedFact, unmatchedActualFacts);
            }
            if (matchedIndex >= 0) {
                unmatchedActualFacts.remove(matchedIndex);
                matchedCount++;
            }
        }
        return matchedCount;
    }

    private int findContainedEvidence(FactKey expectedFact, List<FactKey> actualFacts) {
        for (int index = 0; index < actualFacts.size(); index++) {
            FactKey actualFact = actualFacts.get(index);
            if (actualFact.type() == expectedFact.type()
                    && (actualFact.evidence().contains(expectedFact.evidence())
                    || expectedFact.evidence().contains(actualFact.evidence()))) {
                return index;
            }
        }
        return -1;
    }

    private int countMatchedSignals(
            List<FixedResumeEvidenceAuditDataset.ExpectedSignal> expectedSignals,
            List<String> actualValues) {
        return (int) expectedSignals.stream()
                .filter(signal -> signal.alternatives().stream()
                        .anyMatch(alternative -> actualValues.stream()
                                .anyMatch(value -> value.contains(alternative))))
                .count();
    }

    private Metrics calculateMetrics(List<CaseResult> results) {
        int successfulCases = (int) results.stream().filter(result -> "SUCCEEDED".equals(result.status())).count();
        int structuredOutputCases = (int) results.stream().filter(CaseResult::structuredOutputAvailable).count();
        int expectedFacts = sum(results, CaseResult::expectedFactCount);
        int matchedFacts = sum(results, CaseResult::matchedExpectedFactCount);
        int actualFacts = sum(results, CaseResult::actualConfirmedFactCount);
        int unmatchedFacts = sum(results, CaseResult::unmatchedConfirmedFactCount);
        int candidateFacts = sum(results, CaseResult::candidateFactCount);
        int legalEvidence = sum(results, CaseResult::legalEvidenceCount);
        int expectedMissingSignals = sum(results, CaseResult::expectedMissingSignalCount);
        int matchedMissingSignals = sum(results, CaseResult::matchedMissingSignalCount);
        int expectedRiskSignals = sum(results, CaseResult::expectedRiskSignalCount);
        int matchedRiskSignals = sum(results, CaseResult::matchedRiskSignalCount);
        int maxCandidateFactCount = results.stream()
                .mapToInt(CaseResult::candidateFactCount)
                .max()
                .orElse(0);
        int capacityReachedCaseCount = (int) results.stream()
                .filter(CaseResult::factCapacityReached)
                .count();

        List<Integer> measuredTokens = results.stream()
                .map(CaseResult::totalTokens)
                .filter(Objects::nonNull)
                .toList();
        List<Long> elapsedTimes = results.stream()
                .map(CaseResult::elapsedMillis)
                .sorted(Comparator.naturalOrder())
                .toList();
        Integer totalTokens = measuredTokens.isEmpty()
                ? null : measuredTokens.stream().mapToInt(Integer::intValue).sum();
        Double averageTokens = measuredTokens.isEmpty()
                ? null : round(totalTokens.doubleValue() / measuredTokens.size());

        return new Metrics(
                rate(successfulCases, results.size()),
                rate(structuredOutputCases, results.size()),
                rate(matchedFacts, actualFacts),
                rate(matchedFacts, expectedFacts),
                rate(legalEvidence, candidateFacts),
                rate(matchedMissingSignals, expectedMissingSignals),
                rate(unmatchedFacts, actualFacts),
                rate(matchedRiskSignals, expectedRiskSignals),
                1.0,
                ResumeEvidenceAuditDraft.MAX_FACT_COUNT,
                maxCandidateFactCount,
                capacityReachedCaseCount,
                measuredTokens.size(),
                totalTokens,
                averageTokens,
                null,
                round(elapsedTimes.stream().mapToLong(Long::longValue).average().orElse(0.0)),
                percentile95(elapsedTimes));
    }

    private int sum(List<CaseResult> results, java.util.function.ToIntFunction<CaseResult> mapper) {
        return results.stream().mapToInt(mapper).sum();
    }

    private double rate(int numerator, int denominator) {
        return denominator == 0 ? 1.0 : round((double) numerator / denominator);
    }

    private double round(double value) {
        return Math.round(value * 10_000.0) / 10_000.0;
    }

    private long percentile95(List<Long> sortedValues) {
        if (sortedValues.isEmpty()) {
            return 0L;
        }
        int index = Math.max(0, (int) Math.ceil(sortedValues.size() * 0.95) - 1);
        return sortedValues.get(index);
    }

    private record FactKey(ResumeEvidenceAuditResult.FactType type, String evidence) {
    }

    public record Report(
            String baselineRunId,
            String datasetVersion,
            String role,
            String promptVersion,
            String modelName,
            int totalCaseCount,
            int passedCaseCount,
            Metrics metrics,
            List<FactTypeMetric> factTypeMetrics,
            String costEstimationNote,
            List<CaseResult> cases
    ) {

        public Report {
            baselineRunId = requireText(baselineRunId, "简历审计评估运行标识不能为空");
            datasetVersion = requireText(datasetVersion, "简历审计评估数据集版本不能为空");
            role = requireText(role, "简历审计评估角色不能为空");
            promptVersion = requireText(promptVersion, "简历审计评估 Prompt 版本不能为空");
            modelName = requireText(modelName, "简历审计评估模型名称不能为空");
            Objects.requireNonNull(metrics, "简历审计评估指标不能为空");
            factTypeMetrics = List.copyOf(Objects.requireNonNull(
                    factTypeMetrics, "简历审计事实类型指标不能为空"));
            costEstimationNote = requireText(costEstimationNote, "简历审计费用说明不能为空");
            cases = List.copyOf(Objects.requireNonNull(cases, "简历审计评估结果不能为空"));
        }
    }

    public record Metrics(
            double auditSuccessRate,
            double structureParseSuccessRate,
            double factAccuracyRate,
            double expectedFactRecallRate,
            double evidenceLegalRate,
            double missingInformationRecallRate,
            double hallucinationRate,
            double riskSignalRecallRate,
            double averageModelCallCount,
            int factCapacityLimit,
            int maxCandidateFactCount,
            int capacityReachedCaseCount,
            int tokenMeasuredCaseCount,
            Integer totalTokens,
            Double averageTotalTokens,
            Double estimatedCostCny,
            double averageElapsedMillis,
            long p95ElapsedMillis
    ) {
    }

    public record CaseResult(
            String caseId,
            String status,
            boolean passed,
            boolean structuredOutputAvailable,
            int expectedFactCount,
            int matchedExpectedFactCount,
            int actualConfirmedFactCount,
            int unmatchedConfirmedFactCount,
            int expectedMissingSignalCount,
            int matchedMissingSignalCount,
            int expectedRiskSignalCount,
            int matchedRiskSignalCount,
            int candidateFactCount,
            int legalEvidenceCount,
            boolean factCapacityReached,
            Integer promptTokens,
            Integer completionTokens,
            Integer totalTokens,
            long elapsedMillis,
            String errorType,
            List<FactTypeMetric> factTypeMetrics
    ) {

        public CaseResult {
            factTypeMetrics = List.copyOf(Objects.requireNonNull(
                    factTypeMetrics, "逐样本事实类型指标不能为空"));
        }

        private static CaseResult failed(
                FixedResumeEvidenceAuditDataset.Case evaluationCase,
                boolean structuredOutputAvailable,
                int candidateFactCount,
                int legalEvidenceCount,
                Integer promptTokens,
                Integer completionTokens,
                Integer totalTokens,
                long elapsedMillis,
                String errorType,
                List<FactTypeMetric> factTypeMetrics) {
            return new CaseResult(
                    evaluationCase.id(),
                    "FAILED",
                    false,
                    structuredOutputAvailable,
                    evaluationCase.expectedFacts().size(),
                    0,
                    0,
                    0,
                    evaluationCase.expectedMissingSignals().size(),
                    0,
                    evaluationCase.expectedRiskSignals().size(),
                    0,
                    candidateFactCount,
                    legalEvidenceCount,
                    candidateFactCount >= ResumeEvidenceAuditDraft.MAX_FACT_COUNT,
                    promptTokens,
                    completionTokens,
                    totalTokens,
                    elapsedMillis,
                    requireText(errorType, "简历审计评估失败类型不能为空"),
                    factTypeMetrics);
        }
    }

    public record FactTypeMetric(
            ResumeEvidenceAuditResult.FactType type,
            int expectedFactCount,
            int matchedExpectedFactCount,
            int actualConfirmedFactCount,
            int unmatchedConfirmedFactCount,
            double recallRate,
            double accuracyRate
    ) {

        public FactTypeMetric {
            Objects.requireNonNull(type, "简历审计事实类型不能为空");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }
}
