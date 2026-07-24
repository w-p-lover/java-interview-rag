package com.javainterview.rag.knowledge.evaluation;

import java.util.List;
import java.util.Objects;

public record RagEvaluationCase(
        String id,
        String question,
        List<String> expectedSources,
        List<String> expectedAnswerPoints
) {

    public RagEvaluationCase {
        id = requireText(id, "评估题标识不能为空");
        question = requireText(question, "评估问题不能为空");
        expectedSources = copyRequiredTexts(expectedSources, "期望来源不能为空");
        expectedAnswerPoints = copyRequiredTexts(expectedAnswerPoints, "预期回答要点不能为空");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static List<String> copyRequiredTexts(List<String> values, String message) {
        Objects.requireNonNull(values, message);
        if (values.isEmpty() || values.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException(message);
        }
        return List.copyOf(values);
    }
}
