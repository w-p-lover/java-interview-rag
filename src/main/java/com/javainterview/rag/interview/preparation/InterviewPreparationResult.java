package com.javainterview.rag.interview.preparation;

import java.util.Objects;

public record InterviewPreparationResult(
        String targetRole,
        String promptVersion,
        InterviewPreparationProfile profile
) {

    public InterviewPreparationResult {
        targetRole = requireText(targetRole, "目标岗位不能为空");
        promptVersion = requireText(promptVersion, "Prompt 版本不能为空");
        profile = Objects.requireNonNull(profile, "面试准备画像不能为空");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
