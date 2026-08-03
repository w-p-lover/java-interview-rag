package com.javainterview.rag.interview.evaluation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InterviewAnswerEvaluationLogQueryService {

    private static final int MAX_LIMIT = 100;

    private final InterviewAnswerEvaluationLogStore logStore;

    public InterviewAnswerEvaluationLogQueryService(InterviewAnswerEvaluationLogStore logStore) {
        this.logStore = Objects.requireNonNull(logStore, "面试回答评价日志存储不能为空");
    }

    public Report findRecent(InterviewAnswerEvaluationLog.Mode mode,
                             String baselineRunId,
                             String baselineCaseId,
                             int limit) {
        Objects.requireNonNull(mode, "评价模式不能为空");
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("日志查询数量必须在 1 到 100 之间");
        }
        baselineRunId = optionalFilter(baselineRunId, 36, "评分基线运行标识不能超过 36 个字符");
        baselineCaseId = optionalFilter(baselineCaseId, 100, "评分基线样本标识不能超过 100 个字符");
        if (mode != InterviewAnswerEvaluationLog.Mode.BASELINE
                && (baselineRunId != null || baselineCaseId != null)) {
            throw new IllegalArgumentException("基线运行或样本筛选只支持 BASELINE 模式");
        }
        List<InterviewAnswerEvaluationLog> logs =
                logStore.findRecent(mode, baselineRunId, baselineCaseId, limit);
        int identifiedBaselineCount = mode == InterviewAnswerEvaluationLog.Mode.BASELINE
                ? (int) logs.stream().filter(log -> log.baselineRunId() != null).count()
                : 0;
        int unidentifiedBaselineCount = mode == InterviewAnswerEvaluationLog.Mode.BASELINE
                ? logs.size() - identifiedBaselineCount
                : 0;
        return new Report(
                mode,
                baselineRunId,
                baselineCaseId,
                logs.size(),
                identifiedBaselineCount,
                unidentifiedBaselineCount,
                summarize(logs),
                summarizeBaselineCases(logs),
                logs);
    }

    private String optionalFilter(String value, int maximumLength, String message) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private List<VersionSummary> summarize(List<InterviewAnswerEvaluationLog> logs) {
        Map<VersionKey, List<InterviewAnswerEvaluationLog>> groups = new LinkedHashMap<>();
        for (InterviewAnswerEvaluationLog log : logs) {
            VersionKey key = new VersionKey(log.promptVersion(), log.rubricVersion(), log.modelName());
            groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(log);
        }
        return groups.entrySet().stream()
                .map(entry -> summarize(entry.getKey(), entry.getValue()))
                .toList();
    }

    private VersionSummary summarize(VersionKey key, List<InterviewAnswerEvaluationLog> logs) {
        List<InterviewAnswerEvaluationLog> succeeded = logs.stream()
                .filter(log -> log.status() == InterviewAnswerEvaluationLog.Status.SUCCEEDED)
                .toList();
        Map<String, Integer> failureTypeCounts = new LinkedHashMap<>();
        logs.stream()
                .filter(log -> log.status() == InterviewAnswerEvaluationLog.Status.FAILED)
                .forEach(log -> failureTypeCounts.merge(log.errorType(), 1, Integer::sum));

        Integer minimumOverallScore = succeeded.stream()
                .map(InterviewAnswerEvaluationLog::overallScore)
                .min(Integer::compareTo)
                .orElse(null);
        Integer maximumOverallScore = succeeded.stream()
                .map(InterviewAnswerEvaluationLog::overallScore)
                .max(Integer::compareTo)
                .orElse(null);
        Double averageOverallScore = succeeded.isEmpty() ? null : average(
                succeeded.stream().mapToLong(InterviewAnswerEvaluationLog::overallScore).sum(),
                succeeded.size());

        return new VersionSummary(
                key.promptVersion(),
                key.rubricVersion(),
                key.modelName(),
                logs.size(),
                succeeded.size(),
                logs.size() - succeeded.size(),
                (int) succeeded.stream().filter(log -> Boolean.TRUE.equals(log.knowledgeGrounded())).count(),
                minimumOverallScore,
                maximumOverallScore,
                averageOverallScore,
                average(logs.stream().mapToLong(InterviewAnswerEvaluationLog::elapsedMillis).sum(), logs.size()),
                logs.stream().mapToLong(InterviewAnswerEvaluationLog::elapsedMillis).max().orElse(0),
                failureTypeCounts);
    }

    private List<BaselineCaseSummary> summarizeBaselineCases(List<InterviewAnswerEvaluationLog> logs) {
        Map<BaselineCaseVersionKey, List<InterviewAnswerEvaluationLog>> groups = new LinkedHashMap<>();
        logs.stream()
                .filter(log -> log.mode() == InterviewAnswerEvaluationLog.Mode.BASELINE)
                .filter(log -> log.baselineRunId() != null && log.baselineCaseId() != null)
                .forEach(log -> {
                    BaselineCaseVersionKey key = new BaselineCaseVersionKey(
                            log.baselineCaseId(),
                            log.promptVersion(),
                            log.rubricVersion(),
                            log.modelName());
                    groups.computeIfAbsent(key, ignored -> new ArrayList<>()).add(log);
                });
        return groups.entrySet().stream()
                .map(entry -> summarizeBaselineCase(entry.getKey(), entry.getValue()))
                .toList();
    }

    private BaselineCaseSummary summarizeBaselineCase(
            BaselineCaseVersionKey key,
            List<InterviewAnswerEvaluationLog> logs) {
        Map<String, InterviewAnswerEvaluationLog> latestLogsByRun = new LinkedHashMap<>();
        logs.forEach(log -> latestLogsByRun.putIfAbsent(log.baselineRunId(), log));
        List<InterviewAnswerEvaluationLog> observations = List.copyOf(latestLogsByRun.values());
        List<InterviewAnswerEvaluationLog> succeeded = observations.stream()
                .filter(log -> log.status() == InterviewAnswerEvaluationLog.Status.SUCCEEDED)
                .toList();
        List<Integer> scores = succeeded.stream()
                .map(InterviewAnswerEvaluationLog::overallScore)
                .toList();
        Map<String, Integer> failureTypeCounts = new LinkedHashMap<>();
        observations.stream()
                .filter(log -> log.status() == InterviewAnswerEvaluationLog.Status.FAILED)
                .forEach(log -> failureTypeCounts.merge(log.errorType(), 1, Integer::sum));

        Integer minimumOverallScore = scores.stream().min(Integer::compareTo).orElse(null);
        Integer maximumOverallScore = scores.stream().max(Integer::compareTo).orElse(null);
        Double averageOverallScore = scores.isEmpty() ? null
                : average(scores.stream().mapToLong(Integer::longValue).sum(), scores.size());
        Integer overallScoreRange = scores.isEmpty() ? null : maximumOverallScore - minimumOverallScore;

        return new BaselineCaseSummary(
                key.baselineCaseId(),
                key.promptVersion(),
                key.rubricVersion(),
                key.modelName(),
                logs.size(),
                observations.size(),
                succeeded.size(),
                observations.size() - succeeded.size(),
                (int) succeeded.stream().filter(log -> Boolean.TRUE.equals(log.knowledgeGrounded())).count(),
                minimumOverallScore,
                maximumOverallScore,
                averageOverallScore,
                overallScoreRange,
                populationVariance(scores),
                average(observations.stream().mapToLong(InterviewAnswerEvaluationLog::elapsedMillis).sum(),
                        observations.size()),
                observations.stream().mapToLong(InterviewAnswerEvaluationLog::elapsedMillis).max().orElse(0),
                failureTypeCounts);
    }

    private Double populationVariance(List<Integer> scores) {
        if (scores.size() < 2) {
            return null;
        }
        double mean = scores.stream().mapToInt(Integer::intValue).average().orElseThrow();
        double variance = scores.stream()
                .mapToDouble(score -> Math.pow(score - mean, 2))
                .average()
                .orElseThrow();
        return round(variance);
    }

    private double average(long total, int count) {
        return BigDecimal.valueOf(total)
                .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private record VersionKey(String promptVersion, String rubricVersion, String modelName) {
    }

    private record BaselineCaseVersionKey(
            String baselineCaseId,
            String promptVersion,
            String rubricVersion,
            String modelName
    ) {
    }

    public record Report(
            InterviewAnswerEvaluationLog.Mode mode,
            String baselineRunId,
            String baselineCaseId,
            int returnedCount,
            int identifiedBaselineCount,
            int unidentifiedBaselineCount,
            List<VersionSummary> versionSummaries,
            List<BaselineCaseSummary> baselineCaseSummaries,
            List<InterviewAnswerEvaluationLog> logs
    ) {

        public Report {
            versionSummaries = List.copyOf(Objects.requireNonNull(versionSummaries, "评价日志汇总不能为空"));
            baselineCaseSummaries = List.copyOf(
                    Objects.requireNonNull(baselineCaseSummaries, "评分基线样本汇总不能为空"));
            logs = List.copyOf(Objects.requireNonNull(logs, "评价日志不能为空"));
        }
    }

    public record VersionSummary(
            String promptVersion,
            String rubricVersion,
            String modelName,
            int totalCount,
            int succeededCount,
            int failedCount,
            int groundedSucceededCount,
            Integer minimumOverallScore,
            Integer maximumOverallScore,
            Double averageOverallScore,
            double averageElapsedMillis,
            long maximumElapsedMillis,
            Map<String, Integer> failureTypeCounts
    ) {

        public VersionSummary {
            failureTypeCounts = Collections.unmodifiableMap(new LinkedHashMap<>(
                    Objects.requireNonNull(failureTypeCounts, "失败类型汇总不能为空")));
        }
    }

    public record BaselineCaseSummary(
            String baselineCaseId,
            String promptVersion,
            String rubricVersion,
            String modelName,
            int totalLogCount,
            int distinctRunCount,
            int succeededRunCount,
            int failedRunCount,
            int groundedSucceededRunCount,
            Integer minimumOverallScore,
            Integer maximumOverallScore,
            Double averageOverallScore,
            Integer overallScoreRange,
            Double overallScorePopulationVariance,
            double averageElapsedMillis,
            long maximumElapsedMillis,
            Map<String, Integer> failureTypeCounts
    ) {

        public BaselineCaseSummary {
            failureTypeCounts = Collections.unmodifiableMap(new LinkedHashMap<>(
                    Objects.requireNonNull(failureTypeCounts, "评分基线失败类型汇总不能为空")));
        }
    }
}
