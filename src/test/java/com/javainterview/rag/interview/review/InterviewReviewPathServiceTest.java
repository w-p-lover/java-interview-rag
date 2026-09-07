package com.javainterview.rag.interview.review;

import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluation;
import com.javainterview.rag.interview.session.InterviewSession;
import com.javainterview.rag.interview.session.InterviewSessionStore;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InterviewReviewPathServiceTest {

    @Test
    void ordersUnpracticedAndWeakTasksWithDeterministicIntervals() {
        InterviewSessionStore sessionStore = mock(InterviewSessionStore.class);
        InterviewReviewTaskStore taskStore = mock(InterviewReviewTaskStore.class);
        InterviewReviewAttemptStore attemptStore = mock(InterviewReviewAttemptStore.class);
        when(sessionStore.findById("session-1")).thenReturn(Optional.of(mock(InterviewSession.class)));
        when(sessionStore.findById("session-2")).thenReturn(Optional.of(mock(InterviewSession.class)));
        InterviewReviewTask unpracticed = task("task-1", "session-1", "线程池调度", null);
        InterviewReviewTask improved = task("task-2", "session-2", "向量一致性", null);
        when(taskStore.findBySessionIds(List.of("session-1", "session-2")))
                .thenReturn(List.of(improved, unpracticed));
        when(attemptStore.findByReviewTaskId("session-1", "task-1")).thenReturn(List.of());
        when(attemptStore.findByReviewTaskId("session-2", "task-2"))
                .thenReturn(List.of(attempt("session-2", "task-2", 82, List.of(),
                        LocalDateTime.of(2026, 9, 5, 10, 0))));
        InterviewReviewPathService service = new InterviewReviewPathService(
                sessionStore, taskStore, attemptStore);

        InterviewReviewPathResult result = service.recommend(
                List.of("session-1", "session-2"), LocalDate.of(2026, 9, 7));

        assertEquals(2, result.taskCount());
        assertEquals("task-1", result.recommendations().get(0).taskId());
        assertEquals(LocalDate.of(2026, 9, 7), result.recommendations().get(0).recommendedReviewDate());
        assertEquals(1, result.recommendations().get(0).intervalDays());
        assertEquals("task-2", result.recommendations().get(1).taskId());
        assertEquals(LocalDate.of(2026, 9, 8), result.recommendations().get(1).recommendedReviewDate());
        assertEquals(3, result.recommendations().get(1).intervalDays());
    }

    @Test
    void rejectsMoreThanEightSessions() {
        InterviewReviewPathService service = new InterviewReviewPathService(
                mock(InterviewSessionStore.class), mock(InterviewReviewTaskStore.class),
                mock(InterviewReviewAttemptStore.class));

        assertThrows(IllegalArgumentException.class,
                () -> service.recommend(List.of("1", "2", "3", "4", "5", "6", "7", "8", "9"),
                        LocalDate.of(2026, 9, 7)));
    }

    private InterviewReviewTask task(String id, String sessionId, String objective,
                                     LocalDate plannedReviewDate) {
        return new InterviewReviewTask(
                id, sessionId, 0, "如何掌握" + objective + "？", objective,
                List.of("需要补充关键边界"), "补充异常分支和验证方式。", false, List.of(),
                InterviewReviewTaskStatus.PENDING, plannedReviewDate,
                LocalDateTime.of(2026, 9, 1, 10, 0), null);
    }

    private InterviewReviewAttempt attempt(String sessionId, String taskId, int score,
                                            List<String> gaps, LocalDateTime createdAt) {
        return new InterviewReviewAttempt(
                "attempt-" + taskId, sessionId, taskId, 0, 1, "复习回答",
                new InterviewAnswerEvaluation(score, score, score, score,
                        List.of("说明了流程"), gaps, "继续补充边界。", true),
                false, List.of(), "prompt-v1", "rubric-v1", "model-test",
                70, null, score - 70, null, createdAt);
    }
}
