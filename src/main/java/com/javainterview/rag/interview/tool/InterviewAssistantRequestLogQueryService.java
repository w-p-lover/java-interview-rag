package com.javainterview.rag.interview.tool;

import java.util.List;
import java.util.Objects;

public class InterviewAssistantRequestLogQueryService {

    private static final int MAX_LIMIT = 100;

    private final InterviewAssistantRequestLogStore logStore;

    public InterviewAssistantRequestLogQueryService(InterviewAssistantRequestLogStore logStore) {
        this.logStore = Objects.requireNonNull(logStore, "复习助手请求日志存储不能为空");
    }

    public List<InterviewAssistantRequestLog> findRecent(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("助手请求日志查询数量必须在 1 到 100 之间");
        }
        return logStore.findRecent(limit);
    }
}
