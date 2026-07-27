package com.javainterview.rag.interview.web;

public record InterviewSessionStartRequest(
        String targetRole,
        String resumeText,
        String jobDescription,
        Integer initialQuestionCount,
        Integer maxQuestionCount
) {
}
