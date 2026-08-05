package com.javainterview.rag.interview.persistence;

import com.javainterview.rag.interview.tool.InterviewAssistantRequestLog;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MySqlInterviewAssistantRequestLogStoreTest {

    @Test
    void mapsRequestMetadataWithoutMessageOrResponseBody() {
        InterviewAssistantRequestLogMapper mapper = mock(InterviewAssistantRequestLogMapper.class);
        MySqlInterviewAssistantRequestLogStore store =
                new MySqlInterviewAssistantRequestLogStore(mapper);
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 5, 10, 30);

        store.record(new InterviewAssistantRequestLog(
                null,
                "request-1",
                "qwen3.7-plus",
                InterviewAssistantRequestLog.Status.SUCCEEDED,
                1,
                120,
                30,
                150,
                800,
                true,
                null,
                createdAt));

        ArgumentCaptor<InterviewAssistantRequestLogEntity> captor =
                ArgumentCaptor.forClass(InterviewAssistantRequestLogEntity.class);
        verify(mapper).insert(captor.capture());
        InterviewAssistantRequestLogEntity entity = captor.getValue();
        assertEquals("request-1", entity.getRequestId());
        assertEquals("qwen3.7-plus", entity.getModelName());
        assertEquals("SUCCEEDED", entity.getStatus());
        assertEquals(1, entity.getToolExecutionCount());
        assertEquals(120, entity.getPromptTokens());
        assertEquals(30, entity.getCompletionTokens());
        assertEquals(150, entity.getTotalTokens());
        assertEquals(800L, entity.getElapsedMillis());
        assertEquals(true, entity.getFinalResponsePresent());
        assertEquals(createdAt, entity.getCreatedAt());
    }

    @Test
    void readsRecentRequestLogs() {
        InterviewAssistantRequestLogMapper mapper = mock(InterviewAssistantRequestLogMapper.class);
        MySqlInterviewAssistantRequestLogStore store =
                new MySqlInterviewAssistantRequestLogStore(mapper);
        InterviewAssistantRequestLog log = new InterviewAssistantRequestLog(
                2L,
                "request-2",
                "qwen3.7-plus",
                InterviewAssistantRequestLog.Status.FAILED,
                0,
                null,
                null,
                null,
                25,
                false,
                "IllegalStateException",
                LocalDateTime.of(2026, 8, 5, 10, 35));
        when(mapper.findRecent(20)).thenReturn(List.of(
                InterviewAssistantRequestLogEntity.from(log)));

        List<InterviewAssistantRequestLog> result = store.findRecent(20);

        assertEquals(List.of(log), result);
        verify(mapper).findRecent(20);
    }
}
