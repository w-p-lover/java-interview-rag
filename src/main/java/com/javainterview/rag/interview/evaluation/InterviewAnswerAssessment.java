package com.javainterview.rag.interview.evaluation;

import com.javainterview.rag.interview.question.InterviewQuestion;

import java.util.Objects;

public record InterviewAnswerAssessment(
        InterviewAnswerEvaluation evaluation,
        InterviewQuestion followUpQuestion
) {

    public InterviewAnswerAssessment {
        evaluation = Objects.requireNonNull(evaluation, "回答评价不能为空");
    }
}
