package com.javainterview.rag.knowledge.task;

import java.time.LocalDateTime;
import java.util.Objects;

public record KnowledgeDocumentTask(
        String taskId,
        String source,
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
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("文档来源不能为空");
        }
        operation = Objects.requireNonNull(operation, "任务操作不能为空");
        status = Objects.requireNonNull(status, "任务状态不能为空");
        if (chunkCount < 0 || warningCount < 0) {
            throw new IllegalArgumentException("任务统计数量不能小于 0");
        }
        createdAt = Objects.requireNonNull(createdAt, "任务创建时间不能为空");
        updatedAt = Objects.requireNonNull(updatedAt, "任务更新时间不能为空");
    }

    public boolean terminal() {
        return status == KnowledgeDocumentTaskStatus.SUCCEEDED
                || status == KnowledgeDocumentTaskStatus.FAILED;
    }
}
