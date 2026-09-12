package com.javainterview.rag.knowledge.task;

import com.javainterview.rag.knowledge.document.MarkdownContentImporter;
import com.javainterview.rag.knowledge.document.MarkdownImportResult;
import com.javainterview.rag.knowledge.document.MarkdownRebuildResult;
import org.springframework.core.task.TaskRejectedException;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executor;

public final class KnowledgeDocumentTaskService {

    private final MarkdownContentImporter importer;
    private final KnowledgeDocumentTaskStore taskStore;
    private final Executor executor;

    public KnowledgeDocumentTaskService(MarkdownContentImporter importer,
                                        KnowledgeDocumentTaskStore taskStore,
                                        Executor executor) {
        this.importer = Objects.requireNonNull(importer, "Markdown 导入器不能为空");
        this.taskStore = Objects.requireNonNull(taskStore, "资料任务存储不能为空");
        this.executor = Objects.requireNonNull(executor, "资料任务执行器不能为空");
    }

    public KnowledgeDocumentTask submit(KnowledgeDocumentTaskOperation operation,
                                        KnowledgeDocumentTaskInput input) {
        Objects.requireNonNull(operation, "任务操作不能为空");
        Objects.requireNonNull(input, "任务输入不能为空");
        String taskId = UUID.randomUUID().toString();
        KnowledgeDocumentTask task = taskStore.create(taskId, input.source(), operation);
        try {
            executor.execute(() -> execute(taskId, operation, input));
        } catch (TaskRejectedException exception) {
            taskStore.markFailed(taskId, "TASK_REJECTED");
            throw exception;
        } catch (RuntimeException exception) {
            taskStore.markFailed(taskId, exception.getClass().getSimpleName());
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

    private void execute(String taskId, KnowledgeDocumentTaskOperation operation,
                         KnowledgeDocumentTaskInput input) {
        taskStore.markRunning(taskId);
        try {
            if (operation == KnowledgeDocumentTaskOperation.IMPORT) {
                MarkdownImportResult result = input.structured()
                        ? importer.importStructuredContent(input.source(), input.content(), input.sections())
                        : importer.importContent(input.source(), input.content());
                taskStore.markSucceeded(taskId, result.document().version(), result.imported(), result.chunks().size(),
                        input.format(), input.warnings().size());
                return;
            }
            MarkdownRebuildResult result = input.structured()
                    ? importer.rebuildStructuredContent(input.source(), input.content(), input.sections())
                    : importer.rebuildContent(input.source(), input.content());
            taskStore.markSucceeded(taskId, result.document().version(), true, result.chunks().size(),
                    input.format(), input.warnings().size());
        } catch (RuntimeException exception) {
            taskStore.markFailed(taskId, exception.getClass().getSimpleName());
        }
    }
}
