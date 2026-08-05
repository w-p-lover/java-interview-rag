package com.javainterview.rag.interview.tool;

import java.time.LocalDateTime;
import java.util.Objects;

public record InterviewAssistantRequestLog(
        Long id,
        String requestId,
        String modelName,
        Status status,
        int toolExecutionCount,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        long elapsedMillis,
        boolean finalResponsePresent,
        String errorType,
        LocalDateTime createdAt
) {

    public InterviewAssistantRequestLog {
        requestId = requireText(requestId, "助手请求标识不能为空");
        modelName = requireText(modelName, "助手模型名称不能为空");
        Objects.requireNonNull(status, "助手请求状态不能为空");
        Objects.requireNonNull(createdAt, "助手请求时间不能为空");
        if (toolExecutionCount < 0) {
            throw new IllegalArgumentException("工具执行数量不能为负数");
        }
        if (elapsedMillis < 0) {
            throw new IllegalArgumentException("助手请求耗时不能为负数");
        }
        validateUsage(promptTokens, completionTokens, totalTokens);
        errorType = optionalText(errorType);
        if (status == Status.SUCCEEDED) {
            if (!finalResponsePresent) {
                throw new IllegalArgumentException("成功请求必须存在最终响应");
            }
            if (errorType != null) {
                throw new IllegalArgumentException("成功请求不能记录失败类型");
            }
        } else {
            if (finalResponsePresent) {
                throw new IllegalArgumentException("失败请求不能标记为存在最终响应");
            }
            if (errorType == null) {
                throw new IllegalArgumentException("失败请求必须记录失败类型");
            }
        }
    }

    private static void validateUsage(Integer promptTokens,
                                      Integer completionTokens,
                                      Integer totalTokens) {
        boolean allNull = promptTokens == null && completionTokens == null && totalTokens == null;
        boolean allPresent = promptTokens != null && completionTokens != null && totalTokens != null;
        if (!allNull && !allPresent) {
            throw new IllegalArgumentException("Token 用量必须同时存在或同时为空");
        }
        if (allPresent && (promptTokens < 0 || completionTokens < 0 || totalTokens < 0)) {
            throw new IllegalArgumentException("Token 用量不能为负数");
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
