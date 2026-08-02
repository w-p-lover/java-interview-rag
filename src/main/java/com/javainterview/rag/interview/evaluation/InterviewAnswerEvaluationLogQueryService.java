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

    public Report findRecent(InterviewAnswerEvaluationLog.Mode mode, int limit) {
        Objects.requireNonNull(mode, "评价模式不能为空");
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("日志查询数量必须在 1 到 100 之间");
        }
        List<InterviewAnswerEvaluationLog> logs = logStore.findRecent(mode, limit);
        return new Report(mode, logs.size(), summarize(logs), logs);
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

    private double average(long total, int count) {
        return BigDecimal.valueOf(total)
                .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private record VersionKey(String promptVersion, String rubricVersion, String modelName) {
    }

    public record Report(
            InterviewAnswerEvaluationLog.Mode mode,
            int returnedCount,
            List<VersionSummary> versionSummaries,
            List<InterviewAnswerEvaluationLog> logs
    ) {

        public Report {
            versionSummaries = List.copyOf(Objects.requireNonNull(versionSummaries, "评价日志汇总不能为空"));
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
}
