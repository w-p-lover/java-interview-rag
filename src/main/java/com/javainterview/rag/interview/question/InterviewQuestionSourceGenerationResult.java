package com.javainterview.rag.interview.question;

import java.util.List;
import java.util.Objects;

public record InterviewQuestionSourceGenerationResult(
        String targetRole,
        String promptVersion,
        List<InterviewQuestion> questions,
        List<InterviewQuestionSource> unavailableSources
) {

    public InterviewQuestionSourceGenerationResult {
        targetRole = Objects.requireNonNull(targetRole, "目标岗位不能为空");
        promptVersion = Objects.requireNonNull(promptVersion, "Prompt 版本不能为空");
        questions = List.copyOf(Objects.requireNonNull(questions, "面试题不能为空"));
        unavailableSources = List.copyOf(Objects.requireNonNull(
                unavailableSources, "不可用题目来源不能为空"));
    }
}
