package com.javainterview.rag.interview.evaluation;

import java.util.List;
import java.util.Objects;

/** 复习评价使用的历史作答摘要。 */
public record InterviewAnswerPreviousAttempt(
        String answer,
        InterviewAnswerEvaluation evaluation
) {

    public InterviewAnswerPreviousAttempt {
        if (answer == null || answer.isBlank()) {
            throw new IllegalArgumentException("历史回答不能为空");
        }
        evaluation = Objects.requireNonNull(evaluation, "历史回答评价不能为空");
    }

    public List<String> gaps() {
        return evaluation.gaps();
    }
}
