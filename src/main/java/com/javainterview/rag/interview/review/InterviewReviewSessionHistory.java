package com.javainterview.rag.interview.review;

import java.util.List;
import java.util.Objects;

public record InterviewReviewSessionHistory(
        String sessionId,
        int totalTaskCount,
        int practicedTaskCount,
        int totalAttemptCount,
        int improvedTaskCount,
        List<InterviewReviewTaskHistory> tasks
) {

    public InterviewReviewSessionHistory {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("面试会话 ID 不能为空");
        }
        tasks = List.copyOf(Objects.requireNonNull(tasks, "复习任务历史不能为空"));
        int expectedPracticedTaskCount = (int) tasks.stream()
                .filter(task -> task.attemptCount() > 0)
                .count();
        int expectedTotalAttemptCount = tasks.stream()
                .mapToInt(InterviewReviewTaskHistory::attemptCount)
                .sum();
        int expectedImprovedTaskCount = (int) tasks.stream()
                .filter(task -> task.latestScoreDeltaFromOriginal() != null
                        && task.latestScoreDeltaFromOriginal() > 0)
                .count();
        if (totalTaskCount != tasks.size()
                || practicedTaskCount != expectedPracticedTaskCount
                || totalAttemptCount != expectedTotalAttemptCount
                || improvedTaskCount != expectedImprovedTaskCount) {
            throw new IllegalArgumentException("复习历史汇总计数不一致");
        }
    }
}
