package com.javainterview.rag.interview.tool;

import com.javainterview.rag.interview.review.InterviewReviewKnowledgeSource;
import com.javainterview.rag.interview.review.InterviewReviewTask;
import com.javainterview.rag.interview.review.InterviewReviewTaskService;
import com.javainterview.rag.interview.review.InterviewReviewTaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InterviewReviewToolsTest {

    @Test
    void listsReviewTasksThroughToolBoundary() {
        InterviewReviewTaskService service = mock(InterviewReviewTaskService.class);
        LocalDate from = LocalDate.of(2026, 8, 4);
        LocalDate to = LocalDate.of(2026, 8, 10);
        when(service.findBySessionId("session-1", "PENDING", from, to))
                .thenReturn(List.of(reviewTask(
                        "task-1",
                        InterviewReviewTaskStatus.PENDING,
                        from,
                        List.of(new InterviewReviewKnowledgeSource(
                                "rag/vector-lifecycle.md",
                                "version-1",
                                2,
                                List.of("向量生命周期", "更新"),
                                0.82)))));
        InterviewToolExecutionLogStore logStore = mock(InterviewToolExecutionLogStore.class);
        InterviewReviewTools tools = new InterviewReviewTools(service, logStore);

        InterviewReviewTools.ReviewTaskListToolResult result = tools.listReviewTasks(
                " session-1 ", "PENDING", "2026-08-04", "2026-08-10", null);

        assertEquals("session-1", result.sessionId());
        assertEquals("PENDING", result.statusFilter());
        assertEquals(1, result.totalTaskCount());
        assertEquals(1, result.pendingTaskCount());
        assertEquals(0, result.completedTaskCount());
        InterviewReviewTools.ReviewTaskToolItem task = result.tasks().get(0);
        assertEquals("task-1", task.taskId());
        assertEquals(List.of("缺少失败补偿细节"), task.gaps());
        assertTrue(task.knowledgeGrounded());
        assertEquals("rag/vector-lifecycle.md", task.knowledgeSources().get(0).source());
        verify(service).findBySessionId("session-1", "PENDING", from, to);
        verify(logStore).record(argThat(log ->
                log.status() == InterviewToolExecutionLog.Status.SUCCEEDED
                        && log.resultCount() == 1
                        && log.errorType() == null));
    }

    @Test
    void rejectsInvalidToolArgumentsBeforeCallingService() {
        InterviewReviewTaskService service = mock(InterviewReviewTaskService.class);
        InterviewToolExecutionLogStore logStore = mock(InterviewToolExecutionLogStore.class);
        InterviewReviewTools tools = new InterviewReviewTools(service, logStore);

        IllegalArgumentException sessionException = assertThrows(
                IllegalArgumentException.class,
                () -> tools.listReviewTasks(" ", null, null, null, null));
        IllegalArgumentException dateException = assertThrows(
                IllegalArgumentException.class,
                () -> tools.listReviewTasks("session-1", null, "2026/08/04", null, null));

        assertEquals("面试会话 ID 不能为空", sessionException.getMessage());
        assertEquals("计划复习日期格式必须为 yyyy-MM-dd", dateException.getMessage());
        verifyNoInteractions(service);
        verify(logStore).record(argThat(log ->
                log.status() == InterviewToolExecutionLog.Status.FAILED
                        && "IllegalArgumentException".equals(log.errorType())));
    }

    @Test
    void exposesSpringAiToolCallbackMetadata() {
        InterviewReviewTools tools = new InterviewReviewTools(
                mock(InterviewReviewTaskService.class),
                mock(InterviewToolExecutionLogStore.class));

        ToolCallback[] callbacks = MethodToolCallbackProvider.builder()
                .toolObjects(tools)
                .build()
                .getToolCallbacks();

        assertEquals(2, callbacks.length);
        assertEquals(Set.of("list_review_tasks", "schedule_review_task"),
                java.util.Arrays.stream(callbacks)
                        .map(callback -> callback.getToolDefinition().name())
                        .collect(java.util.stream.Collectors.toSet()));
        ToolCallback listCallback = java.util.Arrays.stream(callbacks)
                .filter(callback -> "list_review_tasks".equals(callback.getToolDefinition().name()))
                .findFirst().orElseThrow();
        ToolCallback scheduleCallback = java.util.Arrays.stream(callbacks)
                .filter(callback -> "schedule_review_task".equals(callback.getToolDefinition().name()))
                .findFirst().orElseThrow();
        assertTrue(listCallback.getToolDefinition().inputSchema().contains("plannedDateFrom"));
        assertTrue(scheduleCallback.getToolDefinition().description().contains("计划复习日期"));
        assertTrue(scheduleCallback.getToolDefinition().inputSchema().contains("taskId"));
        assertFalse(listCallback.getToolDefinition().inputSchema().contains("toolContext"));
        assertFalse(scheduleCallback.getToolDefinition().inputSchema().contains("toolContext"));
    }

    @Test
    void countsCompletedAndPendingTasksSeparately() {
        InterviewReviewTaskService service = mock(InterviewReviewTaskService.class);
        when(service.findBySessionId("session-1", null, null, null))
                .thenReturn(List.of(
                        reviewTask("task-1", InterviewReviewTaskStatus.PENDING, null, List.of()),
                        reviewTask("task-2", InterviewReviewTaskStatus.COMPLETED, null, List.of())));
        InterviewReviewTools tools = new InterviewReviewTools(
                service, mock(InterviewToolExecutionLogStore.class));

        InterviewReviewTools.ReviewTaskListToolResult result = tools.listReviewTasks(
                "session-1", null, null, null, null);

        assertEquals(2, result.totalTaskCount());
        assertEquals(1, result.pendingTaskCount());
        assertEquals(1, result.completedTaskCount());
        assertFalse(result.tasks().get(0).knowledgeGrounded());
    }

    @Test
    void recordsServiceFailureAndRethrowsOriginalException() {
        InterviewReviewTaskService service = mock(InterviewReviewTaskService.class);
        InterviewToolExecutionLogStore logStore = mock(InterviewToolExecutionLogStore.class);
        when(service.findBySessionId("missing-session", null, null, null))
                .thenThrow(new java.util.NoSuchElementException("面试会话不存在"));
        InterviewReviewTools tools = new InterviewReviewTools(service, logStore);

        java.util.NoSuchElementException exception = assertThrows(
                java.util.NoSuchElementException.class,
                () -> tools.listReviewTasks("missing-session", null, null, null, null));

        assertEquals("面试会话不存在", exception.getMessage());
        verify(logStore).record(argThat(log ->
                log.status() == InterviewToolExecutionLog.Status.FAILED
                        && "NoSuchElementException".equals(log.errorType())));
    }

    @Test
    void doesNotFailToolWhenExecutionLogCannotBeStored() {
        InterviewReviewTaskService service = mock(InterviewReviewTaskService.class);
        InterviewToolExecutionLogStore logStore = mock(InterviewToolExecutionLogStore.class);
        when(service.findBySessionId("session-1", null, null, null)).thenReturn(List.of());
        doThrow(new IllegalStateException("日志表不可用"))
                .when(logStore).record(org.mockito.ArgumentMatchers.any());
        InterviewReviewTools tools = new InterviewReviewTools(service, logStore);

        InterviewReviewTools.ReviewTaskListToolResult result = assertDoesNotThrow(
                () -> tools.listReviewTasks("session-1", null, null, null, null));

        assertEquals(0, result.totalTaskCount());
    }

    @Test
    void schedulesReviewTaskThroughControlledWriteTool() {
        InterviewReviewTaskService service = mock(InterviewReviewTaskService.class);
        InterviewToolExecutionLogStore logStore = mock(InterviewToolExecutionLogStore.class);
        LocalDate plannedDate = LocalDate.of(2026, 8, 12);
        when(service.schedule("session-1", "task-1", plannedDate))
                .thenReturn(reviewTask(
                        "task-1", InterviewReviewTaskStatus.PENDING, plannedDate, List.of()));
        InterviewReviewTools tools = new InterviewReviewTools(service, logStore);
        InterviewAssistantRequestContext requestContext =
                InterviewAssistantRequestContext.create(
                        new InterviewReviewScheduleConfirmation(
                                "session-1", "task-1", plannedDate));

        InterviewReviewTools.ReviewTaskScheduleToolResult result =
                tools.scheduleReviewTask(
                        " session-1 ", " task-1 ", "2026-08-12",
                        new ToolContext(requestContext.asMap()));

        assertEquals("session-1", result.sessionId());
        assertEquals("task-1", result.taskId());
        assertEquals(plannedDate, result.plannedReviewDate());
        assertEquals(InterviewReviewTaskStatus.PENDING, result.status());
        verify(service).schedule("session-1", "task-1", plannedDate);
        verify(logStore).record(argThat(log ->
                "schedule_review_task".equals(log.toolName())
                        && log.status() == InterviewToolExecutionLog.Status.SUCCEEDED
                        && log.resultCount() == 1));
    }

    @Test
    void rejectsInvalidScheduleDateAndRecordsFailure() {
        InterviewReviewTaskService service = mock(InterviewReviewTaskService.class);
        InterviewToolExecutionLogStore logStore = mock(InterviewToolExecutionLogStore.class);
        InterviewReviewTools tools = new InterviewReviewTools(service, logStore);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> tools.scheduleReviewTask("session-1", "task-1", "2026/08/12", null));

        assertEquals("计划复习日期格式必须为 yyyy-MM-dd", exception.getMessage());
        verifyNoInteractions(service);
        verify(logStore).record(argThat(log ->
                "schedule_review_task".equals(log.toolName())
                        && log.status() == InterviewToolExecutionLog.Status.FAILED
                        && "IllegalArgumentException".equals(log.errorType())));
    }

    @Test
    void rejectsScheduleWithoutHumanConfirmation() {
        InterviewReviewTaskService service = mock(InterviewReviewTaskService.class);
        InterviewToolExecutionLogStore logStore = mock(InterviewToolExecutionLogStore.class);
        InterviewReviewTools tools = new InterviewReviewTools(service, logStore);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> tools.scheduleReviewTask(
                        "session-1", "task-1", "2026-08-12", null));

        assertEquals("设置计划复习日期前必须提供人工确认", exception.getMessage());
        verifyNoInteractions(service);
        verify(logStore).record(argThat(log ->
                "schedule_review_task".equals(log.toolName())
                        && log.status() == InterviewToolExecutionLog.Status.FAILED
                        && "IllegalStateException".equals(log.errorType())));
    }

    @Test
    void rejectsScheduleWhenConfirmationDoesNotMatchToolArguments() {
        InterviewReviewTaskService service = mock(InterviewReviewTaskService.class);
        InterviewToolExecutionLogStore logStore = mock(InterviewToolExecutionLogStore.class);
        InterviewReviewTools tools = new InterviewReviewTools(service, logStore);
        InterviewAssistantRequestContext requestContext =
                InterviewAssistantRequestContext.create(
                        new InterviewReviewScheduleConfirmation(
                                "session-1", "task-2", LocalDate.of(2026, 8, 12)));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> tools.scheduleReviewTask(
                        "session-1", "task-1", "2026-08-12",
                        new ToolContext(requestContext.asMap())));

        assertEquals("工具参数与人工确认的改期内容不一致", exception.getMessage());
        verifyNoInteractions(service);
    }

    @Test
    void correlatesToolExecutionWithAssistantRequestContext() {
        InterviewReviewTaskService service = mock(InterviewReviewTaskService.class);
        InterviewToolExecutionLogStore logStore = mock(InterviewToolExecutionLogStore.class);
        when(service.findBySessionId("session-1", null, null, null)).thenReturn(List.of());
        InterviewReviewTools tools = new InterviewReviewTools(service, logStore);
        InterviewAssistantRequestContext requestContext =
                InterviewAssistantRequestContext.create();

        tools.listReviewTasks(
                "session-1", null, null, null,
                new ToolContext(requestContext.asMap()));

        assertEquals(1, requestContext.toolExecutionCount());
        verify(logStore).record(argThat(log ->
                requestContext.requestId().equals(log.assistantRequestId())
                        && log.status() == InterviewToolExecutionLog.Status.SUCCEEDED));
    }

    private InterviewReviewTask reviewTask(String id,
                                           InterviewReviewTaskStatus status,
                                           LocalDate plannedReviewDate,
                                           List<InterviewReviewKnowledgeSource> knowledgeSources) {
        LocalDateTime completedAt = status == InterviewReviewTaskStatus.COMPLETED
                ? LocalDateTime.of(2026, 8, 5, 9, 30)
                : null;
        return new InterviewReviewTask(
                id,
                "session-1",
                0,
                "如何保证向量更新一致性？",
                "向量生命周期",
                List.of("缺少失败补偿细节"),
                "补充异常分支和恢复方式。",
                !knowledgeSources.isEmpty(),
                knowledgeSources,
                status,
                plannedReviewDate,
                LocalDateTime.of(2026, 8, 4, 9, 0),
                completedAt
        );
    }
}
