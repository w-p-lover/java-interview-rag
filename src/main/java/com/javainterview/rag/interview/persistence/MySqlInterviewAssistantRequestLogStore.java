package com.javainterview.rag.interview.persistence;

import com.javainterview.rag.interview.tool.InterviewAssistantRequestLog;
import com.javainterview.rag.interview.tool.InterviewAssistantRequestLogStore;

import java.util.List;
import java.util.Objects;

public class MySqlInterviewAssistantRequestLogStore implements InterviewAssistantRequestLogStore {

    private final InterviewAssistantRequestLogMapper mapper;

    public MySqlInterviewAssistantRequestLogStore(InterviewAssistantRequestLogMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "复习助手请求日志 Mapper 不能为空");
    }

    @Override
    public void record(InterviewAssistantRequestLog log) {
        mapper.insert(InterviewAssistantRequestLogEntity.from(log));
    }

    @Override
    public List<InterviewAssistantRequestLog> findRecent(int limit) {
        return mapper.findRecent(limit).stream()
                .map(this::toLog)
                .toList();
    }

    private InterviewAssistantRequestLog toLog(InterviewAssistantRequestLogEntity entity) {
        return new InterviewAssistantRequestLog(
                entity.getId(),
                entity.getRequestId(),
                entity.getModelName(),
                InterviewAssistantRequestLog.Status.valueOf(entity.getStatus()),
                entity.getToolExecutionCount(),
                entity.getPromptTokens(),
                entity.getCompletionTokens(),
                entity.getTotalTokens(),
                entity.getElapsedMillis(),
                entity.getFinalResponsePresent(),
                entity.getErrorType(),
                entity.getCreatedAt());
    }
}
