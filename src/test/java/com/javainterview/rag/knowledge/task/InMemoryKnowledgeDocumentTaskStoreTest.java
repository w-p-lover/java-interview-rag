package com.javainterview.rag.knowledge.task;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryKnowledgeDocumentTaskStoreTest {

    @Test
    void storesQueuedTaskAndAllowsOnlyForwardLifecycleTransitions() {
        KnowledgeDocumentTaskStore store = new InMemoryKnowledgeDocumentTaskStore();
        KnowledgeDocumentTask queued = store.create("task-1", "attempt-1", "java/basic.md",
                KnowledgeDocumentTaskOperation.IMPORT);

        assertThat(queued.status()).isEqualTo(KnowledgeDocumentTaskStatus.QUEUED);
        assertThat(store.markRunning("task-1", "attempt-1")).isTrue();
        assertThat(store.markSucceeded("task-1", "attempt-1", "version-1", true,
                2, "MARKDOWN", 0)).isTrue();
        assertThat(store.markFailed("task-1", "attempt-1", "IllegalStateException")).isFalse();

        KnowledgeDocumentTask result = store.find("task-1").orElseThrow();
        assertThat(result.status()).isEqualTo(KnowledgeDocumentTaskStatus.SUCCEEDED);
        assertThat(result.version()).isEqualTo("version-1");
        assertThat(result.chunkCount()).isEqualTo(2);
    }

    @Test
    void cancelsQueuedTaskAndKeepsItTerminal() {
        KnowledgeDocumentTaskStore store = new InMemoryKnowledgeDocumentTaskStore();
        store.create("task-2", "attempt-2", "java/cancel.md", KnowledgeDocumentTaskOperation.IMPORT);

        assertThat(store.cancelQueued("task-2")).isTrue();
        assertThat(store.cancelQueued("task-2")).isFalse();
        assertThat(store.find("task-2").orElseThrow().status())
                .isEqualTo(KnowledgeDocumentTaskStatus.CANCELLED);
    }

    @Test
    void returnsRecentTasksNewestFirst() {
        KnowledgeDocumentTaskStore store = new InMemoryKnowledgeDocumentTaskStore();
        store.create("task-old", "attempt-old", "old.md", KnowledgeDocumentTaskOperation.IMPORT);
        store.create("task-new", "attempt-new", "new.md", KnowledgeDocumentTaskOperation.REBUILD);

        assertThat(store.findRecent(1)).extracting(KnowledgeDocumentTask::taskId)
                .containsExactly("task-new");
    }

    @Test
    void rejectsLateWritesFromDifferentAttempt() {
        KnowledgeDocumentTaskStore store = new InMemoryKnowledgeDocumentTaskStore();
        store.create("task-3", "attempt-current", "java/fencing.md",
                KnowledgeDocumentTaskOperation.REBUILD);

        assertThat(store.markSucceeded("task-3", "attempt-current", "version-early", true,
                1, "MARKDOWN", 0)).isFalse();
        assertThat(store.markRunning("task-3", "attempt-old")).isFalse();
        assertThat(store.markFailed("task-3", "attempt-old", "LATE_FAILURE")).isFalse();
        assertThat(store.markRunning("task-3", "attempt-current")).isTrue();
        assertThat(store.markSucceeded("task-3", "attempt-old", "version-old", true,
                1, "MARKDOWN", 0)).isFalse();

        KnowledgeDocumentTask current = store.find("task-3").orElseThrow();
        assertThat(current.status()).isEqualTo(KnowledgeDocumentTaskStatus.RUNNING);
        assertThat(current.attemptId()).isEqualTo("attempt-current");
    }

    @Test
    void requeuesRunningTaskWithNewAttemptOnlyOnce() {
        KnowledgeDocumentTaskStore store = new InMemoryKnowledgeDocumentTaskStore();
        store.create("task-recovery", "attempt-old", "java/recovery.md", "file-1",
                KnowledgeDocumentTaskOperation.IMPORT);
        assertThat(store.markRunning("task-recovery", "attempt-old")).isTrue();

        assertThat(store.requeueAfterRestart(
                "task-recovery", "attempt-old", "attempt-new", 1)).isTrue();
        KnowledgeDocumentTask recovered = store.find("task-recovery").orElseThrow();
        assertThat(recovered.status()).isEqualTo(KnowledgeDocumentTaskStatus.QUEUED);
        assertThat(recovered.attemptId()).isEqualTo("attempt-new");
        assertThat(recovered.recoveryCount()).isEqualTo(1);
        assertThat(store.markFailed("task-recovery", "attempt-old", "LATE_FAILURE")).isFalse();
    }
}
