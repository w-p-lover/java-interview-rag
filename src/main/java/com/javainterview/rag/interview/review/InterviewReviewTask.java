package com.javainterview.rag.interview.review;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public record InterviewReviewTask(
        String id,
        String sessionId,
        int questionIndex,
        String question,
        String objective,
        String rubricVersion,
        List<String> weakDimensions,
        List<String> gaps,
        String improvementSuggestion,
        boolean knowledgeGrounded,
        List<InterviewReviewKnowledgeSource> knowledgeSources,
        InterviewReviewTaskStatus status,
        LocalDate plannedReviewDate,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {

    public InterviewReviewTask(
            String id,
            String sessionId,
            int questionIndex,
            String question,
            String objective,
            List<String> gaps,
            String improvementSuggestion,
            boolean knowledgeGrounded,
            List<InterviewReviewKnowledgeSource> knowledgeSources,
            InterviewReviewTaskStatus status,
            LocalDateTime createdAt,
            LocalDateTime completedAt
    ) {
        this(id, sessionId, questionIndex, question, objective,
                "interview-answer-rubric-v2", List.of(), gaps, improvementSuggestion,
                knowledgeGrounded, knowledgeSources, status, null, createdAt, completedAt);
    }

    public InterviewReviewTask(
            String id,
            String sessionId,
            int questionIndex,
            String question,
            String objective,
            List<String> gaps,
            String improvementSuggestion,
            boolean knowledgeGrounded,
            List<InterviewReviewKnowledgeSource> knowledgeSources,
            InterviewReviewTaskStatus status,
            LocalDate plannedReviewDate,
            LocalDateTime createdAt,
            LocalDateTime completedAt
    ) {
        this(id, sessionId, questionIndex, question, objective,
                "interview-answer-rubric-v2", List.of(), gaps, improvementSuggestion,
                knowledgeGrounded, knowledgeSources, status, plannedReviewDate, createdAt, completedAt);
    }

    public InterviewReviewTask {
        id = requireText(id, "复习任务 ID 不能为空");
        sessionId = requireText(sessionId, "面试会话 ID 不能为空");
        if (questionIndex < 0) {
            throw new IllegalArgumentException("复习任务题号不能为负数");
        }
        question = requireText(question, "复习题目不能为空");
        objective = requireText(objective, "复习目标不能为空");
        rubricVersion = requireText(rubricVersion, "复习评分规则版本不能为空");
        weakDimensions = copyOptionalTexts(weakDimensions, "复习薄弱维度不能包含空值");
        gaps = copyTexts(gaps, "复习遗漏项不能为空");
        if (gaps.isEmpty()) {
            throw new IllegalArgumentException("复习任务必须包含遗漏项");
        }
        improvementSuggestion = requireText(improvementSuggestion, "复习改进建议不能为空");
        knowledgeSources = List.copyOf(Objects.requireNonNull(
                knowledgeSources, "复习知识来源不能为空"));
        if (knowledgeGrounded != !knowledgeSources.isEmpty()) {
            throw new IllegalArgumentException("复习知识来源状态与实际来源不一致");
        }
        status = Objects.requireNonNull(status, "复习任务状态不能为空");
        createdAt = Objects.requireNonNull(createdAt, "复习任务创建时间不能为空");
        if (status == InterviewReviewTaskStatus.PENDING && completedAt != null) {
            throw new IllegalArgumentException("待复习任务不能包含完成时间");
        }
        if (status == InterviewReviewTaskStatus.COMPLETED && completedAt == null) {
            throw new IllegalArgumentException("已完成复习任务必须包含完成时间");
        }
    }

    private static List<String> copyTexts(List<String> values, String message) {
        Objects.requireNonNull(values, message);
        if (values.stream().anyMatch(value -> value == null || value.isBlank())) {
            throw new IllegalArgumentException(message);
        }
        return List.copyOf(values);
    }

    private static List<String> copyOptionalTexts(List<String> values, String message) {
        Objects.requireNonNull(values, message);
        if (values.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(message);
        }
        List<String> copied = values.stream().map(String::strip).distinct().toList();
        if (copied.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException(message);
        }
        return List.copyOf(copied);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
