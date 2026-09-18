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
