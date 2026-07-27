package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;

import java.util.List;

public record InterviewSessionCreateRequest(
        String targetRole,
        InterviewPreparationProfile profile,
        List<InterviewQuestion> questions
) {
}
