package com.javainterview.rag.interview.persistence;

import com.javainterview.rag.interview.tool.InterviewToolExecutionLog;
import com.javainterview.rag.interview.tool.InterviewToolExecutionLogStore;

import java.util.List;
import java.util.Objects;

public class MySqlInterviewToolExecutionLogStore implements InterviewToolExecutionLogStore {

    private final InterviewToolExecutionLogMapper mapper;

    public MySqlInterviewToolExecutionLogStore(InterviewToolExecutionLogMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "面试工具执行日志 Mapper 不能为空");
    }

    @Override
    public void record(InterviewToolExecutionLog log) {
        mapper.insert(InterviewToolExecutionLogEntity.from(log));
    }

    @Override
    public List<InterviewToolExecutionLog> findRecent(String sessionId, int limit) {
        return mapper.findRecent(sessionId, limit).stream()
                .map(this::toLog)
                .toList();
    }

    private InterviewToolExecutionLog toLog(InterviewToolExecutionLogEntity entity) {
        return new InterviewToolExecutionLog(
                entity.getId(),
                entity.getExecutionId(),
                entity.getAssistantRequestId(),
                entity.getToolName(),
                entity.getSessionId(),
                InterviewToolExecutionLog.Status.valueOf(entity.getStatus()),
                entity.getResultCount(),
                entity.getElapsedMillis(),
                entity.getErrorType(),
                entity.getCreatedAt());
    }
}
