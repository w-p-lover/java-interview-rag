package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.task.KnowledgeDocumentTask;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskOperation;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskStatus;

import java.time.LocalDateTime;

public record KnowledgeDocumentTaskResponse(
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

    public static KnowledgeDocumentTaskResponse from(KnowledgeDocumentTask task) {
        return new KnowledgeDocumentTaskResponse(task.taskId(), task.attemptId(), task.source(), task.fileId(),
                task.recoveryCount(), task.operation(),
                task.status(), task.version(), task.imported(), task.chunkCount(), task.format(), task.warningCount(),
                task.errorType(), task.createdAt(), task.updatedAt(), task.completedAt());
    }
}
