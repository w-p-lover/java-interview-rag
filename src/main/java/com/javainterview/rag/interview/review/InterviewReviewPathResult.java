package com.javainterview.rag.interview.review;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record InterviewReviewPathResult(
        LocalDate generatedFor,
        int sessionCount,
        int taskCount,
        List<Recommendation> recommendations
) {

    public InterviewReviewPathResult {
        generatedFor = Objects.requireNonNull(generatedFor, "推荐日期不能为空");
        if (sessionCount < 1 || taskCount < 0) {
            throw new IllegalArgumentException("复习路径统计数量无效");
        }
        recommendations = List.copyOf(Objects.requireNonNull(
                recommendations, "复习推荐不能为空"));
    }

    public record Recommendation(
            String sessionId,
            String taskId,
            int questionIndex,
            String question,
            String objective,
            List<String> gaps,
            String improvementSuggestion,
            InterviewReviewTaskStatus status,
            LocalDate plannedReviewDate,
            int attemptCount,
            Integer originalOverallScore,
            Integer latestOverallScore,
            Integer bestOverallScore,
            LocalDate recommendedReviewDate,
            int intervalDays,
            String reason
    ) {

        public Recommendation {
            if (sessionId == null || sessionId.isBlank()
                    || taskId == null || taskId.isBlank()
                    || question == null || question.isBlank()
                    || objective == null || objective.isBlank()
                    || reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("复习推荐缺少必要信息");
            }
            if (questionIndex < 0 || attemptCount < 0
                    || (originalOverallScore != null
                    && (originalOverallScore < 0 || originalOverallScore > 100))
                    || (latestOverallScore != null
                    && (latestOverallScore < 0 || latestOverallScore > 100))
                    || (bestOverallScore != null
                    && (bestOverallScore < 0 || bestOverallScore > 100))
                    || intervalDays < 1) {
                throw new IllegalArgumentException("复习推荐数值无效");
            }
            gaps = List.copyOf(Objects.requireNonNull(gaps, "复习遗漏项不能为空"));
            status = Objects.requireNonNull(status, "复习任务状态不能为空");
            recommendedReviewDate = Objects.requireNonNull(
                    recommendedReviewDate, "推荐复习日期不能为空");
        }
    }
}
