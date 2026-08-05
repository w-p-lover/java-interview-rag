package com.javainterview.rag.interview.tool;

import java.time.LocalDateTime;
import java.util.Objects;

public record InterviewToolExecutionLog(
        Long id,
        String executionId,
        String toolName,
        String sessionId,
        Status status,
        Integer resultCount,
        long elapsedMillis,
        String errorType,
        LocalDateTime createdAt
) {

    public InterviewToolExecutionLog {
        executionId = requireText(executionId, "工具执行标识不能为空");
        toolName = requireText(toolName, "工具名称不能为空");
        sessionId = requireText(sessionId, "面试会话 ID 不能为空");
        Objects.requireNonNull(status, "工具执行状态不能为空");
        Objects.requireNonNull(createdAt, "工具执行时间不能为空");
        if (elapsedMillis < 0) {
            throw new IllegalArgumentException("工具执行耗时不能为负数");
        }
        errorType = optionalText(errorType);
        if (status == Status.SUCCEEDED) {
            if (resultCount == null || resultCount < 0) {
                throw new IllegalArgumentException("成功执行必须记录非负结果数量");
            }
            if (errorType != null) {
                throw new IllegalArgumentException("成功执行不能记录失败类型");
            }
        } else {
            if (resultCount != null) {
                throw new IllegalArgumentException("失败执行不能记录结果数量");
            }
            if (errorType == null) {
                throw new IllegalArgumentException("失败执行必须记录失败类型");
            }
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }

    private static String optionalText(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    public enum Status {
        SUCCEEDED,
        FAILED
    }
}
