package com.javainterview.rag.interview.question;

import com.javainterview.rag.interview.preparation.InterviewFocusArea;
import com.javainterview.rag.interview.preparation.InterviewFocusSource;
import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class InterviewQuestionPlanServiceTest {

    @Test
    void composesFixedSkillAndProfileQuestionsWithConfigurationSnapshot() {
        InterviewQuestionGenerationService generationService = mock(InterviewQuestionGenerationService.class);
        InterviewPreparationProfile profile = profile();
        List<InterviewQuestion> generatedQuestions = List.of(
                generatedQuestion("简历题 1", true),
                generatedQuestion("简历题 2", false),
                generatedQuestion("简历题 3", false));
        when(generationService.generate("Java 后端工程师", profile, 3))
                .thenReturn(new InterviewQuestionGenerationResult(
                        "Java 后端工程师", "interview-question-v1", generatedQuestions));
        InterviewQuestionPlanService service = new InterviewQuestionPlanService(
                generationService, new FixedInterviewQuestionCatalog(), "qwen-test");

        InterviewQuestionGenerationResult result = service.generate(
                "Java 后端工程师",
                profile,
                5,
                8,
                InterviewSkill.JAVA_CORE,
                InterviewDifficulty.ADVANCED,
                "interview-preparation-v1");

        assertEquals(5, result.questions().size());
        assertEquals("一次 Full GC 后延迟仍未恢复，你会如何区分内存、锁竞争和下游阻塞？",
                result.questions().get(0).question());
        assertEquals("简历题 1", result.questions().get(1).question());
        assertEquals("如何设计一个可证明不会丢任务、也不会无限重复执行的进程内异步任务组件？",
                result.questions().get(3).question());
        assertEquals(InterviewQuestionPlanService.STRATEGY_VERSION,
                result.configuration().strategyVersion());
        assertEquals(InterviewSkill.JAVA_CORE, result.configuration().skill());
        assertEquals(InterviewDifficulty.ADVANCED, result.configuration().difficulty());
        assertEquals("qwen-test", result.configuration().modelName());
        assertEquals(Map.of(
                        InterviewQuestionSource.SKILL_FIXED, 2,
                        InterviewQuestionSource.RESUME, 3),
                result.configuration().sourceQuotas());
        assertEquals(InterviewQuestionSource.SKILL_FIXED,
                result.configuration().questionAllocations().get(0).primarySource());
        assertEquals(List.of(InterviewQuestionSource.RESUME, InterviewQuestionSource.KNOWLEDGE),
                result.configuration().questionAllocations().get(1).supportingSources());
        verify(generationService).generate("Java 后端工程师", profile, 3);
    }

    @Test
    void rejectsInvalidConfigurationBeforeCallingModel() {
        InterviewQuestionGenerationService generationService = mock(InterviewQuestionGenerationService.class);
        InterviewQuestionPlanService service = new InterviewQuestionPlanService(
                generationService, new FixedInterviewQuestionCatalog(), "qwen-test");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.generate(
                        "Java 后端工程师", profile(), 5, 4,
                        InterviewSkill.JAVA_CORE, InterviewDifficulty.INTERMEDIATE,
                        "interview-preparation-v1"));

        assertEquals("整场题数上限必须在初始题数到 12 之间", exception.getMessage());
        verifyNoInteractions(generationService);
    }

    @Test
    void rejectsMissingSkillBeforeCallingModel() {
        InterviewQuestionGenerationService generationService = mock(InterviewQuestionGenerationService.class);
        InterviewQuestionPlanService service = new InterviewQuestionPlanService(
                generationService, new FixedInterviewQuestionCatalog(), "qwen-test");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.generate(
                        "Java 后端工程师", profile(), 5, 8,
                        null, InterviewDifficulty.INTERMEDIATE,
                        "interview-preparation-v1"));

        assertEquals("请选择具体的面试 Skill", exception.getMessage());
        verifyNoInteractions(generationService);
    }

    @Test
    void keepsFixedQuestionsWhenProfileQuestionGenerationFails() {
        InterviewQuestionGenerationService generationService = mock(InterviewQuestionGenerationService.class);
        InterviewPreparationProfile profile = profile();
        when(generationService.generate("Java 后端工程师", profile, 3))
                .thenThrow(new IllegalStateException("模型暂不可用"));
        InterviewQuestionPlanService service = new InterviewQuestionPlanService(
                generationService, new FixedInterviewQuestionCatalog(), "qwen-test");

        InterviewQuestionPlanOutcome result = service.generatePlan(
                "Java 后端工程师",
                profile,
                5,
                8,
                InterviewSkill.JAVA_CORE,
                InterviewDifficulty.INTERMEDIATE,
                "interview-preparation-v1");

        assertEquals("QUESTION_GENERATION_UNAVAILABLE", result.failureCode());
        assertEquals(2, result.generation().questions().size());
        assertEquals(2, result.generation().configuration().initialQuestionCount());
        assertEquals(Map.of(InterviewQuestionSource.SKILL_FIXED, 2),
                result.generation().configuration().sourceQuotas());
        assertEquals(List.of(InterviewQuestionSource.RESUME, InterviewQuestionSource.JOB_DESCRIPTION),
                result.generation().unavailableSources());
    }

    @Test
    void replacesOnlySelectedFixedQuestionWithoutCallingModelAgain() {
        InterviewQuestionGenerationService generationService = mock(InterviewQuestionGenerationService.class);
        InterviewPreparationProfile profile = profile();
        List<InterviewQuestion> generatedQuestions = List.of(
                generatedQuestion("简历题 1", false),
                generatedQuestion("简历题 2", false),
                generatedQuestion("简历题 3", false));
        when(generationService.generate("Java 后端工程师", profile, 3))
                .thenReturn(new InterviewQuestionGenerationResult(
                        "Java 后端工程师", "interview-question-v1", generatedQuestions));
        InterviewQuestionPlanService service = new InterviewQuestionPlanService(
                generationService, new FixedInterviewQuestionCatalog(), "qwen-test");
        InterviewQuestionGenerationResult current = service.generate(
                "Java 后端工程师", profile, 5, 8,
                InterviewSkill.JAVA_CORE, InterviewDifficulty.INTERMEDIATE,
                "interview-preparation-v1");

        InterviewQuestionGenerationResult updated = service.replaceQuestion(
                "Java 后端工程师", profile, current, 0);

        assertEquals("CompletableFuture 组合多个任务时如何处理线程池和异常传播？",
                updated.questions().get(0).question());
        assertEquals(current.questions().subList(1, 5), updated.questions().subList(1, 5));
        assertEquals(InterviewQuestionSource.SKILL_FIXED,
                updated.configuration().questionAllocations().get(0).primarySource());
        verify(generationService, never()).generateReplacement(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void replacesOneGeneratedQuestionAndUpdatesItsSourceAllocation() {
        InterviewQuestionGenerationService generationService = mock(InterviewQuestionGenerationService.class);
        InterviewPreparationProfile profile = profile();
        List<InterviewQuestion> generatedQuestions = List.of(
                generatedQuestion("简历题 1", false),
                generatedQuestion("简历题 2", false),
                generatedQuestion("简历题 3", false));
        when(generationService.generate("Java 后端工程师", profile, 3))
                .thenReturn(new InterviewQuestionGenerationResult(
                        "Java 后端工程师", "interview-question-v1", generatedQuestions));
        InterviewQuestion replacement = new InterviewQuestion(
                "岗位描述替换题",
                "验证岗位要求",
                List.of(InterviewFocusSource.JOB_DESCRIPTION),
                "岗位要求",
                false,
                List.of());
        when(generationService.generateReplacement(
                org.mockito.ArgumentMatchers.eq("Java 后端工程师"),
                org.mockito.ArgumentMatchers.eq(profile),
                org.mockito.ArgumentMatchers.eq(InterviewFocusSource.RESUME),
                org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(new InterviewQuestionGenerationResult(
                        "Java 后端工程师", "interview-question-v1", List.of(replacement)));
        InterviewQuestionPlanService service = new InterviewQuestionPlanService(
                generationService, new FixedInterviewQuestionCatalog(), "qwen-test");
        InterviewQuestionGenerationResult current = service.generate(
                "Java 后端工程师", profile, 5, 8,
                InterviewSkill.JAVA_CORE, InterviewDifficulty.INTERMEDIATE,
                "interview-preparation-v1");

        InterviewQuestionGenerationResult updated = service.replaceQuestion(
                "Java 后端工程师", profile, current, 1);

        assertEquals("岗位描述替换题", updated.questions().get(1).question());
        assertEquals(current.questions().get(0), updated.questions().get(0));
        assertEquals(current.questions().subList(2, 5), updated.questions().subList(2, 5));
        assertEquals(InterviewQuestionSource.JOB_DESCRIPTION,
                updated.configuration().questionAllocations().get(1).primarySource());
        assertEquals(Map.of(
                        InterviewQuestionSource.SKILL_FIXED, 2,
                        InterviewQuestionSource.RESUME, 2,
                        InterviewQuestionSource.JOB_DESCRIPTION, 1),
                updated.configuration().sourceQuotas());
        verify(generationService).generateReplacement(
                "Java 后端工程师",
                profile,
                InterviewFocusSource.RESUME,
                current.questions().stream().map(InterviewQuestion::question).toList());
    }

    private InterviewPreparationProfile profile() {
        return new InterviewPreparationProfile(
                "Java 后端候选人",
                List.of("负责 Java 服务开发"),
                List.of("要求 Java 并发经验"),
                List.of(new InterviewFocusArea(
                        "Java 并发",
                        "简历与岗位均涉及并发",
                        List.of(InterviewFocusSource.RESUME, InterviewFocusSource.JOB_DESCRIPTION))));
    }

    private InterviewQuestion generatedQuestion(String text, boolean knowledgeGrounded) {
        return new InterviewQuestion(
                text,
                "验证项目经验",
                List.of(InterviewFocusSource.RESUME),
                text,
                knowledgeGrounded,
                knowledgeGrounded ? List.of(new com.javainterview.rag.knowledge.vector.MarkdownSearchResult(
                        "知识内容", "java.md", "v1", 0, List.of("Java"), 0.88)) : List.of());
    }
}
