package com.javainterview.rag.interview.tool;

import com.javainterview.rag.interview.review.InterviewReviewKnowledgeSource;
import com.javainterview.rag.interview.review.InterviewReviewTask;
import com.javainterview.rag.interview.review.InterviewReviewTaskService;
import com.javainterview.rag.interview.review.InterviewReviewTaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
        InterviewReviewTools tools = new InterviewReviewTools(service);

        InterviewReviewTools.ReviewTaskListToolResult result = tools.listReviewTasks(
                " session-1 ", "PENDING", "2026-08-04", "2026-08-10");

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
    }

    @Test
    void rejectsInvalidToolArgumentsBeforeCallingService() {
        InterviewReviewTaskService service = mock(InterviewReviewTaskService.class);
        InterviewReviewTools tools = new InterviewReviewTools(service);

        IllegalArgumentException sessionException = assertThrows(
                IllegalArgumentException.class,
                () -> tools.listReviewTasks(" ", null, null, null));
        IllegalArgumentException dateException = assertThrows(
                IllegalArgumentException.class,
                () -> tools.listReviewTasks("session-1", null, "2026/08/04", null));

        assertEquals("面试会话 ID 不能为空", sessionException.getMessage());
        assertEquals("计划复习日期格式必须为 yyyy-MM-dd", dateException.getMessage());
        verifyNoInteractions(service);
    }

    @Test
    void exposesSpringAiToolCallbackMetadata() {
        InterviewReviewTools tools = new InterviewReviewTools(mock(InterviewReviewTaskService.class));

        ToolCallback[] callbacks = MethodToolCallbackProvider.builder()
                .toolObjects(tools)
                .build()
                .getToolCallbacks();

        assertEquals(1, callbacks.length);
        assertEquals("list_review_tasks", callbacks[0].getToolDefinition().name());
        assertTrue(callbacks[0].getToolDefinition().description().contains("复习任务"));
        assertTrue(callbacks[0].getToolDefinition().inputSchema().contains("plannedDateFrom"));
    }

    @Test
    void countsCompletedAndPendingTasksSeparately() {
        InterviewReviewTaskService service = mock(InterviewReviewTaskService.class);
        when(service.findBySessionId("session-1", null, null, null))
                .thenReturn(List.of(
                        reviewTask("task-1", InterviewReviewTaskStatus.PENDING, null, List.of()),
                        reviewTask("task-2", InterviewReviewTaskStatus.COMPLETED, null, List.of())));
        InterviewReviewTools tools = new InterviewReviewTools(service);

        InterviewReviewTools.ReviewTaskListToolResult result = tools.listReviewTasks(
                "session-1", null, null, null);

        assertEquals(2, result.totalTaskCount());
        assertEquals(1, result.pendingTaskCount());
        assertEquals(1, result.completedTaskCount());
        assertFalse(result.tasks().get(0).knowledgeGrounded());
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
