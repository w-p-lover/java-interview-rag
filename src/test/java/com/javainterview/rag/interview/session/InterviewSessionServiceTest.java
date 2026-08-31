package com.javainterview.rag.interview.session;

import com.javainterview.rag.interview.evaluation.InterviewAnswerAssessment;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluation;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluator;
import com.javainterview.rag.interview.preparation.InterviewFocusArea;
import com.javainterview.rag.interview.preparation.InterviewFocusSource;
import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InterviewSessionServiceTest {

    @Test
    void startsAnInProgressSessionWithConfirmedProfileAndQuestions() {
        InterviewSessionStore store = mock(InterviewSessionStore.class);
        InterviewAnswerEvaluator evaluator = mock(InterviewAnswerEvaluator.class);
        InterviewSessionService service = new InterviewSessionService(store, evaluator);

        InterviewSession session = service.start(" Java AI 应用开发工程师 ", profile(), questions());

        assertEquals("Java AI 应用开发工程师", session.targetRole());
        assertEquals(InterviewSessionMode.STANDARD, session.mode());
        assertEquals(InterviewSessionStatus.IN_PROGRESS, session.status());
        assertEquals(0, session.currentQuestionIndex());
        assertEquals(0L, session.version());
        assertEquals(List.of(), session.answers());
        assertEquals(3, session.questions().size());
        assertEquals(8, session.maxQuestionCount());
        assertNull(session.report());
        assertNotNull(session.id());
        assertNotNull(session.createdAt());
        assertEquals(session.createdAt(), session.updatedAt());
        verify(store).create(session);
        verifyNoInteractions(evaluator);
    }

    @Test
    void submitsAnswersAndBuildsTheReportWhenTheQuestionQueueIsExhausted() {
        InterviewSessionStore store = mock(InterviewSessionStore.class);
        InterviewAnswerEvaluator evaluator = mock(InterviewAnswerEvaluator.class);
        InterviewSessionService service = new InterviewSessionService(store, evaluator);
        InterviewSession initial = sessionWithAnswers(0);
        InterviewSession afterTwoAnswers = sessionWithAnswers(2);
        when(store.findById("session-1"))
                .thenReturn(Optional.of(initial))
                .thenReturn(Optional.of(afterTwoAnswers));
        when(evaluator.evaluate(any(), any(), any())).thenReturn(assessment(null));
        when(store.update(any(InterviewSession.class), eq(0L))).thenReturn(true);
        when(store.update(any(InterviewSession.class), eq(2L))).thenReturn(true);

        InterviewSession afterFirst = service.submitAnswer("session-1", 0, 0L, "  第一题回答  ");
        InterviewSession completed = service.submitAnswer("session-1", 2, 2L, "第三题回答");

        assertEquals(1, afterFirst.currentQuestionIndex());
        assertEquals(1L, afterFirst.version());
        assertEquals("第一题回答", afterFirst.answers().get(0).answer());
        assertEquals(InterviewSessionStatus.IN_PROGRESS, afterFirst.status());
        assertEquals(3, completed.currentQuestionIndex());
        assertEquals(3L, completed.version());
        assertEquals(InterviewSessionStatus.COMPLETED, completed.status());
        assertEquals("第三题回答", completed.answers().get(2).answer());
        assertNotNull(completed.report());
        assertEquals(3, completed.report().answeredQuestions());
        assertEquals(80.0, completed.report().overallAverageScore());
    }

    @Test
    void appendsOneNonDuplicateFollowUpWithinTheConfiguredCap() {
        InterviewSessionStore store = mock(InterviewSessionStore.class);
        InterviewAnswerEvaluator evaluator = mock(InterviewAnswerEvaluator.class);
        InterviewSessionService service = new InterviewSessionService(store, evaluator);
        InterviewSession initial = sessionWithAnswers(0);
        InterviewQuestion followUp = question("如果向量清理失败，你会怎样补偿？", "失败补偿");
        when(store.findById("session-1")).thenReturn(Optional.of(initial));
        when(evaluator.evaluate(any(), any(), eq("回答"))).thenReturn(assessment(followUp));
        when(store.update(any(InterviewSession.class), eq(0L))).thenReturn(true);

        InterviewSession updated = service.submitAnswer("session-1", 0, 0L, "回答");

        assertEquals(4, updated.questions().size());
        assertEquals(followUp, updated.questions().get(3));
        assertEquals(InterviewSessionStatus.IN_PROGRESS, updated.status());
    }

    @Test
    void rejectsCompletedStaleAndConcurrentSubmissionsBeforeTheyCanOverwriteAnswers() {
        InterviewSessionStore store = mock(InterviewSessionStore.class);
        InterviewAnswerEvaluator evaluator = mock(InterviewAnswerEvaluator.class);
        InterviewSessionService service = new InterviewSessionService(store, evaluator);
        InterviewSession completed = sessionWithAnswers(3);
        when(store.findById("completed")).thenReturn(Optional.of(completed));

        InterviewSessionConflictException completedException = assertThrows(
                InterviewSessionConflictException.class,
                () -> service.submitAnswer("completed", 3, 3L, "额外回答"));

        assertEquals("面试会话已经完成", completedException.getMessage());
        verifyNoInteractions(evaluator);

        InterviewSession initial = sessionWithAnswers(0);
        when(store.findById("session-1")).thenReturn(Optional.of(initial));
        InterviewSessionConflictException staleException = assertThrows(
                InterviewSessionConflictException.class,
                () -> service.submitAnswer("session-1", 1, 0L, "延迟回答"));
        assertEquals("回答请求已过期，请刷新后重试", staleException.getMessage());
        verifyNoInteractions(evaluator);

        when(evaluator.evaluate(any(), any(), eq("并发回答"))).thenReturn(assessment(null));
        when(store.update(any(InterviewSession.class), eq(0L))).thenReturn(false);
        InterviewSessionConflictException conflictException = assertThrows(
                InterviewSessionConflictException.class,
                () -> service.submitAnswer("session-1", 0, 0L, "并发回答"));
        assertEquals("面试会话已被并发更新，请刷新后重试", conflictException.getMessage());
        verify(store, never()).update(eq(completed), eq(3L));
    }

    @Test
    void reportsMissingSessionsAndRejectsInvalidAnswers() {
        InterviewSessionStore store = mock(InterviewSessionStore.class);
        InterviewAnswerEvaluator evaluator = mock(InterviewAnswerEvaluator.class);
        InterviewSessionService service = new InterviewSessionService(store, evaluator);
        when(store.findById("missing")).thenReturn(Optional.empty());

        assertEquals("面试会话不存在",
                assertThrows(NoSuchElementException.class, () -> service.find("missing")).getMessage());
        assertEquals("回答不能为空",
                assertThrows(IllegalArgumentException.class,
                        () -> service.submitAnswer("session-1", 0, 0L, " ")).getMessage());
    }

    private InterviewSession sessionWithAnswers(int answerCount) {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 27, 10, 0);
        InterviewSession session = new InterviewSession(
                "session-1",
                "Java AI 应用开发工程师",
                InterviewSessionMode.STANDARD,
                profile(),
                questions(),
                8,
                List.of(),
                0,
                InterviewSessionStatus.IN_PROGRESS,
                0L,
                null,
                createdAt,
                createdAt
        );
        for (int index = 0; index < answerCount; index++) {
            session = session.answer(
                    "第 " + (index + 1) + " 题回答", evaluation(), null,
                    createdAt.plusMinutes(index + 1));
        }
        return session;
    }

    private InterviewAnswerAssessment assessment(InterviewQuestion followUp) {
        return new InterviewAnswerAssessment(evaluation(), followUp);
    }

    private InterviewAnswerEvaluation evaluation() {
        return new InterviewAnswerEvaluation(
                80, 70, 90, 80,
                List.of("说明了关键流程"),
                List.of("缺少失败补偿细节"),
                "补充异常分支和恢复方式。",
                false
        );
    }

    private InterviewPreparationProfile profile() {
        return new InterviewPreparationProfile(
                "具备 Java 后端和 RAG 项目经验。",
                List.of("使用 Spring Boot 和 Qdrant 开发项目"),
                List.of("岗位要求 MySQL 调优"),
                List.of(new InterviewFocusArea(
                        "RAG 工程化",
                        "目标岗位、简历和 JD 均涉及 RAG",
                        List.of(InterviewFocusSource.TARGET_ROLE,
                                InterviewFocusSource.RESUME,
                                InterviewFocusSource.JOB_DESCRIPTION)))
        );
    }

    private List<InterviewQuestion> questions() {
        return List.of(
                question("如何保证向量更新一致性？", "向量生命周期"),
                question("如何定位 MySQL 慢查询？", "MySQL 调优"),
                question("模型调用失败如何治理？", "AI 工程治理")
        );
    }

    private InterviewQuestion question(String text, String objective) {
        return new InterviewQuestion(
                text,
                objective,
                List.of(InterviewFocusSource.TARGET_ROLE, InterviewFocusSource.RESUME),
                objective,
                false,
                List.of()
        );
    }
}
