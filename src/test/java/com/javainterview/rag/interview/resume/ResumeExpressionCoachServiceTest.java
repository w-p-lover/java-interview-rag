package com.javainterview.rag.interview.resume;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResumeExpressionCoachServiceTest {

    @Test
    void returnsCandidatesBoundToConfirmedFacts() {
        BeanOutputConverter<ResumeExpressionCoachDraft> converter =
                new BeanOutputConverter<>(ResumeExpressionCoachDraft.class);
        ResumeExpressionCoachDraft draft = converter.convert("""
                {
                  "candidates":[
                    {"text":"负责 Spring Boot 订单服务开发，支撑日常业务交付。",
                     "rationale":"保留已确认的职责和技术，压缩为动作与场景。",
                     "supportingFactNumbers":[1],
                     "reviewPoints":["请确认“日常业务交付”是否符合实际场景。"]}
                  ]
                }
                """);
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClient.prompt().system(anyString()).user(anyString()).call()
                .responseEntity(ResumeExpressionCoachDraft.class))
                .thenReturn(new org.springframework.ai.chat.client.ResponseEntity<>(null, draft));
        ResumeExpressionCoachService service = new ResumeExpressionCoachService(chatClient);

        ResumeExpressionCoachResult result = service.coach(new ResumeExpressionCoachRequest(
                "Java 后端工程师",
                List.of(new ResumeEvidenceAuditResult.ConfirmedFact(
                        ResumeEvidenceAuditResult.FactType.RESPONSIBILITY,
                        "负责 Spring Boot 订单服务开发",
                        "负责 Spring Boot 订单服务开发"))));

        assertEquals("RESUME_EXPRESSION_COACH", result.role());
        assertEquals("resume-expression-coach-v1", result.promptVersion());
        assertEquals(1, result.candidates().size());
        assertEquals(List.of(1), result.candidates().get(0).supportingFactNumbers());
        assertEquals("负责 Spring Boot 订单服务开发", result.candidates().get(0)
                .supportingFacts().get(0).evidence());
    }

    @Test
    void rejectsCandidateWithUnsupportedNumber() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(chatClient.prompt().system(anyString()).user(anyString()).call()
                .responseEntity(ResumeExpressionCoachDraft.class))
                .thenReturn(new org.springframework.ai.chat.client.ResponseEntity<>(null,
                        new ResumeExpressionCoachDraft(List.of(new ResumeExpressionCoachDraft.CandidateDraft(
                                "负责订单服务开发，性能提升 50%",
                                "候选表达",
                                List.of(1),
                                List.of())))));
        ResumeExpressionCoachService service = new ResumeExpressionCoachService(chatClient);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.coach(new ResumeExpressionCoachRequest(
                        null,
                        List.of(new ResumeEvidenceAuditResult.ConfirmedFact(
                                ResumeEvidenceAuditResult.FactType.RESPONSIBILITY,
                                "负责订单服务开发",
                                "负责订单服务开发")))));

        assertTrue(exception.getMessage().contains("数字"));
    }
}
