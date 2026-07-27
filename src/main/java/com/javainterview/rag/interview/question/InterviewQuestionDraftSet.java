package com.javainterview.rag.interview.question;

import java.util.List;
import java.util.Objects;

public record InterviewQuestionDraftSet(List<InterviewQuestionDraft> questions) {

    private static final int MAX_QUESTION_COUNT = 8;

    public InterviewQuestionDraftSet {
        questions = List.copyOf(Objects.requireNonNull(questions, "面试题草稿不能为空"));
        if (questions.isEmpty() || questions.size() > MAX_QUESTION_COUNT) {
            throw new IllegalArgumentException("模型返回的面试题数量必须在 1 到 8 之间");
        }
    }
}
