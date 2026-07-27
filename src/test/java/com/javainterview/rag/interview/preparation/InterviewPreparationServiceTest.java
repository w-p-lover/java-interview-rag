package com.javainterview.rag.interview.preparation;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InterviewPreparationServiceTest {

    @Test
    void supportsSpringAiStructuredOutputConversion() {
        BeanOutputConverter<InterviewPreparationProfile> converter =
                new BeanOutputConverter<>(InterviewPreparationProfile.class);

        InterviewPreparationProfile profile = converter.convert("""
                {
                  "candidateSummary":"具备 Java 后端经验。",
                  "resumeEvidence":["使用 Spring Boot 开发项目"],
                  "jobRequirements":[],
                  "focusAreas":[{
                    "topic":"Spring Boot",
                    "reason":"简历明确包含项目经验",
                    "sources":["RESUME"]
                  }]
                }
                """);

        assertEquals("具备 Java 后端经验。", profile.candidateSummary());
        assertEquals(List.of(InterviewFocusSource.RESUME), profile.focusAreas().get(0).sources());
    }

    @Test
    void createsAnEvidenceBasedProfileFromResumeAndJobDescription() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        InterviewPreparationProfile generatedProfile = new InterviewPreparationProfile(
                "具备 Java 后端和 RAG 项目经验，目标是 AI 应用后端岗位。",
                List.of("使用 Spring Boot 开发内容平台", "使用 Qdrant 实现向量检索"),
                List.of("需要 MySQL 调优经验", "需要大模型应用经验"),
                List.of(
                        new InterviewFocusArea("RAG 向量生命周期", "简历明确包含 Qdrant 项目经验",
                                List.of(InterviewFocusSource.RESUME)),
                        new InterviewFocusArea("MySQL 性能优化", "岗位要求与后端经历需要共同验证",
                                List.of(InterviewFocusSource.RESUME, InterviewFocusSource.JOB_DESCRIPTION))
                ));
        when(chatClient.prompt(anyString()).call().entity(InterviewPreparationProfile.class))
                .thenReturn(generatedProfile);
        clearInvocations(chatClient);
        InterviewPreparationService service = new InterviewPreparationService(chatClient);

        InterviewPreparationResult result = service.prepare(
                "Java AI 应用开发工程师",
                "项目：使用 Spring Boot、Qdrant 构建 RAG 内容平台。",
                "要求熟悉 MySQL 调优和大模型应用。"
        );

        assertEquals("Java AI 应用开发工程师", result.targetRole());
        assertEquals("interview-preparation-v1", result.promptVersion());
        assertEquals(generatedProfile, result.profile());
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatClient).prompt(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertEquals(true, prompt.contains("目标岗位：Java AI 应用开发工程师"));
        assertEquals(true, prompt.contains("使用 Spring Boot、Qdrant 构建 RAG 内容平台"));
        assertEquals(true, prompt.contains("要求熟悉 MySQL 调优和大模型应用"));
        assertEquals(true, prompt.contains("不得推断输入中不存在的经历"));
        assertEquals(true, prompt.contains("不得生成面试题"));
    }

    @Test
    void rejectsMissingRequiredInputBeforeCallingTheModel() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        InterviewPreparationService service = new InterviewPreparationService(chatClient);

        assertEquals("目标岗位不能为空",
                assertThrows(IllegalArgumentException.class,
                        () -> service.prepare(" ", "Java 简历", null)).getMessage());
        assertEquals("简历内容不能为空",
                assertThrows(IllegalArgumentException.class,
                        () -> service.prepare("Java 后端", " ", null)).getMessage());

        verifyNoInteractions(chatClient);
    }
}
