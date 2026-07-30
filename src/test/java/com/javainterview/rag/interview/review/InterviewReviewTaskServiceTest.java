package com.javainterview.rag.interview.review;

import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluation;
import com.javainterview.rag.interview.preparation.InterviewFocusArea;
import com.javainterview.rag.interview.preparation.InterviewFocusSource;
import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;
import com.javainterview.rag.interview.session.InterviewSession;
import com.javainterview.rag.interview.session.InterviewSessionStatus;
import com.javainterview.rag.interview.session.InterviewSessionStore;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

class InterviewReviewTaskServiceTest {

    @Test
    void generatesDeterministicTasksOnlyForAnswersWithGaps() {
        InterviewSessionStore sessionStore = mock(InterviewSessionStore.class);
        InterviewReviewTaskStore taskStore = mock(InterviewReviewTaskStore.class);
        InterviewSession completed = completedSession();
        when(sessionStore.findById("session-1")).thenReturn(Optional.of(completed));
        when(taskStore.findBySessionId("session-1")).thenReturn(List.of());
        InterviewReviewTaskService service = new InterviewReviewTaskService(sessionStore, taskStore);

        service.generate("session-1");
        service.generate("session-1");

        ArgumentCaptor<InterviewReviewTask> captor = ArgumentCaptor.forClass(InterviewReviewTask.class);
        verify(taskStore, org.mockito.Mockito.times(4)).createIfAbsent(captor.capture());
        List<InterviewReviewTask> tasks = captor.getAllValues();
        assertEquals(0, tasks.get(0).questionIndex());
        assertEquals(2, tasks.get(1).questionIndex());
        assertEquals(tasks.get(0).id(), tasks.get(2).id());
        assertEquals(tasks.get(1).id(), tasks.get(3).id());
        assertEquals(List.of("缺少失败补偿细节"), tasks.get(0).gaps());
        assertEquals("rag/vector-lifecycle.md", tasks.get(0).knowledgeSources().get(0).source());
        assertFalse(tasks.get(1).knowledgeGrounded());
        assertEquals(List.of(), tasks.get(1).knowledgeSources());
    }

    @Test
    void rejectsReviewGenerationBeforeSessionCompletion() {
        InterviewSessionStore sessionStore = mock(InterviewSessionStore.class);
        InterviewReviewTaskStore taskStore = mock(InterviewReviewTaskStore.class);
        when(sessionStore.findById("session-1")).thenReturn(Optional.of(initialSession()));
        InterviewReviewTaskService service = new InterviewReviewTaskService(sessionStore, taskStore);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> service.generate("session-1"));

        assertEquals("面试会话尚未完成，不能生成复习任务", exception.getMessage());
        verify(taskStore, never()).createIfAbsent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void completesAnExistingReviewTask() {
        InterviewSessionStore sessionStore = mock(InterviewSessionStore.class);
        InterviewReviewTaskStore taskStore = mock(InterviewReviewTaskStore.class);
        when(sessionStore.findById("session-1")).thenReturn(Optional.of(completedSession()));
        InterviewReviewTask completedTask = reviewTask(
                InterviewReviewTaskStatus.COMPLETED,
                LocalDateTime.of(2026, 7, 30, 9, 30));
        when(taskStore.complete(org.mockito.ArgumentMatchers.eq("session-1"),
                org.mockito.ArgumentMatchers.eq("task-1"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(completedTask));
        InterviewReviewTaskService service = new InterviewReviewTaskService(sessionStore, taskStore);

        InterviewReviewTask result = service.complete("session-1", "task-1");

        assertEquals(completedTask, result);
    }

    private InterviewSession completedSession() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 29, 9, 0);
        InterviewSession session = initialSession();
        session = session.answer("第一题回答", evaluation(List.of("缺少失败补偿细节")), null,
                createdAt.plusMinutes(1));
        session = session.answer("第二题回答", evaluation(List.of()), null,
                createdAt.plusMinutes(2));
        return session.answer("第三题回答", evaluation(List.of("缺少重试边界")), null,
                createdAt.plusMinutes(3));
    }

    private InterviewSession initialSession() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 29, 9, 0);
        return new InterviewSession(
                "session-1",
                "Java AI 应用开发工程师",
                profile(),
                questions(),
                3,
                List.of(),
                0,
                InterviewSessionStatus.IN_PROGRESS,
                0L,
                null,
                createdAt,
                createdAt
        );
    }

    private InterviewAnswerEvaluation evaluation(List<String> gaps) {
        return new InterviewAnswerEvaluation(
                80, 70, 90, 80,
                List.of("说明了关键流程"),
                gaps,
                "补充异常分支和恢复方式。",
                true
        );
    }

    private InterviewReviewTask reviewTask(InterviewReviewTaskStatus status,
                                           LocalDateTime completedAt) {
        return new InterviewReviewTask(
                "task-1",
                "session-1",
                0,
                "如何保证向量更新一致性？",
                "向量生命周期",
                List.of("缺少失败补偿细节"),
                "补充异常分支和恢复方式。",
                false,
                List.of(),
                status,
                LocalDateTime.of(2026, 7, 29, 9, 3),
                completedAt
        );
    }

    private InterviewPreparationProfile profile() {
        return new InterviewPreparationProfile(
                "具备 Java 后端和 RAG 项目经验。",
                List.of("使用 Spring Boot 和 Qdrant 开发项目"),
                List.of("岗位要求 MySQL 调优"),
                List.of(new InterviewFocusArea(
                        "RAG 工程化",
                        "目标岗位和简历涉及 RAG",
                        List.of(InterviewFocusSource.TARGET_ROLE, InterviewFocusSource.RESUME)))
        );
    }

    private List<InterviewQuestion> questions() {
        return List.of(
                new InterviewQuestion(
                        "如何保证向量更新一致性？",
                        "向量生命周期",
                        List.of(InterviewFocusSource.RESUME),
                        "向量更新一致性",
                        true,
                        List.of(new MarkdownSearchResult(
                                "知识正文不会进入复习任务",
                                "rag/vector-lifecycle.md",
                                "version-1",
                                2,
                                List.of("向量生命周期", "更新"),
                                0.82))
                ),
                question("如何定位 MySQL 慢查询？", "MySQL 调优"),
                question("模型调用失败如何治理？", "AI 工程治理")
        );
    }

    private InterviewQuestion question(String text, String objective) {
        return new InterviewQuestion(
                text,
                objective,
                List.of(InterviewFocusSource.TARGET_ROLE),
                objective,
                false,
                List.of()
        );
    }
}
