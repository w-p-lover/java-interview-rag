package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.tool.InterviewReviewScheduleConfirmation;

public record InterviewReviewAssistantRequest(
        String message,
        InterviewReviewScheduleConfirmation scheduleConfirmation
) {
}
