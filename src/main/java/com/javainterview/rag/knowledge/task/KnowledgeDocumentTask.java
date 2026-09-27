package com.javainterview.rag.knowledge.task;

import java.time.LocalDateTime;
import java.util.Objects;

public record KnowledgeDocumentTask(
        String taskId,
        String attemptId,
        String source,
        String fileId,
        int recoveryCount,
        KnowledgeDocumentTaskOperation operation,
        KnowledgeDocumentTaskStatus status,
        String version,
        boolean imported,
        int chunkCount,
        String format,
        int warningCount,
        String errorType,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime completedAt
) {

    public KnowledgeDocumentTask {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("任务 ID 不能为空");
        }
        if (attemptId == null || attemptId.isBlank()) {
            throw new IllegalArgumentException("任务执行 ID 不能为空");
        }
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("文档来源不能为空");
        }
        if (recoveryCount < 0) {
            throw new IllegalArgumentException("任务恢复次数不能小于 0");
        }
        operation = Objects.requireNonNull(operation, "任务操作不能为空");
        status = Objects.requireNonNull(status, "任务状态不能为空");
        if (chunkCount < 0 || warningCount < 0) {
            throw new IllegalArgumentException("任务统计数量不能小于 0");
        }
        createdAt = Objects.requireNonNull(createdAt, "任务创建时间不能为空");
        updatedAt = Objects.requireNonNull(updatedAt, "任务更新时间不能为空");
    }

    public KnowledgeDocumentTask(String taskId, String attemptId, String source,
                                 KnowledgeDocumentTaskOperation operation,
                                 KnowledgeDocumentTaskStatus status, String version,
                                 boolean imported, int chunkCount, String format,
                                 int warningCount, String errorType, LocalDateTime createdAt,
                                 LocalDateTime updatedAt, LocalDateTime completedAt) {
        this(taskId, attemptId, source, null, 0, operation, status, version, imported,
                chunkCount, format, warningCount, errorType, createdAt, updatedAt, completedAt);
    }

    public boolean terminal() {
        return status == KnowledgeDocumentTaskStatus.SUCCEEDED
                || status == KnowledgeDocumentTaskStatus.FAILED
                || status == KnowledgeDocumentTaskStatus.CANCELLED;
    }
}
