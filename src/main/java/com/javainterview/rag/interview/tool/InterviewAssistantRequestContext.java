package com.javainterview.rag.interview.tool;

import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

final class InterviewAssistantRequestContext {

    static final String KEY = InterviewAssistantRequestContext.class.getName();

    private final String requestId;
    private final AtomicInteger toolExecutionCount = new AtomicInteger();

    private InterviewAssistantRequestContext(String requestId) {
        this.requestId = requestId;
    }

    static InterviewAssistantRequestContext create() {
        return new InterviewAssistantRequestContext(UUID.randomUUID().toString());
    }

    static InterviewAssistantRequestContext from(ToolContext toolContext) {
        if (toolContext == null || toolContext.getContext() == null) {
            return null;
        }
        Object value = toolContext.getContext().get(KEY);
        return value instanceof InterviewAssistantRequestContext context ? context : null;
    }

    Map<String, Object> asMap() {
        return Map.of(KEY, this);
    }

    String markToolExecution() {
        toolExecutionCount.incrementAndGet();
        return requestId;
    }

    String requestId() {
        return requestId;
    }

    int toolExecutionCount() {
        return toolExecutionCount.get();
    }
}
