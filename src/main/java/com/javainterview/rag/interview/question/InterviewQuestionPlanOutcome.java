package com.javainterview.rag.interview.question;

public record InterviewQuestionPlanOutcome(
        InterviewQuestionGenerationResult generation,
        String failureCode,
        String failureMessage
) {
}
