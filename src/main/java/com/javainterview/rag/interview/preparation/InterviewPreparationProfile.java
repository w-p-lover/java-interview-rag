package com.javainterview.rag.interview.preparation;

import java.util.List;
import java.util.Objects;

public record InterviewPreparationProfile(
        String candidateSummary,
        List<String> resumeEvidence,
        List<String> jobRequirements,
        List<InterviewFocusArea> focusAreas
) {

    public InterviewPreparationProfile {
        candidateSummary = requireText(candidateSummary, "候选人摘要不能为空");
        resumeEvidence = copyTexts(resumeEvidence, true, "简历证据不能为空");
        jobRequirements = copyTexts(jobRequirements, false, "岗位要求不能为空");
        focusAreas = List.copyOf(Objects.requireNonNull(focusAreas, "面试关注领域不能为空"));
        if (focusAreas.isEmpty()) {
            throw new IllegalArgumentException("面试关注领域不能为空");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static List<String> copyTexts(List<String> values, boolean required, String message) {
        Objects.requireNonNull(values, message);
        if ((required && values.isEmpty()) || values.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException(message);
        }
        return List.copyOf(values);
    }
}
