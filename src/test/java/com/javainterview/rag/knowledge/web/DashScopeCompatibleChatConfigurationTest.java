package com.javainterview.rag.knowledge.web;

import org.junit.jupiter.api.Test;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResponseErrorHandler;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DashScopeCompatibleChatConfigurationTest {

    @Test
    void doesNotAutomaticallyRetryPaidChatFailure() {
        RetryTemplate retryTemplate =
                DashScopeCompatibleChatConfiguration.limitedChatRetryTemplate();
        AtomicInteger attempts = new AtomicInteger();

        TransientAiException exception = assertThrows(
                TransientAiException.class,
                () -> retryTemplate.execute(context -> {
                    attempts.incrementAndGet();
                    throw new TransientAiException("上游暂时不可用");
                }));

        assertEquals("上游暂时不可用", exception.getMessage());
        assertEquals(1, attempts.get());
    }

    @Test
    void classifiesRateLimitAndServerErrorsAsTransient() throws Exception {
        ResponseErrorHandler errorHandler =
                DashScopeCompatibleChatConfiguration.limitedChatResponseErrorHandler();
        URI endpoint = URI.create("https://example.test/chat/completions");

        TransientAiException rateLimitException = assertThrows(
                TransientAiException.class,
                () -> errorHandler.handleError(
                        endpoint, HttpMethod.POST, response(HttpStatus.TOO_MANY_REQUESTS)));
        assertEquals("模型服务返回 HTTP 429", rateLimitException.getMessage());
        assertThrows(TransientAiException.class,
                () -> errorHandler.handleError(
                        endpoint, HttpMethod.POST, response(HttpStatus.BAD_GATEWAY)));
        assertThrows(NonTransientAiException.class,
                () -> errorHandler.handleError(
                        endpoint, HttpMethod.POST, response(HttpStatus.BAD_REQUEST)));
    }

    private MockClientHttpResponse response(HttpStatus status) {
        return new MockClientHttpResponse(
                "上游错误".getBytes(StandardCharsets.UTF_8), status);
    }
}
