package com.javainterview.rag.interview.tool;

import java.time.LocalDate;

public record InterviewReviewScheduleConfirmation(
        String sessionId,
        String taskId,
        LocalDate plannedReviewDate
) {

    public InterviewReviewScheduleConfirmation {
        sessionId = requireText(sessionId, "人工确认的面试会话 ID 不能为空");
        taskId = requireText(taskId, "人工确认的复习任务 ID 不能为空");
        if (plannedReviewDate == null) {
            throw new IllegalArgumentException("人工确认的计划复习日期不能为空");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }
}
