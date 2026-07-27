package com.javainterview.rag.interview.evaluation;

import java.util.List;
import java.util.Objects;

public record InterviewAnswerEvaluation(
        int technicalScore,
        int completenessScore,
        int communicationScore,
        int overallScore,
        List<String> strengths,
        List<String> gaps,
        String improvementSuggestion,
        boolean knowledgeGrounded
) {

    public InterviewAnswerEvaluation {
        requireScore(technicalScore);
        requireScore(completenessScore);
        requireScore(communicationScore);
        requireScore(overallScore);
        int expectedOverall = Math.round((technicalScore + completenessScore + communicationScore) / 3.0f);
        if (overallScore != expectedOverall) {
            throw new IllegalArgumentException("总分必须由三项评分计算");
        }
        strengths = copyTexts(strengths, "回答优点不能为空");
        gaps = copyTexts(gaps, "回答遗漏不能为空");
        if (improvementSuggestion == null || improvementSuggestion.isBlank()) {
            throw new IllegalArgumentException("改进建议不能为空");
        }
    }

    public static InterviewAnswerEvaluation from(InterviewAnswerEvaluationDraft draft,
                                                 boolean knowledgeGrounded) {
        int overall = Math.round((draft.technicalScore()
                + draft.completenessScore() + draft.communicationScore()) / 3.0f);
        return new InterviewAnswerEvaluation(
                draft.technicalScore(),
                draft.completenessScore(),
                draft.communicationScore(),
                overall,
                draft.strengths(),
                draft.gaps(),
                draft.improvementSuggestion(),
                knowledgeGrounded
        );
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
}
