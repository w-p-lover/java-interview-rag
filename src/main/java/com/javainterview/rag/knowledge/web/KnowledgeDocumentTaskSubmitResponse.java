package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskOperation;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskStatus;

public record KnowledgeDocumentTaskSubmitResponse(
        String taskId,
        String attemptId,
        String source,
        String fileId,
        KnowledgeDocumentTaskOperation operation,
        KnowledgeDocumentTaskStatus status
) {
}
