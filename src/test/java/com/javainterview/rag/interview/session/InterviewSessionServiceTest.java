package com.javainterview.rag.interview.session;

import com.javainterview.rag.interview.preparation.InterviewFocusArea;
import com.javainterview.rag.interview.preparation.InterviewFocusSource;
import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterviewSessionServiceTest {

    @Test
    void startsAnInProgressSessionWithConfirmedProfileAndQuestions() {
        InterviewSessionStore store = mock(InterviewSessionStore.class);
        InterviewSessionService service = new InterviewSessionService(store);

        InterviewSession session = service.start(" Java AI 应用开发工程师 ", profile(), questions());

        assertEquals("Java AI 应用开发工程师", session.targetRole());
        assertEquals(InterviewSessionStatus.IN_PROGRESS, session.status());
        assertEquals(0, session.currentQuestionIndex());
        assertEquals(0L, session.version());
        assertEquals(List.of(), session.answers());
        assertEquals(3, session.questions().size());
        assertNotNull(session.id());
        assertNotNull(session.createdAt());
        assertEquals(session.createdAt(), session.updatedAt());
        verify(store).create(session);
    }

    @Test
    void submitsTheCurrentAnswerAndCompletesAfterTheThirdQuestion() {
        InterviewSessionStore store = mock(InterviewSessionStore.class);
        InterviewSessionService service = new InterviewSessionService(store);
        InterviewSession initial = sessionWithAnswers(0);
        InterviewSession afterTwoAnswers = sessionWithAnswers(2);
        when(store.findById("session-1"))
                .thenReturn(Optional.of(initial))
                .thenReturn(Optional.of(afterTwoAnswers));
        when(store.update(any(InterviewSession.class), eq(0L))).thenReturn(true);
        when(store.update(any(InterviewSession.class), eq(2L))).thenReturn(true);

        InterviewSession afterFirst = service.submitAnswer("session-1", "  第一题回答  ");
        InterviewSession completed = service.submitAnswer("session-1", "第三题回答");

        assertEquals(1, afterFirst.currentQuestionIndex());
        assertEquals(1L, afterFirst.version());
        assertEquals("第一题回答", afterFirst.answers().get(0).answer());
        assertEquals(InterviewSessionStatus.IN_PROGRESS, afterFirst.status());
        assertEquals(3, completed.currentQuestionIndex());
        assertEquals(3L, completed.version());
        assertEquals(InterviewSessionStatus.COMPLETED, completed.status());
        assertEquals("第三题回答", completed.answers().get(2).answer());
    }

    @Test
    void rejectsAnswersAfterCompletionAndConcurrentUpdates() {
        InterviewSessionStore store = mock(InterviewSessionStore.class);
        InterviewSessionService service = new InterviewSessionService(store);
        InterviewSession completed = sessionWithAnswers(3);
        when(store.findById("completed")).thenReturn(Optional.of(completed));

        IllegalStateException completedException = assertThrows(IllegalStateException.class,
                () -> service.submitAnswer("completed", "额外回答"));

        assertEquals("面试会话已经完成", completedException.getMessage());
        verify(store, never()).update(eq(completed), anyLong());

        InterviewSession initial = sessionWithAnswers(0);
        when(store.findById("session-1")).thenReturn(Optional.of(initial));
        when(store.update(any(InterviewSession.class), eq(0L))).thenReturn(false);

        IllegalStateException conflictException = assertThrows(IllegalStateException.class,
                () -> service.submitAnswer("session-1", "并发回答"));

        assertEquals("面试会话已被并发更新，请刷新后重试", conflictException.getMessage());
    }

    @Test
    void reportsMissingSessionsAndRejectsInvalidAnswers() {
        InterviewSessionStore store = mock(InterviewSessionStore.class);
        InterviewSessionService service = new InterviewSessionService(store);
        when(store.findById("missing")).thenReturn(Optional.empty());

        assertEquals("面试会话不存在",
                assertThrows(NoSuchElementException.class, () -> service.find("missing")).getMessage());
        assertEquals("回答不能为空",
                assertThrows(IllegalArgumentException.class,
                        () -> service.submitAnswer("session-1", " ")).getMessage());
    }

    private InterviewSession sessionWithAnswers(int answerCount) {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 27, 10, 0);
        InterviewSession session = new InterviewSession(
                "session-1",
                "Java AI 应用开发工程师",
                profile(),
                questions(),
                List.of(),
                0,
                InterviewSessionStatus.IN_PROGRESS,
                0L,
                createdAt,
                createdAt
        );
        for (int index = 0; index < answerCount; index++) {
            session = session.answer("第 " + (index + 1) + " 题回答", createdAt.plusMinutes(index + 1));
        }
        return session;
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
