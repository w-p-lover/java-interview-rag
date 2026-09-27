package com.javainterview.rag.knowledge.task;

import com.javainterview.rag.knowledge.document.MarkdownContentImporter;
import com.javainterview.rag.knowledge.document.MarkdownDocument;
import com.javainterview.rag.knowledge.document.MarkdownImportResult;
import com.javainterview.rag.knowledge.document.KnowledgeStoredFileParser;
import com.javainterview.rag.knowledge.document.PdfKnowledgeDocumentExtractor;
import com.javainterview.rag.file.FileAssetService;
import com.javainterview.rag.file.FilePurpose;
import com.javainterview.rag.file.InMemoryFileAssetStore;
import com.javainterview.rag.file.LocalFileObjectStorage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeDocumentTaskServiceTest {

    private final ThreadPoolTaskExecutor executor = taskExecutor();
    private final ScheduledThreadPoolExecutor timeoutScheduler = new ScheduledThreadPoolExecutor(1);
    private final CountDownLatch releaseRunningTask = new CountDownLatch(1);
    @TempDir
    Path fileRoot;

    @AfterEach
    void tearDown() {
        releaseRunningTask.countDown();
        executor.shutdown();
        timeoutScheduler.shutdownNow();
    }

    @Test
    void cancellingQueuedTaskImmediatelyReleasesQueueCapacity() throws Exception {
        MarkdownContentImporter importer = mock(MarkdownContentImporter.class);
        CountDownLatch runningTaskStarted = new CountDownLatch(1);
        when(importer.importContent(anyString(), anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            String source = invocation.getArgument(0);
            if ("java/running.md".equals(source)) {
                runningTaskStarted.countDown();
                releaseRunningTask.await();
            }
            MarkdownDocument document = MarkdownDocument.create(source, invocation.getArgument(1));
            return new MarkdownImportResult(document, true, List.of());
        });
        FileAssetService files = new FileAssetService(
                new InMemoryFileAssetStore(), new LocalFileObjectStorage(fileRoot));
        KnowledgeDocumentTaskService service = new KnowledgeDocumentTaskService(importer,
                new InMemoryKnowledgeDocumentTaskStore(),
                new KnowledgeStoredFileParser(files, new PdfKnowledgeDocumentExtractor()),
                executor, new KnowledgeDocumentTaskMetrics(),
                timeoutScheduler, 5_000);

        service.submit(KnowledgeDocumentTaskOperation.IMPORT, "java/running.md", file(files, "running"));
        assertThat(runningTaskStarted.await(1, TimeUnit.SECONDS)).isTrue();
        KnowledgeDocumentTask cancelled = service.submit(
                KnowledgeDocumentTaskOperation.IMPORT, "java/cancelled.md", file(files, "cancelled"));
        assertThatThrownBy(() -> service.submit(
                KnowledgeDocumentTaskOperation.IMPORT, "java/rejected.md", file(files, "rejected")))
                .isInstanceOf(TaskRejectedException.class);

        assertThat(service.cancel(cancelled.taskId()).status())
                .isEqualTo(KnowledgeDocumentTaskStatus.CANCELLED);
        KnowledgeDocumentTask replacement = service.submit(
                KnowledgeDocumentTaskOperation.IMPORT, "java/replacement.md", file(files, "replacement"));

        assertThat(replacement.status()).isEqualTo(KnowledgeDocumentTaskStatus.QUEUED);
        assertThat(executor.getThreadPoolExecutor().getQueue()).hasSize(1);
    }

    @Test
    void resumesRunningTaskFromStoredFileWithNewAttempt() throws Exception {
        MarkdownContentImporter importer = mock(MarkdownContentImporter.class);
        when(importer.importContent(anyString(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> new MarkdownImportResult(
                        MarkdownDocument.create(invocation.getArgument(0), invocation.getArgument(1)),
                        true, List.of()));
        FileAssetService files = new FileAssetService(
                new InMemoryFileAssetStore(), new LocalFileObjectStorage(fileRoot));
        InMemoryKnowledgeDocumentTaskStore store = new InMemoryKnowledgeDocumentTaskStore();
        String fileId = file(files, "recovery");
        store.create("task-recovery", "attempt-old", "java/recovery.md", fileId,
                KnowledgeDocumentTaskOperation.IMPORT);
        assertThat(store.markRunning("task-recovery", "attempt-old")).isTrue();
        KnowledgeDocumentTaskService service = new KnowledgeDocumentTaskService(importer, store,
                new KnowledgeStoredFileParser(files, new PdfKnowledgeDocumentExtractor()),
                executor, new KnowledgeDocumentTaskMetrics(), timeoutScheduler, 5_000);

        service.resumePendingTasks();

        KnowledgeDocumentTask recovered = store.find("task-recovery").orElseThrow();
        for (int attempt = 0; attempt < 20 && !recovered.terminal(); attempt++) {
            Thread.sleep(25);
            recovered = store.find("task-recovery").orElseThrow();
        }
        assertThat(recovered.status()).isEqualTo(KnowledgeDocumentTaskStatus.SUCCEEDED);
        assertThat(recovered.attemptId()).isNotEqualTo("attempt-old");
        assertThat(recovered.recoveryCount()).isEqualTo(1);
        assertThat(store.markFailed("task-recovery", "attempt-old", "LATE_FAILURE")).isFalse();
    }

    private String file(FileAssetService files, String name) {
        return files.store(FilePurpose.KNOWLEDGE, name + ".md", "text/markdown",
                ("# Java\n" + name).getBytes(StandardCharsets.UTF_8)).id();
    }

    private ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(1);
        taskExecutor.setMaxPoolSize(1);
        taskExecutor.setQueueCapacity(1);
        taskExecutor.setThreadNamePrefix("knowledge-task-test-");
        taskExecutor.initialize();
        return taskExecutor;
    }
}
