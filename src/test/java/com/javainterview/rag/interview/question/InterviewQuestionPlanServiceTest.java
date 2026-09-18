package com.javainterview.rag.interview.question;

import com.javainterview.rag.interview.preparation.InterviewFocusArea;
import com.javainterview.rag.interview.preparation.InterviewFocusSource;
import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.session.InterviewConfigurationSnapshot;
import com.javainterview.rag.interview.session.InterviewSessionMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InterviewQuestionPlanServiceTest {

    @Test
    void composesProfileAndDirectionQuestionsWithConfigurationSnapshot() {
        InterviewQuestionGenerationService generationService = mock(InterviewQuestionGenerationService.class);
        InterviewPreparationProfile profile = profile();
        List<InterviewQuestion> questions = List.of(
                generatedQuestion("简历题 1", InterviewFocusSource.RESUME, true),
                generatedQuestion("简历题 2", InterviewFocusSource.RESUME, false),
                generatedQuestion("简历题 3", InterviewFocusSource.RESUME, false),
                generatedQuestion("方向题 1", InterviewFocusSource.TARGET_ROLE, false),
                generatedQuestion("方向题 2", InterviewFocusSource.JOB_DESCRIPTION, false));
        when(generationService.generateBySources(
                "Java 后端工程师", profile, 5, List.of()))
                .thenReturn(new InterviewQuestionSourceGenerationResult(
                        "Java 后端工程师", "interview-question-v1", questions, List.of()));
        InterviewQuestionPlanService service = service(generationService);

        InterviewQuestionGenerationResult result = service.generate(
                "Java 后端工程师", profile, 5, 8,
                InterviewSkill.JAVA_CORE, InterviewDifficulty.ADVANCED,
                "interview-preparation-v1");

        assertEquals(5, result.questions().size());
        assertEquals(InterviewQuestionPlanService.STRATEGY_VERSION,
                result.configuration().strategyVersion());
        assertEquals(Map.of(
                        InterviewQuestionSource.RESUME, 3,
                        InterviewQuestionSource.TARGET_ROLE, 1,
                        InterviewQuestionSource.JOB_DESCRIPTION, 1),
                result.configuration().sourceQuotas());
        assertEquals(List.of(InterviewQuestionSource.RESUME, InterviewQuestionSource.KNOWLEDGE),
                result.configuration().questionAllocations().get(0).supportingSources());
    }

    @Test
    void removesSameSessionAndRecentHistoryDuplicatesThenFillsWithSkillFallback() {
        InterviewQuestionGenerationService generationService = mock(InterviewQuestionGenerationService.class);
        InterviewPreparationProfile profile = profile();
        List<HistoricalInterviewQuestion> history = List.of(
                new HistoricalInterviewQuestion("历史题？", "RESUME", "历史知识点"));
        List<InterviewQuestion> questions = List.of(
                generatedQuestion("重复题？", InterviewFocusSource.RESUME, false),
                generatedQuestion(" 重复题  ", InterviewFocusSource.RESUME, false),
                generatedQuestion("历史题", InterviewFocusSource.RESUME, false),
                generatedQuestion("方向题 1", InterviewFocusSource.TARGET_ROLE, false),
                generatedQuestion("方向题 2", InterviewFocusSource.JOB_DESCRIPTION, false));
        when(generationService.generateBySources("Java 后端工程师", profile, 5, history))
                .thenReturn(new InterviewQuestionSourceGenerationResult(
                        "Java 后端工程师", "interview-question-v1", questions, List.of()));
        InterviewQuestionPlanService service = service(generationService);

        InterviewQuestionPlanOutcome result = service.generatePlan(
                "Java 后端工程师", profile, 5, 8,
                InterviewSkill.JAVA_CORE, InterviewDifficulty.INTERMEDIATE,
                "interview-preparation-v1", history);

        assertEquals(5, result.generation().questions().size());
        assertEquals(1, result.generation().questions().stream()
                .filter(question -> question.question().contains("重复题")).count());
        assertEquals(0, result.generation().questions().stream()
                .filter(question -> question.question().contains("历史题")).count());
        assertEquals(2, result.generation().configuration().sourceQuotas()
                .get(InterviewQuestionSource.SKILL_FIXED));
    }

    @Test
    void fillsAllQuestionsFromSkillFallbackWhenBothModelSourcesFail() {
        InterviewQuestionGenerationService generationService = mock(InterviewQuestionGenerationService.class);
        when(generationService.generateBySources(anyString(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(), anyList()))
                .thenReturn(new InterviewQuestionSourceGenerationResult(
                        "Java 后端工程师", "interview-question-v1", List.of(),
                        List.of(InterviewQuestionSource.RESUME, InterviewQuestionSource.TARGET_ROLE)));
        InterviewQuestionPlanService service = service(generationService);

        InterviewQuestionPlanOutcome result = service.generatePlan(
                "Java 后端工程师", profile(), 5, 8,
                InterviewSkill.JAVA_CORE, InterviewDifficulty.INTERMEDIATE,
                "interview-preparation-v1");

        assertEquals("QUESTION_SOURCE_DEGRADED", result.failureCode());
        assertEquals(5, result.generation().questions().size());
        assertEquals(Map.of(InterviewQuestionSource.SKILL_FIXED, 5),
                result.generation().configuration().sourceQuotas());
    }

    @Test
    void rejectsInvalidConfigurationBeforeCallingModel() {
        InterviewQuestionGenerationService generationService = mock(InterviewQuestionGenerationService.class);
        InterviewQuestionPlanService service = service(generationService);

        assertEquals("整场题数上限必须在初始题数到 12 之间",
                assertThrows(IllegalArgumentException.class, () -> service.generate(
                        "Java 后端工程师", profile(), 5, 4,
                        InterviewSkill.JAVA_CORE, InterviewDifficulty.INTERMEDIATE,
                        "interview-preparation-v1")).getMessage());
        verifyNoInteractions(generationService);
    }

    @Test
    void replacesOnlySelectedFixedQuestionWithoutCallingModelAgain() {
        InterviewQuestionGenerationService generationService = mock(InterviewQuestionGenerationService.class);
        InterviewQuestionPlanService service = service(generationService);
        InterviewQuestion fixed = new FixedInterviewQuestionCatalog()
                .select(InterviewSkill.JAVA_CORE, InterviewDifficulty.INTERMEDIATE, 1).get(0);
        InterviewQuestion generated = generatedQuestion("简历题", InterviewFocusSource.RESUME, false);
        InterviewConfigurationSnapshot configuration = new InterviewConfigurationSnapshot(
                InterviewQuestionPlanService.STRATEGY_VERSION, InterviewSessionMode.STANDARD,
                InterviewSkill.JAVA_CORE, InterviewDifficulty.INTERMEDIATE, 3, 8,
                Map.of(InterviewQuestionSource.SKILL_FIXED, 1, InterviewQuestionSource.RESUME, 2),
                List.of(
                        new InterviewQuestionAllocation(0, InterviewQuestionSource.SKILL_FIXED,
                                List.of(InterviewQuestionSource.TARGET_ROLE)),
                        new InterviewQuestionAllocation(1, InterviewQuestionSource.RESUME,
                                List.of(InterviewQuestionSource.RESUME)),
                        new InterviewQuestionAllocation(2, InterviewQuestionSource.RESUME,
                                List.of(InterviewQuestionSource.RESUME))),
                "interview-preparation-v1", "interview-question-v1", "qwen-test");
        InterviewQuestionGenerationResult current = new InterviewQuestionGenerationResult(
                "Java 后端工程师", "interview-question-v1",
                List.of(fixed, generated, generated), configuration);

        InterviewQuestionGenerationResult updated = service.replaceQuestion(
                "Java 后端工程师", profile(), current, 0);

        assertEquals("请说明 AQS 的状态、队列和唤醒流程如何协作。",
                updated.questions().get(0).question());
        assertEquals(current.questions().subList(1, 3), updated.questions().subList(1, 3));
        verify(generationService, never()).generateReplacement(
                anyString(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), anyList());
    }

    private InterviewQuestionPlanService service(InterviewQuestionGenerationService generationService) {
        return new InterviewQuestionPlanService(
                generationService, new FixedInterviewQuestionCatalog(), "qwen-test");
    }

    private InterviewPreparationProfile profile() {
        return new InterviewPreparationProfile(
                "Java 后端候选人",
                List.of("负责 Java 服务开发"),
                List.of("要求 Java 并发经验"),
                List.of(new InterviewFocusArea(
                        "Java 并发", "简历与岗位均涉及并发",
                        List.of(InterviewFocusSource.TARGET_ROLE,
                                InterviewFocusSource.RESUME,
                                InterviewFocusSource.JOB_DESCRIPTION))));
    }

    private InterviewQuestion generatedQuestion(String text,
                                                InterviewFocusSource source,
                                                boolean knowledgeGrounded) {
        return new InterviewQuestion(
                text, "验证项目经验", List.of(source), text, knowledgeGrounded,
                knowledgeGrounded ? List.of(new com.javainterview.rag.knowledge.vector.MarkdownSearchResult(
                        "知识内容", "java.md", "v1", 0, List.of("Java"), 0.88)) : List.of());
    }
}
