package com.javainterview.rag.interview.matching;

import java.time.LocalDateTime;
import java.util.Objects;

public record ResumeJobMatchRequestLog(
        Long id,
        String requestId,
        String roleName,
        String promptVersion,
        String modelName,
        Status status,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        long elapsedMillis,
        String errorType,
        LocalDateTime createdAt
) {

    public ResumeJobMatchRequestLog {
        requestId = requireText(requestId, "岗位匹配请求标识不能为空");
        roleName = requireText(roleName, "岗位匹配角色不能为空");
        promptVersion = requireText(promptVersion, "岗位匹配 Prompt 版本不能为空");
        modelName = requireText(modelName, "岗位匹配模型名称不能为空");
        Objects.requireNonNull(status, "岗位匹配请求状态不能为空");
        Objects.requireNonNull(createdAt, "岗位匹配请求时间不能为空");
        if (elapsedMillis < 0) {
            throw new IllegalArgumentException("岗位匹配请求耗时不能为负数");
        }
        boolean allNull = promptTokens == null && completionTokens == null && totalTokens == null;
        boolean allPresent = promptTokens != null && completionTokens != null && totalTokens != null;
        if (!allNull && !allPresent) {
            throw new IllegalArgumentException("Token 用量必须同时存在或同时为空");
        }
        if (allPresent && (promptTokens < 0 || completionTokens < 0 || totalTokens < 0)) {
            throw new IllegalArgumentException("Token 用量不能为负数");
        }
        errorType = errorType == null || errorType.isBlank() ? null : errorType.strip();
        if (status == Status.SUCCEEDED && errorType != null) {
            throw new IllegalArgumentException("成功匹配不能记录失败类型");
        }
        if (status == Status.FAILED && errorType == null) {
            throw new IllegalArgumentException("失败匹配必须记录失败类型");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }

    public enum Status {
        SUCCEEDED,
        FAILED
    }
}
