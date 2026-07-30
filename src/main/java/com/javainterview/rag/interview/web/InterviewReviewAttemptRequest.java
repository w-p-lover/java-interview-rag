package com.javainterview.rag.interview.web;

public record InterviewReviewAttemptRequest(
        String answer,
        int expectedAttemptNumber
) {
}
