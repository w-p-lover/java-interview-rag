package com.javainterview.rag.knowledge.persistence;

import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskOperation;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MySqlKnowledgeDocumentTaskStoreTest {

    private final KnowledgeDocumentTaskMapper mapper = mock(KnowledgeDocumentTaskMapper.class);
    private final MySqlKnowledgeDocumentTaskStore store = new MySqlKnowledgeDocumentTaskStore(mapper);

    @Test
    void restoresTaskMetadataWithoutPayload() {
        KnowledgeDocumentTaskEntity entity = new KnowledgeDocumentTaskEntity(
                "task-1", "java/basic.md", "IMPORT", "SUCCEEDED", "version-1", true,
                2, "MARKDOWN", 1, null, java.time.LocalDateTime.now(),
                java.time.LocalDateTime.now(), java.time.LocalDateTime.now());
        when(mapper.find("task-1")).thenReturn(entity);

        var task = store.find("task-1").orElseThrow();

        assertThat(task.operation()).isEqualTo(KnowledgeDocumentTaskOperation.IMPORT);
        assertThat(task.status()).isEqualTo(KnowledgeDocumentTaskStatus.SUCCEEDED);
        assertThat(task.imported()).isTrue();
        assertThat(task.chunkCount()).isEqualTo(2);
        assertThat(task.source()).isEqualTo("java/basic.md");
    }

    @Test
    void marksRunningTasksAsFailedAfterRestart() {
        store.failRunningAfterRestart();

        verify(mapper).failRunningAfterRestart();
    }

    @Test
    void cancelsOnlyQueuedTask() {
        when(mapper.cancelQueued("task-1")).thenReturn(1);

        assertThat(store.cancelQueued("task-1")).isTrue();
        verify(mapper).cancelQueued("task-1");
    }

    @Test
    void loadsRecentTasksThroughMapper() {
        when(mapper.findRecent(2)).thenReturn(java.util.List.of());

        assertThat(store.findRecent(2)).isEmpty();
        verify(mapper).findRecent(2);
    }
}
