package com.javainterview.rag.interview.draft;

import com.javainterview.rag.interview.preparation.InterviewPreparationResult;
import com.javainterview.rag.interview.question.InterviewQuestionGenerationResult;
import com.javainterview.rag.interview.question.InterviewQuestionSource;

import java.util.List;
import java.util.Objects;

public record InterviewDraftResult(
        String draftId,
        boolean reused,
        int targetQuestionCount,
        int actualQuestionCount,
        InterviewDraftStatus status,
        List<InterviewQuestionSource> unavailableSources,
        String failureCode,
        String failureMessage,
        InterviewPreparationResult preparation,
        InterviewQuestionGenerationResult generation
) {

    public InterviewDraftResult {
        if (draftId == null || draftId.isBlank()) {
            throw new IllegalArgumentException("草稿 ID 不能为空");
        }
        if (targetQuestionCount < 3 || targetQuestionCount > 8) {
            throw new IllegalArgumentException("目标题数必须在 3 到 8 之间");
        }
        if (actualQuestionCount < 1 || actualQuestionCount > targetQuestionCount) {
            throw new IllegalArgumentException("实际题数必须在 1 到目标题数之间");
        }
        status = Objects.requireNonNull(status, "草稿状态不能为空");
        unavailableSources = List.copyOf(Objects.requireNonNull(
                unavailableSources, "不可用题目来源不能为空"));
        preparation = Objects.requireNonNull(preparation, "面试准备结果不能为空");
        generation = Objects.requireNonNull(generation, "面试出题结果不能为空");
        if (generation.questions().size() != actualQuestionCount) {
            throw new IllegalArgumentException("草稿实际题数与题目列表不一致");
        }
    }

    public InterviewDraftResult withReused(boolean nextReused) {
        return new InterviewDraftResult(
                draftId,
                nextReused,
                targetQuestionCount,
                actualQuestionCount,
                status,
                unavailableSources,
                failureCode,
                failureMessage,
                preparation,
                generation);
    }
}
