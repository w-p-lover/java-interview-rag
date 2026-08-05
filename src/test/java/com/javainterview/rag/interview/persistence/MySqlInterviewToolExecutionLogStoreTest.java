package com.javainterview.rag.interview.persistence;

import com.javainterview.rag.interview.tool.InterviewToolExecutionLog;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MySqlInterviewToolExecutionLogStoreTest {

    @Test
    void mapsExecutionMetadataWithoutToolResultContent() {
        InterviewToolExecutionLogMapper mapper = mock(InterviewToolExecutionLogMapper.class);
        MySqlInterviewToolExecutionLogStore store =
                new MySqlInterviewToolExecutionLogStore(mapper);
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 5, 9, 0);

        store.record(new InterviewToolExecutionLog(
                null,
                "execution-1",
                "list_review_tasks",
                "session-1",
                InterviewToolExecutionLog.Status.SUCCEEDED,
                2,
                18,
                null,
                createdAt));

        ArgumentCaptor<InterviewToolExecutionLogEntity> captor =
                ArgumentCaptor.forClass(InterviewToolExecutionLogEntity.class);
        verify(mapper).insert(captor.capture());
        InterviewToolExecutionLogEntity entity = captor.getValue();
        assertEquals("execution-1", entity.getExecutionId());
        assertEquals("list_review_tasks", entity.getToolName());
        assertEquals("session-1", entity.getSessionId());
        assertEquals("SUCCEEDED", entity.getStatus());
        assertEquals(2, entity.getResultCount());
        assertEquals(18L, entity.getElapsedMillis());
        assertEquals(createdAt, entity.getCreatedAt());
    }

    @Test
    void readsRecentLogsForRequestedSession() {
        InterviewToolExecutionLogMapper mapper = mock(InterviewToolExecutionLogMapper.class);
        MySqlInterviewToolExecutionLogStore store =
                new MySqlInterviewToolExecutionLogStore(mapper);
        InterviewToolExecutionLog log = new InterviewToolExecutionLog(
                3L,
                "execution-3",
                "list_review_tasks",
                "session-1",
                InterviewToolExecutionLog.Status.FAILED,
                null,
                9,
                "NoSuchElementException",
                LocalDateTime.of(2026, 8, 5, 9, 10));
        when(mapper.findRecent("session-1", 20))
                .thenReturn(List.of(InterviewToolExecutionLogEntity.from(log)));

        List<InterviewToolExecutionLog> result = store.findRecent("session-1", 20);

        assertEquals(List.of(log), result);
        verify(mapper).findRecent("session-1", 20);
    }
}
