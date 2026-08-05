package com.javainterview.rag.interview.tool;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterviewAssistantRequestLogQueryServiceTest {

    @Test
    void returnsRecentRequestLogsWithinLimit() {
        InterviewAssistantRequestLogStore store = mock(InterviewAssistantRequestLogStore.class);
        List<InterviewAssistantRequestLog> logs = List.of(succeededLog());
        when(store.findRecent(20)).thenReturn(logs);
        InterviewAssistantRequestLogQueryService service =
                new InterviewAssistantRequestLogQueryService(store);

        List<InterviewAssistantRequestLog> result = service.findRecent(20);

        assertEquals(logs, result);
        verify(store).findRecent(20);
    }

    @Test
    void rejectsLimitOutsideSupportedRange() {
        InterviewAssistantRequestLogQueryService service =
                new InterviewAssistantRequestLogQueryService(
                        mock(InterviewAssistantRequestLogStore.class));

        IllegalArgumentException tooSmall = assertThrows(
                IllegalArgumentException.class, () -> service.findRecent(0));
        IllegalArgumentException tooLarge = assertThrows(
                IllegalArgumentException.class, () -> service.findRecent(101));

        assertEquals("助手请求日志查询数量必须在 1 到 100 之间", tooSmall.getMessage());
        assertEquals("助手请求日志查询数量必须在 1 到 100 之间", tooLarge.getMessage());
    }

    private InterviewAssistantRequestLog succeededLog() {
        return new InterviewAssistantRequestLog(
                1L,
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
                LocalDateTime.of(2026, 8, 5, 10, 30));
    }
}
