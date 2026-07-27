package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;

public record InterviewQuestionGenerationRequest(
        String targetRole,
        InterviewPreparationProfile profile
) {
}
