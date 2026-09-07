package com.javainterview.rag.interview.review;

import com.javainterview.rag.interview.session.InterviewSessionStore;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

public class InterviewReviewPathService {

    private static final int MAX_SESSION_COUNT = 8;
    private static final int MAX_RECOMMENDATION_COUNT = 20;
    private final InterviewSessionStore sessionStore;
    private final InterviewReviewTaskStore taskStore;
    private final InterviewReviewAttemptStore attemptStore;

    public InterviewReviewPathService(InterviewSessionStore sessionStore,
                                      InterviewReviewTaskStore taskStore,
                                      InterviewReviewAttemptStore attemptStore) {
        this.sessionStore = Objects.requireNonNull(sessionStore, "面试会话存储不能为空");
        this.taskStore = Objects.requireNonNull(taskStore, "复习任务存储不能为空");
        this.attemptStore = Objects.requireNonNull(attemptStore, "复习回答存储不能为空");
    }

    public InterviewReviewPathResult recommend(List<String> sessionIds) {
        return recommend(sessionIds, LocalDate.now());
    }

    InterviewReviewPathResult recommend(List<String> sessionIds, LocalDate today) {
        List<String> normalizedIds = normalizeSessionIds(sessionIds);
        Objects.requireNonNull(today, "推荐日期不能为空");
        normalizedIds.forEach(sessionId -> sessionStore.findById(sessionId)
                .orElseThrow(() -> new NoSuchElementException("面试会话不存在")));
        List<InterviewReviewTask> tasks = taskStore.findBySessionIds(normalizedIds);
        List<InterviewReviewPathResult.Recommendation> recommendations = tasks.stream()
                .filter(task -> task.status() == InterviewReviewTaskStatus.PENDING)
                .map(task -> toRecommendation(task, today))
                .sorted(Comparator.comparing(InterviewReviewPathResult.Recommendation::recommendedReviewDate)
                        .thenComparing(InterviewReviewPathResult.Recommendation::latestOverallScore,
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(InterviewReviewPathResult.Recommendation::attemptCount)
                        .thenComparing(InterviewReviewPathResult.Recommendation::question))
                .limit(MAX_RECOMMENDATION_COUNT)
                .toList();
        return new InterviewReviewPathResult(
                today, normalizedIds.size(), tasks.size(), recommendations);
    }

    private InterviewReviewPathResult.Recommendation toRecommendation(
            InterviewReviewTask task, LocalDate today) {
        List<InterviewReviewAttempt> attempts = attemptStore
                .findByReviewTaskId(task.sessionId(), task.id()).stream()
                .sorted(Comparator.comparingInt(InterviewReviewAttempt::attemptNumber))
                .toList();
        InterviewReviewAttempt latest = attempts.isEmpty() ? null : attempts.get(attempts.size() - 1);
        int intervalDays = intervalDays(latest);
        LocalDate calculatedDate = latest == null
                ? today
                : latest.createdAt().toLocalDate().plusDays(intervalDays);
        LocalDate recommendedDate = task.plannedReviewDate() != null
                && task.plannedReviewDate().isAfter(calculatedDate)
                ? task.plannedReviewDate() : calculatedDate;
        if (recommendedDate.isBefore(today)) {
            recommendedDate = today;
        }
        String reason = reason(latest, recommendedDate, today);
        Integer latestScore = latest == null ? null : latest.evaluation().overallScore();
        Integer bestScore = attempts.stream()
                .map(attempt -> attempt.evaluation().overallScore())
                .max(Integer::compareTo)
                .orElse(null);
        Integer originalScore = latest == null ? null : latest.originalOverallScore();
        return new InterviewReviewPathResult.Recommendation(
                task.sessionId(), task.id(), task.questionIndex(), task.question(), task.objective(),
                task.gaps(), task.improvementSuggestion(), task.status(), task.plannedReviewDate(),
                attempts.size(), originalScore, latestScore, bestScore,
                recommendedDate, intervalDays, reason);
    }

    private int intervalDays(InterviewReviewAttempt latest) {
        if (latest == null) {
            return 1;
        }
        int score = latest.evaluation().overallScore();
        if (score < 70 || !latest.evaluation().gaps().isEmpty()) {
            return 1;
        }
        if (score < 85) {
            return 3;
        }
        return 7;
    }

    private String reason(InterviewReviewAttempt latest, LocalDate recommendedDate,
                          LocalDate today) {
        if (latest == null) {
            return "尚未练习，今天开始第一次复习";
        }
        if (latest.evaluation().overallScore() < 70) {
            return "最近得分低于 70，建议 1 天后再次练习";
        }
        if (!latest.evaluation().gaps().isEmpty()) {
            return "最近回答仍有遗漏，建议 1 天后再次练习";
        }
        if (recommendedDate.isAfter(today)) {
            return "最近回答已改善，按间隔安排下一次复习";
        }
        return "复习日期已到，优先巩固该主题";
    }

    private List<String> normalizeSessionIds(List<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            throw new IllegalArgumentException("至少需要一个面试会话 ID");
        }
        List<String> normalized = sessionIds.stream()
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("面试会话 ID 不能为空");
        }
        if (normalized.size() > MAX_SESSION_COUNT) {
            throw new IllegalArgumentException("最多同时分析 8 个面试会话");
        }
        return normalized;
    }
}
