package com.javainterview.rag.interview.web;

public record InterviewPreparationRequest(
        String targetRole,
        String resumeText,
        String jobDescription
) {
}
