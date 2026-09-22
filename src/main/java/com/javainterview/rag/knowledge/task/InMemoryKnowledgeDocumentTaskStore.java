package com.javainterview.rag.knowledge.task;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;

public final class InMemoryKnowledgeDocumentTaskStore implements KnowledgeDocumentTaskStore {

    private final Map<String, KnowledgeDocumentTask> tasks = new LinkedHashMap<>();

    @Override
    public synchronized KnowledgeDocumentTask create(String taskId, String attemptId, String source,
                                                      KnowledgeDocumentTaskOperation operation) {
        Objects.requireNonNull(taskId, "任务 ID 不能为空");
        if (tasks.containsKey(taskId)) {
            return tasks.get(taskId);
        }
        LocalDateTime now = LocalDateTime.now();
        KnowledgeDocumentTask task = new KnowledgeDocumentTask(taskId, attemptId, source.strip(), operation,
                KnowledgeDocumentTaskStatus.QUEUED, null, false, 0, null, 0, null, now, now, null);
        tasks.put(taskId, task);
        return task;
    }

    @Override
    public synchronized Optional<KnowledgeDocumentTask> find(String taskId) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    @Override
    public synchronized boolean markRunning(String taskId, String attemptId) {
        KnowledgeDocumentTask existing = requiredTask(taskId);
        if (existing.status() != KnowledgeDocumentTaskStatus.QUEUED
                || !existing.attemptId().equals(attemptId)) {
            return false;
        }
        return update(taskId, attemptId, task -> copy(task,
                KnowledgeDocumentTaskStatus.RUNNING, task.version(), task.imported(), task.chunkCount(),
                task.format(), task.warningCount(), task.errorType(), null));
    }

    @Override
    public synchronized boolean markSucceeded(String taskId, String attemptId, String version,
                                               boolean imported, int chunkCount,
                                               String format, int warningCount) {
        KnowledgeDocumentTask existing = requiredTask(taskId);
        if (existing.status() != KnowledgeDocumentTaskStatus.RUNNING
                || !existing.attemptId().equals(attemptId)) {
            return false;
        }
        return update(taskId, attemptId, task -> copy(task,
                KnowledgeDocumentTaskStatus.SUCCEEDED, version, imported, chunkCount, format,
                warningCount, null, LocalDateTime.now()));
    }

    @Override
    public synchronized boolean markFailed(String taskId, String attemptId, String errorType) {
        return update(taskId, attemptId, task -> copy(task,
                KnowledgeDocumentTaskStatus.FAILED, task.version(), task.imported(), task.chunkCount(),
                task.format(), task.warningCount(), Objects.requireNonNull(errorType, "错误类型不能为空"),
                LocalDateTime.now()));
    }

    @Override
    public synchronized boolean cancelQueued(String taskId) {
        KnowledgeDocumentTask existing = tasks.get(taskId);
        if (existing == null) {
            throw new NoSuchElementException("知识资料任务不存在");
        }
        if (existing.status() != KnowledgeDocumentTaskStatus.QUEUED) {
            return false;
        }
        tasks.put(taskId, copy(existing, KnowledgeDocumentTaskStatus.CANCELLED, existing.version(),
                existing.imported(), existing.chunkCount(), existing.format(), existing.warningCount(),
                "CANCELLED_BY_USER", LocalDateTime.now()));
        return true;
    }

    @Override
    public synchronized List<KnowledgeDocumentTask> findRecent(int limit) {
        java.util.ArrayList<KnowledgeDocumentTask> recent = new java.util.ArrayList<>(tasks.values());
        java.util.Collections.reverse(recent);
        return recent.stream().limit(limit).collect(Collectors.toList());
    }

    private boolean update(String taskId, String attemptId,
                           java.util.function.UnaryOperator<KnowledgeDocumentTask> updater) {
        KnowledgeDocumentTask existing = requiredTask(taskId);
        if (existing.terminal() || !existing.attemptId().equals(attemptId)) {
            return false;
        }
        tasks.put(taskId, updater.apply(existing));
        return true;
    }

    private KnowledgeDocumentTask requiredTask(String taskId) {
        KnowledgeDocumentTask existing = tasks.get(taskId);
        if (existing == null) {
            throw new NoSuchElementException("知识资料任务不存在");
        }
        return existing;
    }

    private KnowledgeDocumentTask copy(KnowledgeDocumentTask task,
                                       KnowledgeDocumentTaskStatus status,
                                       String version, boolean imported, int chunkCount, String format,
                                       int warningCount, String errorType,
                                       LocalDateTime completedAt) {
        LocalDateTime now = LocalDateTime.now();
        return new KnowledgeDocumentTask(task.taskId(), task.attemptId(), task.source(), task.operation(), status,
                version, imported, chunkCount, format, warningCount, errorType,
                task.createdAt(), now, completedAt);
    }
}
