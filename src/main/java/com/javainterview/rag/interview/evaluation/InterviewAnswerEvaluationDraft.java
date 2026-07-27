package com.javainterview.rag.interview.evaluation;

import java.util.List;
import java.util.Objects;

public record InterviewAnswerEvaluationDraft(
        int technicalScore,
        int completenessScore,
        int communicationScore,
        List<String> strengths,
        List<String> gaps,
        String improvementSuggestion,
        boolean followUpNeeded,
        String followUpQuestion,
        String followUpObjective,
        String followUpKnowledgeQuery
) {

    public InterviewAnswerEvaluationDraft {
        requireScore(technicalScore);
        requireScore(completenessScore);
        requireScore(communicationScore);
        strengths = copyTexts(strengths, "回答优点不能为空");
        gaps = copyTexts(gaps, "回答遗漏不能为空");
        improvementSuggestion = requireText(improvementSuggestion, "改进建议不能为空");
        if (followUpNeeded) {
            followUpQuestion = requireText(followUpQuestion, "追问题目不能为空");
            followUpObjective = requireText(followUpObjective, "追问目标不能为空");
            followUpKnowledgeQuery = requireText(followUpKnowledgeQuery, "追问检索查询不能为空");
        } else {
            followUpQuestion = optionalText(followUpQuestion);
            followUpObjective = optionalText(followUpObjective);
            followUpKnowledgeQuery = optionalText(followUpKnowledgeQuery);
        }
    }

    private static void requireScore(int score) {
        if (score < 0 || score > 100) {
            throw new IllegalArgumentException("面试评分必须在 0 到 100 之间");
        }
    }

    private static List<String> copyTexts(List<String> values, String message) {
        Objects.requireNonNull(values, message);
        if (values.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException(message);
        }
        return List.copyOf(values);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static String optionalText(String value) {
        return value == null ? "" : value;
    }
}
