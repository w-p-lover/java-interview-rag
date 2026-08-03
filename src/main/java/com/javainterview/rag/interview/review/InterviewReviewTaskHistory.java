package com.javainterview.rag.interview.review;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record InterviewReviewTaskHistory(
        String reviewTaskId,
        int questionIndex,
        int attemptCount,
        int originalOverallScore,
        Integer firstOverallScore,
        Integer latestOverallScore,
        Integer bestOverallScore,
        Integer latestScoreDeltaFromOriginal,
        List<String> unresolvedGaps,
        LocalDateTime latestAttemptAt,
        List<InterviewReviewAttempt> attempts
) {

    public InterviewReviewTaskHistory {
        if (reviewTaskId == null || reviewTaskId.isBlank()) {
            throw new IllegalArgumentException("复习任务 ID 不能为空");
        }
        if (questionIndex < 0 || attemptCount < 0) {
            throw new IllegalArgumentException("复习历史计数不能为负数");
        }
        if (originalOverallScore < 0 || originalOverallScore > 100) {
            throw new IllegalArgumentException("正式回答总分必须在 0 到 100 之间");
        }
        unresolvedGaps = List.copyOf(Objects.requireNonNull(
                unresolvedGaps, "未解决遗漏不能为空"));
        attempts = List.copyOf(Objects.requireNonNull(attempts, "复习回答历史不能为空"));
        if (attemptCount != attempts.size()) {
            throw new IllegalArgumentException("复习回答次数与历史数量不一致");
        }
        if (attemptCount == 0) {
            if (firstOverallScore != null || latestOverallScore != null
                    || bestOverallScore != null || latestScoreDeltaFromOriginal != null
                    || latestAttemptAt != null) {
                throw new IllegalArgumentException("未作答任务不能包含复习评分");
            }
        } else if (firstOverallScore == null || latestOverallScore == null
                || bestOverallScore == null || latestScoreDeltaFromOriginal == null
                || latestAttemptAt == null) {
            throw new IllegalArgumentException("已作答任务必须包含完整复习汇总");
        }
    }
}
