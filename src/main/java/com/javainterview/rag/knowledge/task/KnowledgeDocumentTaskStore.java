package com.javainterview.rag.knowledge.task;

import java.util.Optional;
import java.util.List;

public interface KnowledgeDocumentTaskStore {

    KnowledgeDocumentTask create(String taskId, String attemptId, String source,
                                 String fileId, KnowledgeDocumentTaskOperation operation);

    default KnowledgeDocumentTask create(String taskId, String attemptId, String source,
                                         KnowledgeDocumentTaskOperation operation) {
        return create(taskId, attemptId, source, null, operation);
    }

    Optional<KnowledgeDocumentTask> find(String taskId);

    boolean markRunning(String taskId, String attemptId);

    boolean markSucceeded(String taskId, String attemptId, String version, boolean imported,
                          int chunkCount, String format, int warningCount);

    boolean markFailed(String taskId, String attemptId, String errorType);

    boolean cancelQueued(String taskId);

    List<KnowledgeDocumentTask> findRecent(int limit);

    List<KnowledgeDocumentTask> findRecoverable(int limit);

    boolean requeueAfterRestart(String taskId, String oldAttemptId,
                                String newAttemptId, int maxRecoveryCount);
}
