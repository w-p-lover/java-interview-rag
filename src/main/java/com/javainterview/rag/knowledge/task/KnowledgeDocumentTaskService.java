package com.javainterview.rag.knowledge.task;

import com.javainterview.rag.knowledge.document.MarkdownContentImporter;
import com.javainterview.rag.knowledge.document.MarkdownImportResult;
import com.javainterview.rag.knowledge.document.MarkdownRebuildResult;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Objects;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public final class KnowledgeDocumentTaskService {

    private final MarkdownContentImporter importer;
    private final KnowledgeDocumentTaskStore taskStore;
    private final ThreadPoolTaskExecutor executor;
    private final KnowledgeDocumentTaskMetrics metrics;
    private final ScheduledExecutorService timeoutScheduler;
    private final long timeoutMillis;
    private final ConcurrentHashMap<String, AtomicBoolean> completionGuards = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, FutureTask<Void>> submittedTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ScheduledFuture<?>> taskTimeouts = new ConcurrentHashMap<>();

    public KnowledgeDocumentTaskService(MarkdownContentImporter importer,
                                        KnowledgeDocumentTaskStore taskStore,
                                        ThreadPoolTaskExecutor executor,
                                        KnowledgeDocumentTaskMetrics metrics,
                                        ScheduledExecutorService timeoutScheduler,
                                        long timeoutMillis) {
        this.importer = Objects.requireNonNull(importer, "Markdown 导入器不能为空");
        this.taskStore = Objects.requireNonNull(taskStore, "资料任务存储不能为空");
        this.executor = Objects.requireNonNull(executor, "资料任务执行器不能为空");
        this.metrics = Objects.requireNonNull(metrics, "资料任务指标不能为空");
        this.timeoutScheduler = Objects.requireNonNull(timeoutScheduler, "资料任务超时调度器不能为空");
        if (timeoutMillis < 1_000) {
            throw new IllegalArgumentException("资料任务超时必须至少为 1000 毫秒");
        }
        this.timeoutMillis = timeoutMillis;
    }

    public KnowledgeDocumentTask submit(KnowledgeDocumentTaskOperation operation,
                                        KnowledgeDocumentTaskInput input) {
        Objects.requireNonNull(operation, "任务操作不能为空");
        Objects.requireNonNull(input, "任务输入不能为空");
        String taskId = UUID.randomUUID().toString();
        String attemptId = UUID.randomUUID().toString();
        KnowledgeDocumentTask task = taskStore.create(taskId, attemptId, input.source(), operation);
        completionGuards.put(taskId, new AtomicBoolean());
        metrics.submitted();
        FutureTask<Void> work = new FutureTask<>(() -> {
            execute(taskId, attemptId, operation, input);
            return null;
        });
        submittedTasks.put(taskId, work);
        try {
            ScheduledFuture<?> timeout = timeoutScheduler.schedule(
                    () -> timeout(taskId, attemptId, work), timeoutMillis, TimeUnit.MILLISECONDS);
            taskTimeouts.put(taskId, timeout);
            executor.execute(work);
        } catch (TaskRejectedException exception) {
            failSubmission(taskId, attemptId, work, "TASK_REJECTED");
            throw exception;
        } catch (RuntimeException exception) {
            failSubmission(taskId, attemptId, work, exception.getClass().getSimpleName());
            throw exception;
        }
        return task;
    }

    public KnowledgeDocumentTask find(String taskId) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("任务 ID 不能为空");
        }
        return taskStore.find(taskId.strip())
                .orElseThrow(() -> new java.util.NoSuchElementException("知识资料任务不存在"));
    }

    public KnowledgeDocumentTask cancel(String taskId) {
        String normalizedTaskId = taskId == null ? null : taskId.strip();
        if (normalizedTaskId == null || normalizedTaskId.isBlank()) {
            throw new IllegalArgumentException("任务 ID 不能为空");
        }
        if (!taskStore.cancelQueued(normalizedTaskId)) {
            KnowledgeDocumentTask task = find(normalizedTaskId);
            if (task.status() == KnowledgeDocumentTaskStatus.CANCELLED) {
                return task;
            }
            throw new KnowledgeDocumentTaskCancellationException("任务已开始执行，不能取消");
        }
        metrics.cancelled();
        cancelPendingExecution(normalizedTaskId);
        return find(normalizedTaskId);
    }

    public List<KnowledgeDocumentTask> findRecent(int limit) {
        if (limit < 1 || limit > 20) {
            throw new IllegalArgumentException("任务历史数量必须在 1 到 20 之间");
        }
        return taskStore.findRecent(limit);
    }

    private void execute(String taskId, String attemptId, KnowledgeDocumentTaskOperation operation,
                         KnowledgeDocumentTaskInput input) {
        if (!taskStore.markRunning(taskId, attemptId)) {
            return;
        }
        metrics.started();
        try {
            if (operation == KnowledgeDocumentTaskOperation.IMPORT) {
                MarkdownImportResult result = input.structured()
                        ? importer.importStructuredContent(input.source(), input.content(), input.sections(), attemptId)
                        : importer.importContent(input.source(), input.content(), attemptId);
                if (complete(taskId)) {
                    if (taskStore.markSucceeded(taskId, attemptId, result.document().version(), result.imported(),
                            result.chunks().size(), input.format(), input.warnings().size())) {
                        metrics.succeeded();
                    }
                }
                return;
            }
            MarkdownRebuildResult result = input.structured()
                    ? importer.rebuildStructuredContent(input.source(), input.content(), input.sections(), attemptId)
                    : importer.rebuildContent(input.source(), input.content(), attemptId);
            if (complete(taskId)) {
                if (taskStore.markSucceeded(taskId, attemptId, result.document().version(), true,
                        result.chunks().size(), input.format(), input.warnings().size())) {
                    metrics.succeeded();
                }
            }
        } catch (RuntimeException exception) {
            if (complete(taskId)) {
                if (taskStore.markFailed(taskId, attemptId, exception.getClass().getSimpleName())) {
                    metrics.failed();
                }
            }
        }
    }

    private void timeout(String taskId, String attemptId, FutureTask<Void> work) {
        if (taskStore.find(taskId).map(task -> task.attemptId().equals(attemptId)
                        && (task.status() == KnowledgeDocumentTaskStatus.QUEUED
                        || task.status() == KnowledgeDocumentTaskStatus.RUNNING)).orElse(false)
                && complete(taskId)) {
            if (taskStore.markFailed(taskId, attemptId, "TASK_TIMEOUT")) {
                metrics.timeoutTask();
            }
            work.cancel(true);
            executor.getThreadPoolExecutor().remove(work);
        }
    }

    private boolean complete(String taskId) {
        AtomicBoolean guard = completionGuards.remove(taskId);
        if (guard == null || !guard.compareAndSet(false, true)) {
            return false;
        }
        submittedTasks.remove(taskId);
        cancelTimeout(taskId);
        return true;
    }

    private void failSubmission(String taskId, String attemptId, FutureTask<Void> work, String errorType) {
        if (complete(taskId)) {
            if (taskStore.markFailed(taskId, attemptId, errorType)) {
                metrics.rejected();
                metrics.failed();
            }
        }
        work.cancel(false);
        executor.getThreadPoolExecutor().remove(work);
    }

    private void cancelPendingExecution(String taskId) {
        completionGuards.remove(taskId);
        FutureTask<Void> work = submittedTasks.remove(taskId);
        if (work != null) {
            work.cancel(false);
            executor.getThreadPoolExecutor().remove(work);
        }
        cancelTimeout(taskId);
    }

    private void cancelTimeout(String taskId) {
        ScheduledFuture<?> timeout = taskTimeouts.remove(taskId);
        if (timeout != null) {
            timeout.cancel(false);
        }
    }
}
