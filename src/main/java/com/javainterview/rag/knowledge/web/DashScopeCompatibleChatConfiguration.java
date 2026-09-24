package com.javainterview.rag.knowledge.web;

import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

public final class DashScopeCompatibleChatConfiguration {

    private static final int CHAT_MAX_ATTEMPTS = 2;
    private static final Duration CHAT_RETRY_BACKOFF = Duration.ofMillis(500);

    private DashScopeCompatibleChatConfiguration() {
    }

    public static RetryTemplate limitedChatRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(CHAT_MAX_ATTEMPTS)
                .retryOn(DashScopeCompatibleChatConfiguration::isRetryableChatFailure)
                .fixedBackoff(CHAT_RETRY_BACKOFF)
                .build();
    }

    public static boolean isRetryableChatFailure(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof TransientAiException
                    || current instanceof ConnectException
                    || current instanceof SocketTimeoutException
                    || current instanceof HttpTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    public static ResponseErrorHandler limitedChatResponseErrorHandler() {
        return new ResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) throws IOException {
                return response.getStatusCode().isError();
            }

            @Override
            public void handleError(URI url,
                                    HttpMethod method,
                                    ClientHttpResponse response) throws IOException {
                if (!response.getStatusCode().isError()) {
                    return;
                }
                String message = "模型服务返回 HTTP " + response.getStatusCode().value();
                if (response.getStatusCode().value() == 429
                        || response.getStatusCode().is5xxServerError()) {
                    throw new TransientAiException(message);
                }
                throw new NonTransientAiException(message);
            }
        };
    }
}
