package com.javainterview.rag.interview.session;

import java.time.LocalDateTime;
import java.util.Objects;

public record InterviewSessionSummary(
        String id,
        String targetRole,
        String resumeVersionId,
        InterviewSessionMode mode,
        InterviewSessionStatus status,
        int answeredQuestionCount,
        int questionCount,
        Double overallAverageScore,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public InterviewSessionSummary {
        id = requireText(id, "面试会话 ID 不能为空");
        targetRole = requireText(targetRole, "目标岗位不能为空");
        mode = Objects.requireNonNull(mode, "面试会话模式不能为空");
        status = Objects.requireNonNull(status, "面试会话状态不能为空");
        if (answeredQuestionCount < 0 || questionCount < answeredQuestionCount) {
            throw new IllegalArgumentException("面试历史题量不合法");
        }
        createdAt = Objects.requireNonNull(createdAt, "面试会话创建时间不能为空");
        updatedAt = Objects.requireNonNull(updatedAt, "面试会话更新时间不能为空");
    }

    public static InterviewSessionSummary from(InterviewSession session) {
        Objects.requireNonNull(session, "面试会话不能为空");
        return new InterviewSessionSummary(
                session.id(), session.targetRole(), session.resumeVersionId(), session.mode(),
                session.status(), session.answers().size(), session.questions().size(),
                session.report() == null ? null : session.report().overallAverageScore(),
                session.createdAt(), session.updatedAt());
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }
}
