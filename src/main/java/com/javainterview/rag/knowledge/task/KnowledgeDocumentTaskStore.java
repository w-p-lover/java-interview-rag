package com.javainterview.rag.knowledge.task;

import java.util.Optional;

public interface KnowledgeDocumentTaskStore {

    KnowledgeDocumentTask create(String taskId, String source,
                                 KnowledgeDocumentTaskOperation operation);

    Optional<KnowledgeDocumentTask> find(String taskId);

    void markRunning(String taskId);

    void markSucceeded(String taskId, String version, boolean imported, int chunkCount,
                       String format, int warningCount);

    void markFailed(String taskId, String errorType);
}
