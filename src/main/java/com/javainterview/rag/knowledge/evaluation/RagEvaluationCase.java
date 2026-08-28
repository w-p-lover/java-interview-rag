package com.javainterview.rag.knowledge.evaluation;

import java.util.List;
import java.util.Objects;

public record RagEvaluationCase(
        String id,
        String question,
        RagEvaluationCaseType type,
        List<String> expectedSources,
        List<List<String>> expectedHeadingPaths,
        List<RagEvaluationStructureExpectation> expectedStructures,
        List<String> expectedAnswerPoints
) {

    public RagEvaluationCase(String id, String question, RagEvaluationCaseType type,
                             List<String> expectedSources, List<List<String>> expectedHeadingPaths,
                             List<String> expectedAnswerPoints) {
        this(id, question, type, expectedSources, expectedHeadingPaths, List.of(), expectedAnswerPoints);
    }

    public RagEvaluationCase {
        id = requireText(id, "评估题标识不能为空");
        question = requireText(question, "评估问题不能为空");
        type = Objects.requireNonNull(type, "评估题型不能为空");
        expectedSources = copyTexts(expectedSources, "期望来源不能为空");
        expectedHeadingPaths = copyHeadingPaths(expectedHeadingPaths);
        expectedStructures = List.copyOf(Objects.requireNonNull(expectedStructures, "结构期望不能为空"));
        if (type != RagEvaluationCaseType.NO_ANSWER
                && (expectedSources.isEmpty() || expectedHeadingPaths.isEmpty())) {
            throw new IllegalArgumentException("可回答评估题必须配置期望来源和标题路径");
        }
        if (type == RagEvaluationCaseType.NO_ANSWER && !expectedStructures.isEmpty()) {
            throw new IllegalArgumentException("无答案评估题不能配置结构期望");
        }
        List<String> configuredSources = expectedSources;
        List<List<String>> configuredHeadingPaths = expectedHeadingPaths;
        if (expectedStructures.stream().anyMatch(expectation ->
                !configuredSources.contains(expectation.source())
                        || !configuredHeadingPaths.contains(expectation.headingPath()))) {
            throw new IllegalArgumentException("结构期望必须属于已配置的来源和标题路径");
        }
        expectedAnswerPoints = copyRequiredTexts(expectedAnswerPoints, "预期回答要点不能为空");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    private static List<String> copyRequiredTexts(List<String> values, String message) {
        List<String> copied = copyTexts(values, message);
        if (copied.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return copied;
    }

    private static List<String> copyTexts(List<String> values, String message) {
        Objects.requireNonNull(values, message);
        if (values.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException(message);
        }
        return List.copyOf(values);
    }

    private static List<List<String>> copyHeadingPaths(List<List<String>> headingPaths) {
        Objects.requireNonNull(headingPaths, "期望标题路径不能为空");
        return headingPaths.stream()
                .map(path -> copyRequiredTexts(path, "期望标题路径不能为空"))
                .toList();
    }
}
