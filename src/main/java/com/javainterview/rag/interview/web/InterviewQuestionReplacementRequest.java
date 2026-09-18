package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.question.InterviewQuestionGenerationResult;

public record InterviewQuestionReplacementRequest(
        InterviewQuestionGenerationResult generation
) {
}
