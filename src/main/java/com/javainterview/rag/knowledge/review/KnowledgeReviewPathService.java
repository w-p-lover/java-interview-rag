package com.javainterview.rag.knowledge.review;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class KnowledgeReviewPathService {
    private static final int MAX_RECOMMENDATION_COUNT = 20;
    private final KnowledgeReviewTaskStore taskStore;
    private final KnowledgeReviewAttemptStore attemptStore;

    public KnowledgeReviewPathService(KnowledgeReviewTaskStore taskStore,
                                      KnowledgeReviewAttemptStore attemptStore) {
        this.taskStore = Objects.requireNonNull(taskStore, "知识复习任务存储不能为空");
        this.attemptStore = Objects.requireNonNull(attemptStore, "知识复习回答存储不能为空");
    }

    public KnowledgeReviewPathResult recommend() {
        return recommend(LocalDate.now());
    }

    KnowledgeReviewPathResult recommend(LocalDate today) {
        Objects.requireNonNull(today, "推荐日期不能为空");
        List<KnowledgeReviewTask> tasks = taskStore.find(null, null, null);
        List<KnowledgeReviewPathResult.Recommendation> recommendations = tasks.stream()
                .filter(task -> task.status() == KnowledgeReviewTaskStatus.PENDING)
                .map(task -> toRecommendation(task, today))
                .sorted(Comparator.comparing(KnowledgeReviewPathResult.Recommendation::recommendedReviewDate)
                        .thenComparing(KnowledgeReviewPathResult.Recommendation::latestOverallScore,
                                Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(KnowledgeReviewPathResult.Recommendation::attemptCount)
                        .thenComparing(KnowledgeReviewPathResult.Recommendation::claim))
                .limit(MAX_RECOMMENDATION_COUNT)
                .toList();
        return new KnowledgeReviewPathResult(today, tasks.size(), recommendations);
    }

    private KnowledgeReviewPathResult.Recommendation toRecommendation(KnowledgeReviewTask task, LocalDate today) {
        List<KnowledgeReviewAttempt> attempts = attemptStore.findByTaskId(task.id()).stream()
                .sorted(Comparator.comparing(KnowledgeReviewAttempt::createdAt))
                .toList();
        KnowledgeReviewAttempt latest = attempts.isEmpty() ? null : attempts.get(attempts.size() - 1);
        int intervalDays = intervalDays(latest);
        LocalDate calculatedDate = latest == null ? today : latest.createdAt().toLocalDate().plusDays(intervalDays);
        LocalDate recommendedDate = task.plannedReviewDate() != null && task.plannedReviewDate().isAfter(calculatedDate)
                ? task.plannedReviewDate() : calculatedDate;
        if (recommendedDate.isBefore(today)) recommendedDate = today;
        Integer latestScore = latest == null ? null : latest.evaluation().overallScore();
        Integer bestScore = attempts.stream().map(attempt -> attempt.evaluation().overallScore())
                .max(Integer::compareTo).orElse(null);
        return new KnowledgeReviewPathResult.Recommendation(task.id(), task.claim(), task.question(), task.status(),
                task.plannedReviewDate(), attempts.size(), latestScore, bestScore, recommendedDate, intervalDays,
                !recommendedDate.isAfter(today), reason(latest, recommendedDate, today));
    }

    private int intervalDays(KnowledgeReviewAttempt latest) {
        if (latest == null) return 1;
        if (latest.evaluation().overallScore() < 70 || !latest.evaluation().gaps().isEmpty()) return 1;
        if (latest.evaluation().overallScore() < 85) return 3;
        return 7;
    }

    private String reason(KnowledgeReviewAttempt latest, LocalDate date, LocalDate today) {
        if (latest == null) return "尚未练习，今天开始第一次复习";
        if (latest.evaluation().overallScore() < 70) return "最近得分低于 70，建议 1 天后再次练习";
        if (!latest.evaluation().gaps().isEmpty()) return "最近回答仍有遗漏，建议 1 天后再次练习";
        if (date.isAfter(today)) return "最近回答已改善，按间隔安排下一次复习";
        return "复习日期已到，优先巩固该知识点";
    }
}
