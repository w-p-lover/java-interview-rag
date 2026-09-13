package com.javainterview.rag.knowledge.task;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryKnowledgeDocumentTaskStoreTest {

    @Test
    void storesQueuedTaskAndAllowsOnlyForwardLifecycleTransitions() {
        KnowledgeDocumentTaskStore store = new InMemoryKnowledgeDocumentTaskStore();
        KnowledgeDocumentTask queued = store.create("task-1", "java/basic.md",
                KnowledgeDocumentTaskOperation.IMPORT);

        assertThat(queued.status()).isEqualTo(KnowledgeDocumentTaskStatus.QUEUED);
        store.markRunning("task-1");
        store.markSucceeded("task-1", "version-1", true, 2, "MARKDOWN", 0);
        store.markFailed("task-1", "IllegalStateException");

        KnowledgeDocumentTask result = store.find("task-1").orElseThrow();
        assertThat(result.status()).isEqualTo(KnowledgeDocumentTaskStatus.SUCCEEDED);
        assertThat(result.version()).isEqualTo("version-1");
        assertThat(result.chunkCount()).isEqualTo(2);
    }

    @Test
    void cancelsQueuedTaskAndKeepsItTerminal() {
        KnowledgeDocumentTaskStore store = new InMemoryKnowledgeDocumentTaskStore();
        store.create("task-2", "java/cancel.md", KnowledgeDocumentTaskOperation.IMPORT);

        assertThat(store.cancelQueued("task-2")).isTrue();
        assertThat(store.cancelQueued("task-2")).isFalse();
        assertThat(store.find("task-2").orElseThrow().status())
                .isEqualTo(KnowledgeDocumentTaskStatus.CANCELLED);
    }

    @Test
    void returnsRecentTasksNewestFirst() {
        KnowledgeDocumentTaskStore store = new InMemoryKnowledgeDocumentTaskStore();
        store.create("task-old", "old.md", KnowledgeDocumentTaskOperation.IMPORT);
        store.create("task-new", "new.md", KnowledgeDocumentTaskOperation.REBUILD);

        assertThat(store.findRecent(1)).extracting(KnowledgeDocumentTask::taskId)
                .containsExactly("task-new");
    }
}
