package com.javainterview.rag.knowledge.persistence;

import com.javainterview.rag.knowledge.task.KnowledgeDocumentTask;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskOperation;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskStatus;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskStore;

import java.util.Optional;

public final class MySqlKnowledgeDocumentTaskStore implements KnowledgeDocumentTaskStore {

    private final KnowledgeDocumentTaskMapper mapper;

    public MySqlKnowledgeDocumentTaskStore(KnowledgeDocumentTaskMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public KnowledgeDocumentTask create(String taskId, String source,
                                        KnowledgeDocumentTaskOperation operation) {
        mapper.insert(taskId, source.strip(), operation.name(), KnowledgeDocumentTaskStatus.QUEUED.name());
        return find(taskId).orElseThrow(() -> new IllegalStateException("资料任务创建失败"));
    }

    @Override
    public Optional<KnowledgeDocumentTask> find(String taskId) {
        KnowledgeDocumentTaskEntity entity = mapper.find(taskId);
        return entity == null ? Optional.empty() : Optional.of(toTask(entity));
    }

    @Override
    public void markRunning(String taskId) {
        mapper.markRunning(taskId, KnowledgeDocumentTaskStatus.RUNNING.name());
    }

    @Override
    public void markSucceeded(String taskId, String version, boolean imported, int chunkCount,
                              String format, int warningCount) {
        mapper.markSucceeded(taskId, KnowledgeDocumentTaskStatus.SUCCEEDED.name(), version,
                imported, chunkCount, format, warningCount, null);
    }

    @Override
    public void markFailed(String taskId, String errorType) {
        mapper.markFailed(taskId, errorType);
    }

    public int failRunningAfterRestart() {
        return mapper.failRunningAfterRestart();
    }

    private KnowledgeDocumentTask toTask(KnowledgeDocumentTaskEntity entity) {
        return new KnowledgeDocumentTask(entity.getTaskId(), entity.getSource(),
                KnowledgeDocumentTaskOperation.valueOf(entity.getOperation()),
                KnowledgeDocumentTaskStatus.valueOf(entity.getStatus()), entity.getVersion(),
                Boolean.TRUE.equals(entity.getImported()), entity.getChunkCount() == null ? 0 : entity.getChunkCount(), entity.getFormat(),
                entity.getWarningCount() == null ? 0 : entity.getWarningCount(), entity.getErrorType(),
                entity.getCreatedAt(), entity.getUpdatedAt(), entity.getCompletedAt());
    }
}
