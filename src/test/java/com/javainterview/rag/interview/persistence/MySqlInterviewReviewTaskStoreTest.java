package com.javainterview.rag.interview.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.interview.review.InterviewReviewKnowledgeSource;
import com.javainterview.rag.interview.review.InterviewReviewTask;
import com.javainterview.rag.interview.review.InterviewReviewTaskStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MySqlInterviewReviewTaskStoreTest {

    @Test
    void persistsAndRestoresReviewTaskJsonMetadata() throws Exception {
        InterviewReviewTaskMapper mapper = mock(InterviewReviewTaskMapper.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        MySqlInterviewReviewTaskStore store = new MySqlInterviewReviewTaskStore(mapper, objectMapper);
        InterviewReviewTask task = task();

        store.createIfAbsent(task);

        ArgumentCaptor<InterviewReviewTaskEntity> captor =
                ArgumentCaptor.forClass(InterviewReviewTaskEntity.class);
        verify(mapper).insertIfAbsent(captor.capture());
        InterviewReviewTaskEntity entity = captor.getValue();
        assertEquals("[\"缺少失败补偿细节\"]", entity.getGapsJson());
        assertFalse(entity.getKnowledgeSourcesJson().contains("知识正文"));

        when(mapper.findBySessionId("session-1")).thenReturn(List.of(entity));
        assertEquals(List.of(task), store.findBySessionId("session-1"));
    }

    @Test
    void completesAndRestoresReviewTaskBySessionAndTaskId() throws Exception {
        InterviewReviewTaskMapper mapper = mock(InterviewReviewTaskMapper.class);
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        MySqlInterviewReviewTaskStore store = new MySqlInterviewReviewTaskStore(mapper, objectMapper);
        LocalDateTime completedAt = LocalDateTime.of(2026, 7, 30, 9, 30);
        InterviewReviewTask completedTask = task(
                InterviewReviewTaskStatus.COMPLETED, completedAt);
        InterviewReviewTaskEntity completedEntity = InterviewReviewTaskEntity.from(
                completedTask,
                objectMapper.writeValueAsString(completedTask.gaps()),
                objectMapper.writeValueAsString(completedTask.knowledgeSources()));
        when(mapper.findBySessionAndId("session-1", "task-1"))
                .thenReturn(completedEntity);

        Optional<InterviewReviewTask> result = store.complete(
                "session-1", "task-1", completedAt);

        verify(mapper).complete("session-1", "task-1", completedAt);
        assertEquals(Optional.of(completedTask), result);
    }

    private InterviewReviewTask task() {
        return task(InterviewReviewTaskStatus.PENDING, null);
    }

    private InterviewReviewTask task(InterviewReviewTaskStatus status,
                                     LocalDateTime completedAt) {
        return new InterviewReviewTask(
                "task-1",
                "session-1",
                0,
                "如何保证向量更新一致性？",
                "向量生命周期",
                List.of("缺少失败补偿细节"),
                "补充异常分支和恢复方式。",
                true,
                List.of(new InterviewReviewKnowledgeSource(
                        "rag/vector-lifecycle.md",
                        "version-1",
                        2,
                        List.of("向量生命周期", "更新"),
                        0.82)),
                status,
                LocalDateTime.of(2026, 7, 29, 9, 3),
                completedAt
        );
    }
}
