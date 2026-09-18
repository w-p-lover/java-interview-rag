package com.javainterview.rag.interview.question;

import java.util.List;
import java.util.Objects;

public record InterviewQuestionAllocation(
        int questionIndex,
        InterviewQuestionSource primarySource,
        List<InterviewQuestionSource> supportingSources
) {

    public InterviewQuestionAllocation {
        if (questionIndex < 0) {
            throw new IllegalArgumentException("题目序号不能小于 0");
        }
        primarySource = Objects.requireNonNull(primarySource, "题目主来源不能为空");
        supportingSources = List.copyOf(Objects.requireNonNull(
                supportingSources, "题目辅助来源不能为空"));
    }
}
