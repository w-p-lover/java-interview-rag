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
        List<String> answerEvidence,
        List<KnowledgeEvidenceDraft> knowledgeEvidence,
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
        answerEvidence = copyEvidence(answerEvidence, "回答证据不能为空");
        knowledgeEvidence = copyKnowledgeEvidence(knowledgeEvidence);
        if (followUpNeeded) {
            followUpQuestion = requireText(followUpQuestion, "追问题目不能为空");
            followUpObjective = requireText(followUpObjective, "追问目标不能为空");
            followUpKnowledgeQuery = optionalText(followUpKnowledgeQuery);
            if (followUpKnowledgeQuery.isBlank()) {
                followUpKnowledgeQuery = followUpQuestion;
            }
        } else {
            followUpQuestion = optionalText(followUpQuestion);
            followUpObjective = optionalText(followUpObjective);
            followUpKnowledgeQuery = optionalText(followUpKnowledgeQuery);
        }
    }

    public InterviewAnswerEvaluationDraft(
            int technicalScore,
            int completenessScore,
            int communicationScore,
            List<String> strengths,
            List<String> gaps,
            String improvementSuggestion,
            boolean followUpNeeded,
            String followUpQuestion,
            String followUpObjective,
            String followUpKnowledgeQuery) {
        this(technicalScore, completenessScore, communicationScore,
                strengths, gaps, improvementSuggestion, List.of(), List.of(),
                followUpNeeded, followUpQuestion, followUpObjective, followUpKnowledgeQuery);
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

    private static List<String> copyEvidence(List<String> values, String message) {
        List<String> copied = copyTexts(values, message).stream()
                .map(String::strip)
                .distinct()
                .toList();
        if (copied.size() > 3) {
            throw new IllegalArgumentException("回答证据最多返回 3 条");
        }
        return copied;
    }

    private static List<KnowledgeEvidenceDraft> copyKnowledgeEvidence(
            List<KnowledgeEvidenceDraft> values) {
        Objects.requireNonNull(values, "知识证据不能为空");
        List<KnowledgeEvidenceDraft> copied = values.stream().distinct().toList();
        if (copied.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("知识证据不能包含空值");
        }
        if (copied.size() > 3) {
            throw new IllegalArgumentException("知识证据最多返回 3 条");
        }
        return copied;
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

    public record KnowledgeEvidenceDraft(int sourceIndex, String quote) {

        public KnowledgeEvidenceDraft {
            if (sourceIndex < 1) {
                throw new IllegalArgumentException("知识证据来源序号必须从 1 开始");
            }
            quote = requireText(quote, "知识证据引文不能为空");
        }
    }
}
