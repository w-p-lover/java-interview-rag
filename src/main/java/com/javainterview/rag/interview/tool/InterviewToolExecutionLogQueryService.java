package com.javainterview.rag.interview.tool;

import java.util.List;
import java.util.Objects;

public class InterviewToolExecutionLogQueryService {

    private static final int MAX_LIMIT = 100;

    private final InterviewToolExecutionLogStore logStore;

    public InterviewToolExecutionLogQueryService(InterviewToolExecutionLogStore logStore) {
        this.logStore = Objects.requireNonNull(logStore, "面试工具执行日志存储不能为空");
    }

    public List<InterviewToolExecutionLog> findRecent(String sessionId, int limit) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("面试会话 ID 不能为空");
        }
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("工具执行日志查询数量必须在 1 到 100 之间");
        }
        return logStore.findRecent(sessionId.strip(), limit);
    }
}
