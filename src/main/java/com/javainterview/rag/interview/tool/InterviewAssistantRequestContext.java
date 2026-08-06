package com.javainterview.rag.interview.tool;

import org.springframework.ai.chat.model.ToolContext;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

final class InterviewAssistantRequestContext {

    static final String KEY = InterviewAssistantRequestContext.class.getName();

    private final String requestId;
    private final InterviewReviewScheduleConfirmation scheduleConfirmation;
    private final AtomicInteger toolExecutionCount = new AtomicInteger();

    private InterviewAssistantRequestContext(
            String requestId,
            InterviewReviewScheduleConfirmation scheduleConfirmation) {
        this.requestId = requestId;
        this.scheduleConfirmation = scheduleConfirmation;
    }

    static InterviewAssistantRequestContext create() {
        return create(null);
    }

    static InterviewAssistantRequestContext create(
            InterviewReviewScheduleConfirmation scheduleConfirmation) {
        return new InterviewAssistantRequestContext(
                UUID.randomUUID().toString(), scheduleConfirmation);
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

    void requireScheduleConfirmation(String sessionId,
                                     String taskId,
                                     LocalDate plannedReviewDate) {
        if (scheduleConfirmation == null) {
            throw new IllegalStateException("设置计划复习日期前必须提供人工确认");
        }
        if (!scheduleConfirmation.sessionId().equals(sessionId)
                || !scheduleConfirmation.taskId().equals(taskId)
                || !scheduleConfirmation.plannedReviewDate().equals(plannedReviewDate)) {
            throw new IllegalStateException("工具参数与人工确认的改期内容不一致");
        }
    }
}
