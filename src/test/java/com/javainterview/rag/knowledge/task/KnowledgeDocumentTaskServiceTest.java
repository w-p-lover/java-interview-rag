package com.javainterview.rag.knowledge.task;

import com.javainterview.rag.knowledge.document.MarkdownContentImporter;
import com.javainterview.rag.knowledge.document.MarkdownDocument;
import com.javainterview.rag.knowledge.document.MarkdownImportResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class KnowledgeDocumentTaskServiceTest {

    private final ThreadPoolTaskExecutor executor = taskExecutor();
    private final ScheduledThreadPoolExecutor timeoutScheduler = new ScheduledThreadPoolExecutor(1);
    private final CountDownLatch releaseRunningTask = new CountDownLatch(1);

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
        when(importer.importContent(anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            String source = invocation.getArgument(0);
            if ("java/running.md".equals(source)) {
                runningTaskStarted.countDown();
                releaseRunningTask.await();
            }
            MarkdownDocument document = MarkdownDocument.create(source, invocation.getArgument(1));
            return new MarkdownImportResult(document, true, List.of());
        });
        KnowledgeDocumentTaskService service = new KnowledgeDocumentTaskService(importer,
                new InMemoryKnowledgeDocumentTaskStore(), executor, new KnowledgeDocumentTaskMetrics(),
                timeoutScheduler, 5_000);

        service.submit(KnowledgeDocumentTaskOperation.IMPORT, input("java/running.md"));
        assertThat(runningTaskStarted.await(1, TimeUnit.SECONDS)).isTrue();
        KnowledgeDocumentTask cancelled = service.submit(
                KnowledgeDocumentTaskOperation.IMPORT, input("java/cancelled.md"));
        assertThatThrownBy(() -> service.submit(
                KnowledgeDocumentTaskOperation.IMPORT, input("java/rejected.md")))
                .isInstanceOf(TaskRejectedException.class);

        assertThat(service.cancel(cancelled.taskId()).status())
                .isEqualTo(KnowledgeDocumentTaskStatus.CANCELLED);
        KnowledgeDocumentTask replacement = service.submit(
                KnowledgeDocumentTaskOperation.IMPORT, input("java/replacement.md"));

        assertThat(replacement.status()).isEqualTo(KnowledgeDocumentTaskStatus.QUEUED);
        assertThat(executor.getThreadPoolExecutor().getQueue()).hasSize(1);
    }

    private KnowledgeDocumentTaskInput input(String source) {
        return new KnowledgeDocumentTaskInput(source, "# Java\n任务内容", "MARKDOWN",
                null, List.of(), List.of(), List.of());
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
