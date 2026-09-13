package com.javainterview.rag.knowledge.task;

import com.javainterview.rag.knowledge.document.MarkdownContentImporter;
import com.javainterview.rag.knowledge.document.MarkdownImportResult;
import com.javainterview.rag.knowledge.document.MarkdownRebuildResult;
import org.springframework.core.task.TaskRejectedException;

import java.util.Objects;
import java.util.UUID;
import java.util.List;
import java.util.concurrent.Executor;

public final class KnowledgeDocumentTaskService {

    private final MarkdownContentImporter importer;
    private final KnowledgeDocumentTaskStore taskStore;
    private final Executor executor;
    private final KnowledgeDocumentTaskMetrics metrics;

    public KnowledgeDocumentTaskService(MarkdownContentImporter importer,
                                        KnowledgeDocumentTaskStore taskStore,
                                        Executor executor,
                                        KnowledgeDocumentTaskMetrics metrics) {
        this.importer = Objects.requireNonNull(importer, "Markdown 导入器不能为空");
        this.taskStore = Objects.requireNonNull(taskStore, "资料任务存储不能为空");
        this.executor = Objects.requireNonNull(executor, "资料任务执行器不能为空");
        this.metrics = Objects.requireNonNull(metrics, "资料任务指标不能为空");
    }

    public KnowledgeDocumentTask submit(KnowledgeDocumentTaskOperation operation,
                                        KnowledgeDocumentTaskInput input) {
        Objects.requireNonNull(operation, "任务操作不能为空");
        Objects.requireNonNull(input, "任务输入不能为空");
        String taskId = UUID.randomUUID().toString();
        KnowledgeDocumentTask task = taskStore.create(taskId, input.source(), operation);
        metrics.submitted();
        try {
            executor.execute(() -> execute(taskId, operation, input));
        } catch (TaskRejectedException exception) {
            taskStore.markFailed(taskId, "TASK_REJECTED");
            metrics.rejected();
            metrics.failed();
            throw exception;
        } catch (RuntimeException exception) {
            taskStore.markFailed(taskId, exception.getClass().getSimpleName());
            metrics.rejected();
            metrics.failed();
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
        return find(normalizedTaskId);
    }

    public List<KnowledgeDocumentTask> findRecent(int limit) {
        if (limit < 1 || limit > 20) {
            throw new IllegalArgumentException("任务历史数量必须在 1 到 20 之间");
        }
        return taskStore.findRecent(limit);
    }

    private void execute(String taskId, KnowledgeDocumentTaskOperation operation,
                         KnowledgeDocumentTaskInput input) {
        taskStore.markRunning(taskId);
        if (taskStore.find(taskId).map(task -> task.status() != KnowledgeDocumentTaskStatus.RUNNING).orElse(true)) {
            return;
        }
        metrics.started();
        try {
            if (operation == KnowledgeDocumentTaskOperation.IMPORT) {
                MarkdownImportResult result = input.structured()
                        ? importer.importStructuredContent(input.source(), input.content(), input.sections())
                        : importer.importContent(input.source(), input.content());
                taskStore.markSucceeded(taskId, result.document().version(), result.imported(), result.chunks().size(),
                        input.format(), input.warnings().size());
                metrics.succeeded();
                return;
            }
            MarkdownRebuildResult result = input.structured()
                    ? importer.rebuildStructuredContent(input.source(), input.content(), input.sections())
                    : importer.rebuildContent(input.source(), input.content());
            taskStore.markSucceeded(taskId, result.document().version(), true, result.chunks().size(),
                    input.format(), input.warnings().size());
            metrics.succeeded();
        } catch (RuntimeException exception) {
            taskStore.markFailed(taskId, exception.getClass().getSimpleName());
            metrics.failed();
        }
    }
}
