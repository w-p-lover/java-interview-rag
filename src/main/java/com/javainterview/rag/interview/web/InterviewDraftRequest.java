package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.question.InterviewDifficulty;
import com.javainterview.rag.interview.question.InterviewSkill;

public record InterviewDraftRequest(
        String targetRole,
        String resumeText,
        String jobDescription,
        Integer questionCount,
        Integer maxQuestionCount,
        InterviewSkill skill,
        InterviewDifficulty difficulty,
        Boolean regenerate
) {
}
