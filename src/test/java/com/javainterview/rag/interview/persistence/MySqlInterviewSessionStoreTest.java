package com.javainterview.rag.interview.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluation;
import com.javainterview.rag.interview.evaluation.InterviewKnowledgeEvidence;
import com.javainterview.rag.interview.preparation.InterviewFocusArea;
import com.javainterview.rag.interview.preparation.InterviewFocusSource;
import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;
import com.javainterview.rag.interview.question.InterviewDifficulty;
import com.javainterview.rag.interview.question.InterviewQuestionAllocation;
import com.javainterview.rag.interview.question.InterviewQuestionSource;
import com.javainterview.rag.interview.question.InterviewSkill;
import com.javainterview.rag.interview.question.InterviewQuestionPlanService;
import com.javainterview.rag.interview.session.InterviewConfigurationSnapshot;
import com.javainterview.rag.interview.session.InterviewSession;
import com.javainterview.rag.interview.session.InterviewSessionMode;
import com.javainterview.rag.interview.session.InterviewSessionStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

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
import static org.mockito.Mockito.doThrow;

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
        assertEquals("STANDARD", entity.getSessionMode());
        assertTrue(entity.getConfigurationJson().contains("legacy-v1"));
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
        entity.setSessionMode(null);
        when(mapper.selectById("session-1")).thenReturn(entity);

        InterviewSession restored = store.findById("session-1").orElseThrow();

        assertEquals(session, restored);
        assertEquals(InterviewSessionStatus.COMPLETED, restored.status());
        assertEquals(InterviewSessionMode.STANDARD, restored.mode());
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
    void restoresProjectDeepDiveSessionMode() throws Exception {
        InterviewSessionMapper mapper = mock(InterviewSessionMapper.class);
        ObjectMapper objectMapper = objectMapper();
        MySqlInterviewSessionStore store = new MySqlInterviewSessionStore(mapper, objectMapper);
        InterviewSession standard = initialSession();
        InterviewSession projectDeepDive = new InterviewSession(
                standard.id(), standard.targetRole(), InterviewSessionMode.PROJECT_DEEP_DIVE,
                standard.profile(), standard.questions(), standard.maxQuestionCount(),
                standard.answers(), standard.currentQuestionIndex(), standard.status(),
                standard.version(), standard.report(), standard.createdAt(), standard.updatedAt());
        InterviewSessionEntity entity = InterviewSessionEntity.from(
                projectDeepDive,
                objectMapper.writeValueAsString(projectDeepDive.profile()),
                objectMapper.writeValueAsString(projectDeepDive.questions()),
                objectMapper.writeValueAsString(projectDeepDive.answers()),
                null);
        when(mapper.selectById("session-1")).thenReturn(entity);

        InterviewSession restored = store.findById("session-1").orElseThrow();

        assertEquals(InterviewSessionMode.PROJECT_DEEP_DIVE, restored.mode());
    }

    @Test
    void persistsAndRestoresConfirmedConfigurationSnapshot() {
        InterviewSessionMapper mapper = mock(InterviewSessionMapper.class);
        MySqlInterviewSessionStore store = new MySqlInterviewSessionStore(mapper, objectMapper());
        InterviewSession legacy = initialSession();
        InterviewConfigurationSnapshot configuration = new InterviewConfigurationSnapshot(
                "deterministic-multi-source-v1",
                InterviewSessionMode.STANDARD,
                InterviewSkill.JAVA_CORE,
                InterviewDifficulty.ADVANCED,
                3,
                8,
                java.util.Map.of(
                        InterviewQuestionSource.SKILL_FIXED, 1,
                        InterviewQuestionSource.RESUME, 2),
                List.of(
                        new InterviewQuestionAllocation(0, InterviewQuestionSource.SKILL_FIXED,
                                List.of(InterviewQuestionSource.TARGET_ROLE)),
                        new InterviewQuestionAllocation(1, InterviewQuestionSource.RESUME,
                                List.of(InterviewQuestionSource.RESUME)),
                        new InterviewQuestionAllocation(2, InterviewQuestionSource.RESUME,
                                List.of(InterviewQuestionSource.RESUME))),
                "interview-preparation-v1",
                "interview-question-v1",
                "qwen-test");
        InterviewSession configured = new InterviewSession(
                legacy.id(), legacy.targetRole(), legacy.mode(), legacy.profile(), configuration,
                legacy.questions(), legacy.maxQuestionCount(), legacy.answers(),
                legacy.currentQuestionIndex(), legacy.status(), legacy.version(), legacy.report(),
                legacy.createdAt(), legacy.updatedAt());

        store.create(configured);

        ArgumentCaptor<InterviewSessionEntity> captor = ArgumentCaptor.forClass(InterviewSessionEntity.class);
        verify(mapper).insert(captor.capture());
        InterviewSessionEntity entity = captor.getValue();
        assertTrue(entity.getConfigurationJson().contains("deterministic-multi-source-v1"));
        when(mapper.selectById(configured.id())).thenReturn(entity);
        InterviewSession restored = store.findById(configured.id()).orElseThrow();
        assertEquals(configuration, restored.configuration());
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

    @Test
    void persistsRequestIdAndRestoresConcurrentWinner() throws Exception {
        InterviewSessionMapper mapper = mock(InterviewSessionMapper.class);
        ObjectMapper objectMapper = objectMapper();
        MySqlInterviewSessionStore store = new MySqlInterviewSessionStore(mapper, objectMapper);
        InterviewSession session = initialSession();
        doThrow(new DuplicateKeyException("request_id 重复"))
                .when(mapper).insert(any(InterviewSessionEntity.class));
        InterviewSessionEntity existing = InterviewSessionEntity.from(
                session, "request-1",
                objectMapper.writeValueAsString(session.configuration()),
                objectMapper.writeValueAsString(session.profile()),
                objectMapper.writeValueAsString(session.questions()),
                objectMapper.writeValueAsString(session.answers()),
                null);
        when(mapper.selectByRequestId("request-1")).thenReturn(existing);

        InterviewSession result = store.createIfAbsent(session, "request-1").orElseThrow();

        assertEquals(session, result);
        ArgumentCaptor<InterviewSessionEntity> captor = ArgumentCaptor.forClass(InterviewSessionEntity.class);
        verify(mapper).insert(captor.capture());
        assertEquals("request-1", captor.getValue().getRequestId());
    }

    @Test
    void loadsRecentHistoricalQuestionsForTheSameSkill() throws Exception {
        InterviewSessionMapper mapper = mock(InterviewSessionMapper.class);
        ObjectMapper objectMapper = objectMapper();
        MySqlInterviewSessionStore store = new MySqlInterviewSessionStore(mapper, objectMapper);
        InterviewSession legacy = initialSession();
        InterviewConfigurationSnapshot configuration = new InterviewConfigurationSnapshot(
                InterviewQuestionPlanService.STRATEGY_VERSION,
                InterviewSessionMode.STANDARD,
                InterviewSkill.JAVA_CORE,
                InterviewDifficulty.INTERMEDIATE,
                3, 8,
                java.util.Map.of(InterviewQuestionSource.RESUME, 3),
                List.of(
                        new InterviewQuestionAllocation(0, InterviewQuestionSource.RESUME, List.of()),
                        new InterviewQuestionAllocation(1, InterviewQuestionSource.RESUME, List.of()),
                        new InterviewQuestionAllocation(2, InterviewQuestionSource.RESUME, List.of())),
                "interview-preparation-v1", "interview-question-v1", "qwen-test");
        InterviewSession configured = new InterviewSession(
                legacy.id(), legacy.targetRole(), legacy.mode(), legacy.profile(), configuration,
                legacy.questions(), legacy.maxQuestionCount(), legacy.answers(),
                legacy.currentQuestionIndex(), legacy.status(), legacy.version(), legacy.report(),
                legacy.createdAt(), legacy.updatedAt());
        InterviewSessionEntity entity = InterviewSessionEntity.from(
                configured, null,
                objectMapper.writeValueAsString(configuration),
                objectMapper.writeValueAsString(configured.profile()),
                objectMapper.writeValueAsString(configured.questions()),
                objectMapper.writeValueAsString(configured.answers()), null);
        when(mapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(entity));

        var history = store.findRecentQuestions(InterviewSkill.JAVA_CORE, 10);

        assertEquals(3, history.size());
        assertEquals("RESUME", history.get(0).type());
        assertEquals("向量生命周期", history.get(0).topicSummary());
    }

    private ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    private InterviewSession initialSession() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 7, 27, 10, 0);
        return new InterviewSession(
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
