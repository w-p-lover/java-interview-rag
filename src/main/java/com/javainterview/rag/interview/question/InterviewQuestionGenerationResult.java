package com.javainterview.rag.interview.question;

import java.util.List;
import java.util.Objects;

public record InterviewQuestionGenerationResult(
        String targetRole,
        String promptVersion,
        List<InterviewQuestion> questions
) {

    private static final int MAX_QUESTION_COUNT = 8;

    public InterviewQuestionGenerationResult {
        targetRole = requireText(targetRole, "目标岗位不能为空");
        promptVersion = requireText(promptVersion, "Prompt 版本不能为空");
        questions = List.copyOf(Objects.requireNonNull(questions, "面试题不能为空"));
        if (questions.isEmpty() || questions.size() > MAX_QUESTION_COUNT) {
            throw new IllegalArgumentException("首轮面试题数量必须在 1 到 8 之间");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
