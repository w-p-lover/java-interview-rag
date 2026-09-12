package com.javainterview.rag.knowledge.task;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

public final class InMemoryKnowledgeDocumentTaskStore implements KnowledgeDocumentTaskStore {

    private final Map<String, KnowledgeDocumentTask> tasks = new LinkedHashMap<>();

    @Override
    public synchronized KnowledgeDocumentTask create(String taskId, String source,
                                                      KnowledgeDocumentTaskOperation operation) {
        Objects.requireNonNull(taskId, "任务 ID 不能为空");
        if (tasks.containsKey(taskId)) {
            return tasks.get(taskId);
        }
        LocalDateTime now = LocalDateTime.now();
        KnowledgeDocumentTask task = new KnowledgeDocumentTask(taskId, source.strip(), operation,
                KnowledgeDocumentTaskStatus.QUEUED, null, false, 0, null, 0, null, now, now, null);
        tasks.put(taskId, task);
        return task;
    }

    @Override
    public synchronized Optional<KnowledgeDocumentTask> find(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    @Override
    public synchronized void markRunning(String taskId) {
        update(taskId, task -> task.terminal() ? task : copy(task,
                KnowledgeDocumentTaskStatus.RUNNING, task.version(), task.imported(), task.chunkCount(),
                task.format(), task.warningCount(), task.errorType(), null));
    }

    @Override
    public synchronized void markSucceeded(String taskId, String version, boolean imported, int chunkCount,
                                            String format, int warningCount) {
        update(taskId, task -> task.terminal() ? task : copy(task,
                KnowledgeDocumentTaskStatus.SUCCEEDED, version, imported, chunkCount, format,
                warningCount, null, LocalDateTime.now()));
    }

    @Override
    public synchronized void markFailed(String taskId, String errorType) {
        update(taskId, task -> task.terminal() ? task : copy(task,
                KnowledgeDocumentTaskStatus.FAILED, task.version(), task.imported(), task.chunkCount(),
                task.format(), task.warningCount(), Objects.requireNonNull(errorType, "错误类型不能为空"),
                LocalDateTime.now()));
    }

    private void update(String taskId, java.util.function.UnaryOperator<KnowledgeDocumentTask> updater) {
        KnowledgeDocumentTask existing = tasks.get(taskId);
        if (existing == null) {
            throw new NoSuchElementException("知识资料任务不存在");
        }
        tasks.put(taskId, updater.apply(existing));
    }

    private KnowledgeDocumentTask copy(KnowledgeDocumentTask task,
                                       KnowledgeDocumentTaskStatus status,
                                       String version, boolean imported, int chunkCount, String format,
                                       int warningCount, String errorType,
                                       LocalDateTime completedAt) {
        LocalDateTime now = LocalDateTime.now();
        return new KnowledgeDocumentTask(task.taskId(), task.source(), task.operation(), status,
                version, imported, chunkCount, format, warningCount, errorType,
                task.createdAt(), now, completedAt);
    }
}
