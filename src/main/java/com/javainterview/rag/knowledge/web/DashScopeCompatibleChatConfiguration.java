package com.javainterview.rag.knowledge.web;

import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.net.URI;

public final class DashScopeCompatibleChatConfiguration {

    private DashScopeCompatibleChatConfiguration() {
    }

    public static RetryTemplate limitedChatRetryTemplate() {
        // Chat 超时可能发生在 Provider 已开始计费后，由业务恢复入口决定是否再次调用。
        return RetryTemplate.builder().maxAttempts(1).build();
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
