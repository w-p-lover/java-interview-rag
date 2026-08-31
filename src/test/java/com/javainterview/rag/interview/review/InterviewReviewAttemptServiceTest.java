package com.javainterview.rag.interview.review;

import com.javainterview.rag.interview.evaluation.InterviewAnswerAssessment;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluation;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluator;
import com.javainterview.rag.interview.preparation.InterviewFocusArea;
import com.javainterview.rag.interview.preparation.InterviewFocusSource;
import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;
import com.javainterview.rag.interview.session.InterviewSession;
import com.javainterview.rag.interview.session.InterviewSessionMode;
import com.javainterview.rag.interview.session.InterviewSessionAnswer;
import com.javainterview.rag.interview.session.InterviewSessionStatus;
import com.javainterview.rag.interview.session.InterviewSessionStore;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InterviewReviewAttemptServiceTest {

    @Test
    void storesIndependentAttemptAndComparesWithOriginalAnswer() {
        InterviewSessionStore sessionStore = mock(InterviewSessionStore.class);
        InterviewReviewTaskStore taskStore = mock(InterviewReviewTaskStore.class);
        InterviewReviewAttemptStore attemptStore = mock(InterviewReviewAttemptStore.class);
        InterviewAnswerEvaluator evaluator = mock(InterviewAnswerEvaluator.class);
        InterviewSession session = completedSession();
        InterviewReviewTask task = task();
        when(sessionStore.findById("session-1")).thenReturn(Optional.of(session));
        when(taskStore.findBySessionAndId("session-1", "task-1")).thenReturn(Optional.of(task));
        when(attemptStore.findByReviewTaskId("session-1", "task-1")).thenReturn(List.of());
        when(evaluator.evaluateForReview(eq(session.profile()), eq(session.questions().get(0)),
                eq("补充了失败补偿"), any())).thenReturn(new InterviewAnswerAssessment(
                evaluation(90), null));
        when(evaluator.promptVersion()).thenReturn("prompt-v2");
        when(evaluator.rubricVersion()).thenReturn("rubric-v1");
        when(evaluator.modelName()).thenReturn("model-test");
        InterviewReviewTaskService service = new InterviewReviewTaskService(
                sessionStore, taskStore, attemptStore, evaluator);

        InterviewReviewAttempt result = service.submitAttempt(
                "session-1", "task-1", "补充了失败补偿", 1);

        assertEquals(1, result.attemptNumber());
        assertEquals(80, result.originalOverallScore());
        assertEquals(10, result.scoreDeltaFromOriginal());
        assertEquals("prompt-v2", result.promptVersion());
        ArgumentCaptor<InterviewReviewAttempt> captor = ArgumentCaptor.forClass(InterviewReviewAttempt.class);
        verify(attemptStore).create(captor.capture());
        assertEquals(result, captor.getValue());
    }

    @Test
    void rejectsStaleAttemptNumberBeforeCallingTheModel() {
        InterviewSessionStore sessionStore = mock(InterviewSessionStore.class);
        InterviewReviewTaskStore taskStore = mock(InterviewReviewTaskStore.class);
        InterviewReviewAttemptStore attemptStore = mock(InterviewReviewAttemptStore.class);
        InterviewAnswerEvaluator evaluator = mock(InterviewAnswerEvaluator.class);
        when(sessionStore.findById("session-1")).thenReturn(Optional.of(completedSession()));
        when(taskStore.findBySessionAndId("session-1", "task-1")).thenReturn(Optional.of(task()));
        when(attemptStore.findByReviewTaskId("session-1", "task-1")).thenReturn(List.of(attempt()));
        InterviewReviewTaskService service = new InterviewReviewTaskService(
                sessionStore, taskStore, attemptStore, evaluator);

        assertThrows(InterviewReviewAttemptConflictException.class,
                () -> service.submitAttempt("session-1", "task-1", "新回答", 1));
        org.mockito.Mockito.verifyNoInteractions(evaluator);
    }

    @Test
    void summarizesPersistedAttemptsWithinOneSession() {
        InterviewSessionStore sessionStore = mock(InterviewSessionStore.class);
        InterviewReviewTaskStore taskStore = mock(InterviewReviewTaskStore.class);
        InterviewReviewAttemptStore attemptStore = mock(InterviewReviewAttemptStore.class);
        InterviewSession session = completedSession();
        InterviewReviewTask firstTask = task();
        InterviewReviewTask secondTask = new InterviewReviewTask(
                "task-2", "session-1", 1, "如何定位 MySQL 慢查询？", "工程能力",
                List.of("缺少执行计划分析"), "补充 EXPLAIN 分析。", false, List.of(),
                InterviewReviewTaskStatus.PENDING,
                LocalDateTime.of(2026, 7, 29, 9, 4), null);
        when(sessionStore.findById("session-1")).thenReturn(Optional.of(session));
        when(taskStore.findBySessionId("session-1")).thenReturn(List.of(firstTask, secondTask));
        when(attemptStore.findByReviewTaskId("session-1", "task-1"))
                .thenReturn(List.of(attempt(1, 85, List.of("缺少失败补偿细节")),
                        attempt(2, 92, List.of())));
        when(attemptStore.findByReviewTaskId("session-1", "task-2")).thenReturn(List.of());
        InterviewReviewTaskService service = new InterviewReviewTaskService(
                sessionStore, taskStore, attemptStore, mock(InterviewAnswerEvaluator.class));

        InterviewReviewSessionHistory result = service.findAttemptHistory("session-1");

        assertEquals(2, result.totalTaskCount());
        assertEquals(1, result.practicedTaskCount());
        assertEquals(2, result.totalAttemptCount());
        assertEquals(1, result.improvedTaskCount());
        InterviewReviewTaskHistory practiced = result.tasks().get(0);
        assertEquals(80, practiced.originalOverallScore());
        assertEquals(85, practiced.firstOverallScore());
        assertEquals(92, practiced.latestOverallScore());
        assertEquals(92, practiced.bestOverallScore());
        assertEquals(12, practiced.latestScoreDeltaFromOriginal());
        assertEquals(List.of(), practiced.unresolvedGaps());
        assertEquals(2, practiced.attempts().size());
        InterviewReviewTaskHistory unpracticed = result.tasks().get(1);
        assertNull(unpracticed.firstOverallScore());
        assertEquals(List.of("缺少执行计划分析"), unpracticed.unresolvedGaps());
    }

    private InterviewReviewAttempt attempt() {
        return attempt(1, 85, List.of("缺少失败补偿细节"));
    }

    private InterviewReviewAttempt attempt(int attemptNumber, int score, List<String> gaps) {
        Integer previousScore = attemptNumber == 1 ? null : 85;
        return new InterviewReviewAttempt(
                "attempt-" + attemptNumber, "session-1", "task-1", 0, attemptNumber,
                "第 " + attemptNumber + " 次复习回答", evaluation(score, gaps),
                true, List.of(reviewSource()), "prompt-v2", "rubric-v1", "model-test",
                80, previousScore, score - 80,
                previousScore == null ? null : score - previousScore,
                LocalDateTime.of(2026, 7, 30, 9, attemptNumber));
    }

    private InterviewReviewTask task() {
        return new InterviewReviewTask(
                "task-1", "session-1", 0, "如何保证向量更新一致性？", "向量生命周期",
                List.of("缺少失败补偿细节"), "补充异常分支和恢复方式。", true,
                List.of(new InterviewReviewKnowledgeSource(
                        "rag/vector-lifecycle.md", "version-1", 2,
                        List.of("向量生命周期"), 0.82)),
                InterviewReviewTaskStatus.PENDING,
                LocalDateTime.of(2026, 7, 29, 9, 3), null);
    }

    private InterviewSession completedSession() {
        LocalDateTime time = LocalDateTime.of(2026, 7, 29, 9, 0);
        InterviewSession session = new InterviewSession(
                "session-1", "Java AI 应用开发工程师", InterviewSessionMode.STANDARD,
                profile(), questions(), 3,
                List.of(), 0, InterviewSessionStatus.IN_PROGRESS, 0, null, time, time);
        session = session.answer("正式回答", evaluation(80), null, time.plusMinutes(1));
        session = session.answer("第二回答", evaluation(82), null, time.plusMinutes(2));
        return session.answer("第三回答", evaluation(84), null, time.plusMinutes(3));
    }

    private InterviewAnswerEvaluation evaluation(int overall) {
        return evaluation(overall, List.of("缺少失败补偿细节"));
    }

    private InterviewAnswerEvaluation evaluation(int overall, List<String> gaps) {
        return new InterviewAnswerEvaluation(
                overall, overall, overall, overall,
                List.of("说明了关键流程"), gaps,
                "补充异常分支和恢复方式。", true);
    }

    private List<InterviewQuestion> questions() {
        return List.of(
                new InterviewQuestion("如何保证向量更新一致性？", "向量生命周期",
                        List.of(InterviewFocusSource.RESUME), "向量更新一致性", true,
                        List.of(source())),
                question("如何定位 MySQL 慢查询？"), question("模型调用失败如何治理？"));
    }

    private InterviewQuestion question(String text) {
        return new InterviewQuestion(text, "工程能力", List.of(InterviewFocusSource.TARGET_ROLE),
                "工程能力", false, List.of());
    }

    private MarkdownSearchResult source() {
        return new MarkdownSearchResult("知识正文", "rag/vector-lifecycle.md", "version-1", 2,
                List.of("向量生命周期"), 0.82);
    }

    private InterviewReviewKnowledgeSource reviewSource() {
        return new InterviewReviewKnowledgeSource("rag/vector-lifecycle.md", "version-1", 2,
                List.of("向量生命周期"), 0.82);
    }

    private InterviewPreparationProfile profile() {
        return new InterviewPreparationProfile(
                "具备 Java 后端和 RAG 项目经验。",
                List.of("使用 Spring Boot 和 Qdrant 开发项目"),
                List.of("岗位要求向量生命周期治理"),
                List.of(new InterviewFocusArea("RAG 工程化", "目标岗位和简历涉及 RAG",
                        List.of(InterviewFocusSource.TARGET_ROLE, InterviewFocusSource.RESUME))));
    }
}
