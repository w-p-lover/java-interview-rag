package com.javainterview.rag.interview.review;

import java.util.List;
import java.util.Objects;

public record InterviewReviewAttemptComparison(
        String attemptId,
        int attemptNumber,
        int technicalDeltaFromOriginal,
        int completenessDeltaFromOriginal,
        int communicationDeltaFromOriginal,
        Integer technicalDeltaFromPrevious,
        Integer completenessDeltaFromPrevious,
        Integer communicationDeltaFromPrevious,
        List<String> originalGapsStillPresent,
        List<String> originalGapsNotRepeated,
        List<String> previousGapsNotRepeated,
        List<String> newGapsSincePrevious
) {

    public InterviewReviewAttemptComparison {
        if (attemptId == null || attemptId.isBlank()) {
            throw new IllegalArgumentException("复习回答 ID 不能为空");
        }
        if (attemptNumber < 1) {
            throw new IllegalArgumentException("复习回答序号必须从 1 开始");
        }
        boolean hasPreviousDeltas = technicalDeltaFromPrevious != null
                && completenessDeltaFromPrevious != null
                && communicationDeltaFromPrevious != null;
        boolean hasNoPreviousDeltas = technicalDeltaFromPrevious == null
                && completenessDeltaFromPrevious == null
                && communicationDeltaFromPrevious == null;
        if (!hasPreviousDeltas && !hasNoPreviousDeltas) {
            throw new IllegalArgumentException("相对上次回答的三维分差必须同时存在或同时为空");
        }
        originalGapsStillPresent = copy(originalGapsStillPresent, "仍出现的原遗漏不能为空");
        originalGapsNotRepeated = copy(originalGapsNotRepeated, "未再出现的原遗漏不能为空");
        previousGapsNotRepeated = copy(previousGapsNotRepeated, "较上次未再出现的遗漏不能为空");
        newGapsSincePrevious = copy(newGapsSincePrevious, "本次新增评价项不能为空");
    }

    private static List<String> copy(List<String> values, String message) {
        return List.copyOf(Objects.requireNonNull(values, message));
    }
}
