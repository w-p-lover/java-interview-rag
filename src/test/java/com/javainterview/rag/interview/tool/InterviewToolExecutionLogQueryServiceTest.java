package com.javainterview.rag.interview.tool;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InterviewToolExecutionLogQueryServiceTest {

    @Test
    void findsRecentLogsWithinOneSession() {
        InterviewToolExecutionLogStore store = mock(InterviewToolExecutionLogStore.class);
        InterviewToolExecutionLog log = succeededLog();
        when(store.findRecent("session-1", 20)).thenReturn(List.of(log));
        InterviewToolExecutionLogQueryService service =
                new InterviewToolExecutionLogQueryService(store);

        List<InterviewToolExecutionLog> result = service.findRecent(" session-1 ", 20);

        assertEquals(List.of(log), result);
        verify(store).findRecent("session-1", 20);
    }

    @Test
    void rejectsInvalidSessionOrLimitBeforeQueryingStore() {
        InterviewToolExecutionLogStore store = mock(InterviewToolExecutionLogStore.class);
        InterviewToolExecutionLogQueryService service =
                new InterviewToolExecutionLogQueryService(store);

        IllegalArgumentException sessionException = assertThrows(
                IllegalArgumentException.class,
                () -> service.findRecent(" ", 20));
        IllegalArgumentException limitException = assertThrows(
                IllegalArgumentException.class,
                () -> service.findRecent("session-1", 101));

        assertEquals("面试会话 ID 不能为空", sessionException.getMessage());
        assertEquals("工具执行日志查询数量必须在 1 到 100 之间", limitException.getMessage());
        verifyNoInteractions(store);
    }

    private InterviewToolExecutionLog succeededLog() {
        return new InterviewToolExecutionLog(
                1L,
                "execution-1",
                null,
                "list_review_tasks",
                "session-1",
                InterviewToolExecutionLog.Status.SUCCEEDED,
                2,
                15,
                null,
                LocalDateTime.of(2026, 8, 5, 9, 0));
    }
}
