package com.javainterview.rag.interview.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluation;
import com.javainterview.rag.interview.evaluation.InterviewKnowledgeEvidence;
import com.javainterview.rag.interview.preparation.InterviewFocusArea;
import com.javainterview.rag.interview.preparation.InterviewFocusSource;
import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;
import com.javainterview.rag.interview.session.InterviewSession;
import com.javainterview.rag.interview.session.InterviewSessionStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MySqlInterviewSessionStoreTest {

    @Test
    void persistsProfileQuestionsAndAnswersAsJsonSnapshots() {
        InterviewSessionMapper mapper = mock(InterviewSessionMapper.class);
        MySqlInterviewSessionStore store = new MySqlInterviewSessionStore(mapper, objectMapper());
        InterviewSession session = initialSession();

        store.create(session);

        ArgumentCaptor<InterviewSessionEntity> captor = ArgumentCaptor.forClass(InterviewSessionEntity.class);
        verify(mapper).insert(captor.capture());
        InterviewSessionEntity entity = captor.getValue();
        assertEquals("session-1", entity.getId());
        assertEquals("Java AI 应用开发工程师", entity.getTargetRole());
        assertTrue(entity.getProfileJson().contains("具备 Java 后端和 RAG 项目经验"));
        assertTrue(entity.getQuestionsJson().contains("如何保证向量更新一致性"));
        assertEquals(8, entity.getMaxQuestionCount());
        assertEquals("[]", entity.getAnswersJson());
        assertNull(entity.getReportJson());
        assertEquals("IN_PROGRESS", entity.getStatus());
        assertEquals(0L, entity.getVersion());
    }

    @Test
    void restoresCompletedSessionWithEvaluationsAndReport() throws Exception {
        InterviewSessionMapper mapper = mock(InterviewSessionMapper.class);
        ObjectMapper objectMapper = objectMapper();
        MySqlInterviewSessionStore store = new MySqlInterviewSessionStore(mapper, objectMapper);
        InterviewSession session = completedSession();
        InterviewSessionEntity entity = InterviewSessionEntity.from(
                session,
                objectMapper.writeValueAsString(session.profile()),
                objectMapper.writeValueAsString(session.questions()),
                objectMapper.writeValueAsString(session.answers()),
                objectMapper.writeValueAsString(session.report())
        );
        when(mapper.selectById("session-1")).thenReturn(entity);

        InterviewSession restored = store.findById("session-1").orElseThrow();

        assertEquals(session, restored);
        assertEquals(InterviewSessionStatus.COMPLETED, restored.status());
        assertEquals(3, restored.currentQuestionIndex());
        assertEquals("第 1 题回答", restored.answers().get(0).answer());
        assertEquals(80, restored.answers().get(0).evaluation().overallScore());
        assertEquals(List.of("说明了关键流程"),
                restored.answers().get(0).evaluation().answerEvidence());
        assertEquals("rag/vector-lifecycle.md",
                restored.answers().get(0).evaluation().knowledgeEvidence().get(0).source());
        assertEquals(80.0, restored.report().overallAverageScore());
    }

    @Test
    void updatesQuestionsAnswersAndReportWithTheExpectedVersion() {
        InterviewSessionMapper mapper = mock(InterviewSessionMapper.class);
        MySqlInterviewSessionStore store = new MySqlInterviewSessionStore(mapper, objectMapper());
        InterviewQuestion followUp = question("向量清理失败时如何补偿？", "失败补偿");
        InterviewSession updated = initialSession().answer(
                "第一题回答", evaluation(), followUp,
                LocalDateTime.of(2026, 7, 27, 10, 5));
        when(mapper.updateState(any(InterviewSessionEntity.class), eq(0L))).thenReturn(1);

        assertTrue(store.update(updated, 0L));

        ArgumentCaptor<InterviewSessionEntity> captor = ArgumentCaptor.forClass(InterviewSessionEntity.class);
        verify(mapper).updateState(captor.capture(), eq(0L));
        InterviewSessionEntity entity = captor.getValue();
        assertEquals(1L, entity.getVersion());
        assertEquals(1, entity.getCurrentQuestionIndex());
        assertTrue(entity.getQuestionsJson().contains("向量清理失败时如何补偿"));
        assertTrue(entity.getAnswersJson().contains("第一题回答"));
        assertTrue(entity.getAnswersJson().contains("technicalScore"));
        assertTrue(entity.getAnswersJson().contains("answerEvidence"));
        assertTrue(entity.getAnswersJson().contains("knowledgeEvidence"));
        assertTrue(entity.getAnswersJson().contains("rag/vector-lifecycle.md"));
        assertNull(entity.getReportJson());

        when(mapper.updateState(any(InterviewSessionEntity.class), eq(1L))).thenReturn(0);
        assertFalse(store.update(updated, 1L));
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    private InterviewSession initialSession() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 27, 10, 0);
        return new InterviewSession(
                "session-1",
                "Java AI 应用开发工程师",
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
    }

    private InterviewSession completedSession() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 27, 10, 0);
        InterviewSession session = initialSession();
        for (int index = 0; index < 3; index++) {
            session = session.answer(
                    "第 " + (index + 1) + " 题回答", evaluation(), null,
                    createdAt.plusMinutes(index + 1));
        }
        return session;
    }

    private InterviewAnswerEvaluation evaluation() {
        return new InterviewAnswerEvaluation(
                80, 70, 90, 80,
                List.of("说明了关键流程"),
                List.of("缺少失败补偿细节"),
                "补充异常分支和恢复方式。",
                true,
                List.of("说明了关键流程"),
                List.of(new InterviewKnowledgeEvidence(
                        "rag/vector-lifecycle.md",
                        "version-1",
                        2,
                        List.of("RAG", "向量生命周期"),
                        0.88,
                        "新版本成功后再清理旧版本"))
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
