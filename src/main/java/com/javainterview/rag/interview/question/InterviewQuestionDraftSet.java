package com.javainterview.rag.interview.question;

import java.util.List;
import java.util.Objects;

public record InterviewQuestionDraftSet(List<InterviewQuestionDraft> questions) {

    private static final int QUESTION_COUNT = 3;

    public InterviewQuestionDraftSet {
        questions = List.copyOf(Objects.requireNonNull(questions, "面试题草稿不能为空"));
        if (questions.size() != QUESTION_COUNT) {
            throw new IllegalArgumentException("模型必须返回 3 道面试题");
        }
    }
}
