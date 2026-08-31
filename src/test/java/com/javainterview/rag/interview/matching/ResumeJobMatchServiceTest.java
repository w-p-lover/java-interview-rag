package com.javainterview.rag.interview.matching;

import com.javainterview.rag.interview.resume.ResumeEvidenceAuditResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ResumeJobMatchServiceTest {

    @Test
    void supportsStructuredOutputAndVerifiesBothEvidenceSources() {
        BeanOutputConverter<ResumeJobMatchDraft> converter =
                new BeanOutputConverter<>(ResumeJobMatchDraft.class);
        ResumeJobMatchDraft draft = converter.convert("""
                {
                  "summary":"已有后端基础，RAG 经验需要继续核对。",
                  "assessments":[
                    {"requirement":"熟悉 Spring Boot","requirementEvidence":"熟悉 Spring Boot",
                     "status":"MATCHED","resumeEvidence":"使用 Spring Boot 开发订单服务","explanation":"已有直接项目证据。"},
                    {"requirement":"具备大模型应用经验","requirementEvidence":"具备大模型应用经验",
                     "status":"MISSING","resumeEvidence":null,"explanation":"已确认事实中没有对应证据。"}
                  ]
                }
                """);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClient.prompt(anyString()).call().responseEntity(ResumeJobMatchDraft.class))
                .thenReturn(new org.springframework.ai.chat.client.ResponseEntity<>(null, draft));
        clearInvocations(chatClient);
        ResumeJobMatchService service = new ResumeJobMatchService(
                chatClient, mock(ResumeJobMatchRequestLogStore.class), "qwen3.7-plus");

        ResumeJobMatchResult result = service.match(new ResumeJobMatchRequest(
                "Java 后端工程师",
                "任职要求：熟悉 Spring Boot；具备大模型应用经验。",
                List.of(new ResumeJobMatchRequest.ConfirmedFact(
                        ResumeEvidenceAuditResult.FactType.TECHNOLOGY,
                        "使用 Spring Boot 开发订单服务",
                        "使用 Spring Boot 开发订单服务"))));

        assertEquals("JOB_MATCH_ADVISOR", result.role());
        assertEquals("job-match-v1", result.promptVersion());
        assertEquals(2, result.assessments().size());
        assertEquals(ResumeJobMatchResult.MatchStatus.MATCHED, result.assessments().get(0).status());
        assertEquals(ResumeJobMatchResult.MatchStatus.MISSING, result.assessments().get(1).status());
        assertEquals(null, result.assessments().get(1).resumeEvidence());
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatClient).prompt(promptCaptor.capture());
        assertTrue(promptCaptor.getValue().contains("<confirmed_facts>"));
        assertTrue(promptCaptor.getValue().contains("使用 Spring Boot 开发订单服务"));
    }

    @Test
    void rejectsEvidenceOutsideJobDescriptionOrConfirmedFacts() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClient.prompt(anyString()).call().responseEntity(ResumeJobMatchDraft.class))
                .thenReturn(new org.springframework.ai.chat.client.ResponseEntity<>(null, new ResumeJobMatchDraft(
                        "需要人工核对。",
                        List.of(new ResumeJobMatchDraft.AssessmentDraft(
                                "熟悉 Redis", "熟悉 Redis", ResumeJobMatchDraft.MatchStatus.MATCHED,
                                "使用 Redis", "证据不匹配")))));
        ResumeJobMatchService service = new ResumeJobMatchService(
                chatClient, mock(ResumeJobMatchRequestLogStore.class), "qwen3.7-plus");

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.match(new ResumeJobMatchRequest(
                        "Java 后端工程师", "熟悉 Redis。",
                        List.of(new ResumeJobMatchRequest.ConfirmedFact(
                                ResumeEvidenceAuditResult.FactType.TECHNOLOGY,
                                "使用 Qdrant", "使用 Qdrant")))));

        assertEquals("模型返回的简历证据不在已确认事实中", exception.getMessage());
    }

    @Test
    void rejectsMissingInputBeforeCallingModel() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ResumeJobMatchService service = new ResumeJobMatchService(
                chatClient, mock(ResumeJobMatchRequestLogStore.class), "qwen3.7-plus");

        assertEquals("岗位描述不能为空", assertThrows(IllegalArgumentException.class,
                () -> service.match(new ResumeJobMatchRequest("Java 后端", " ", List.of()))).getMessage());
        verifyNoInteractions(chatClient);
    }
}
