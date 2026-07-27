package com.javainterview.rag.interview.web;

public record InterviewAnswerSubmissionRequest(
        int questionIndex,
        long expectedVersion,
        String answer
) {
}
