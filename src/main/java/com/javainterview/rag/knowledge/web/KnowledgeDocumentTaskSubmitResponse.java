package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskOperation;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskStatus;

public record KnowledgeDocumentTaskSubmitResponse(
        String taskId,
        String source,
        KnowledgeDocumentTaskOperation operation,
        KnowledgeDocumentTaskStatus status
) {
}
