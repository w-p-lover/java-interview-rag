package com.javainterview.rag.interview.preparation;

import java.util.List;
import java.util.Objects;

public record InterviewFocusArea(String topic, String reason, List<InterviewFocusSource> sources) {

    public InterviewFocusArea {
        topic = requireText(topic, "面试关注主题不能为空");
        reason = requireText(reason, "面试关注原因不能为空");
        sources = List.copyOf(Objects.requireNonNull(sources, "面试关注依据不能为空"));
        if (sources.isEmpty()) {
            throw new IllegalArgumentException("面试关注依据不能为空");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
