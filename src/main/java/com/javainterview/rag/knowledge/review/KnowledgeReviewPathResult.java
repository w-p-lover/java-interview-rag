package com.javainterview.rag.knowledge.review;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record KnowledgeReviewPathResult(
        LocalDate generatedFor,
        int taskCount,
        List<Recommendation> recommendations
) {
    public KnowledgeReviewPathResult {
        generatedFor = Objects.requireNonNull(generatedFor, "推荐日期不能为空");
        if (taskCount < 0) {
            throw new IllegalArgumentException("复习任务数量不能为负数");
        }
        recommendations = List.copyOf(Objects.requireNonNull(recommendations, "复习推荐不能为空"));
    }

    public record Recommendation(
            String taskId,
            String claim,
            String question,
            KnowledgeReviewTaskStatus status,
            LocalDate plannedReviewDate,
            int attemptCount,
            Integer latestOverallScore,
            Integer bestOverallScore,
            LocalDate recommendedReviewDate,
            int intervalDays,
            boolean due,
            String reason
    ) {
        public Recommendation {
            if (taskId == null || taskId.isBlank() || claim == null || claim.isBlank()
                    || question == null || question.isBlank() || reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("知识复习推荐缺少必要信息");
            }
            if (attemptCount < 0 || intervalDays < 1
                    || (latestOverallScore != null && (latestOverallScore < 0 || latestOverallScore > 100))
                    || (bestOverallScore != null && (bestOverallScore < 0 || bestOverallScore > 100))) {
                throw new IllegalArgumentException("知识复习推荐数值无效");
            }
            status = Objects.requireNonNull(status, "复习任务状态不能为空");
            recommendedReviewDate = Objects.requireNonNull(recommendedReviewDate, "推荐复习日期不能为空");
        }
    }
}
