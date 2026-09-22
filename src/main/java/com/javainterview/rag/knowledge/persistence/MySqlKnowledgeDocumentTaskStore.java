package com.javainterview.rag.knowledge.persistence;

import com.javainterview.rag.knowledge.task.KnowledgeDocumentTask;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskOperation;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskStatus;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskStore;

import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

public final class MySqlKnowledgeDocumentTaskStore implements KnowledgeDocumentTaskStore {

    private final KnowledgeDocumentTaskMapper mapper;

    public MySqlKnowledgeDocumentTaskStore(KnowledgeDocumentTaskMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public KnowledgeDocumentTask create(String taskId, String attemptId, String source,
                                        KnowledgeDocumentTaskOperation operation) {
        mapper.insert(taskId, attemptId, source.strip(), operation.name(), KnowledgeDocumentTaskStatus.QUEUED.name());
        return find(taskId).orElseThrow(() -> new IllegalStateException("资料任务创建失败"));
    }

    @Override
    public Optional<KnowledgeDocumentTask> find(String taskId) {
        KnowledgeDocumentTaskEntity entity = mapper.find(taskId);
        return entity == null ? Optional.empty() : Optional.of(toTask(entity));
    }

    @Override
    public boolean markRunning(String taskId, String attemptId) {
        return mapper.markRunning(taskId, attemptId, KnowledgeDocumentTaskStatus.RUNNING.name()) == 1;
    }

    @Override
    public boolean markSucceeded(String taskId, String attemptId, String version, boolean imported,
                                 int chunkCount, String format, int warningCount) {
        return mapper.markSucceeded(taskId, attemptId, KnowledgeDocumentTaskStatus.SUCCEEDED.name(),
                version, imported, chunkCount, format, warningCount, null) == 1;
    }

    @Override
    public boolean markFailed(String taskId, String attemptId, String errorType) {
        return mapper.markFailed(taskId, attemptId, errorType) == 1;
    }

    @Override
    public boolean cancelQueued(String taskId) {
        return mapper.cancelQueued(taskId) > 0;
    }

    @Override
    public List<KnowledgeDocumentTask> findRecent(int limit) {
        return mapper.findRecent(limit).stream().map(this::toTask).collect(Collectors.toList());
    }

    public int failRunningAfterRestart() {
        return mapper.failRunningAfterRestart();
    }

    private KnowledgeDocumentTask toTask(KnowledgeDocumentTaskEntity entity) {
        return new KnowledgeDocumentTask(entity.getTaskId(), entity.getAttemptId(), entity.getSource(),
                KnowledgeDocumentTaskOperation.valueOf(entity.getOperation()),
                KnowledgeDocumentTaskStatus.valueOf(entity.getStatus()), entity.getVersion(),
                Boolean.TRUE.equals(entity.getImported()), entity.getChunkCount() == null ? 0 : entity.getChunkCount(), entity.getFormat(),
                entity.getWarningCount() == null ? 0 : entity.getWarningCount(), entity.getErrorType(),
                entity.getCreatedAt(), entity.getUpdatedAt(), entity.getCompletedAt());
    }
}
