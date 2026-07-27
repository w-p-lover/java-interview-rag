package com.javainterview.rag.interview.question;

import com.javainterview.rag.interview.preparation.InterviewFocusSource;

import java.util.List;
import java.util.Objects;

public record InterviewQuestionDraft(
        String question,
        String objective,
        List<InterviewFocusSource> basisSources,
        String knowledgeQuery
) {

    public InterviewQuestionDraft {
        question = requireText(question, "面试题不能为空");
        objective = requireText(objective, "考察目标不能为空");
        basisSources = List.copyOf(Objects.requireNonNull(basisSources, "面试题依据不能为空"));
        if (basisSources.isEmpty()) {
            throw new IllegalArgumentException("面试题依据不能为空");
        }
        knowledgeQuery = requireText(knowledgeQuery, "知识检索查询不能为空");
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
