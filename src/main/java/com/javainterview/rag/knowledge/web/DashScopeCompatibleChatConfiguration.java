package com.javainterview.rag.knowledge.web;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
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

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class DashScopeCompatibleChatConfiguration {

    private static final int CHAT_MAX_ATTEMPTS = 2;
    private static final Duration CHAT_RETRY_BACKOFF = Duration.ofMillis(500);

    @Bean
    ChatModel dashScopeCompatibleChatModel(
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model}") String model,
            @Value("${spring.ai.openai.chat.options.temperature}") Double temperature) {
        Assert.hasText(apiKey, "DashScope API Key 不能为空");
        return OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl(baseUrl)
                        .apiKey(new SimpleApiKey(apiKey))
                        .responseErrorHandler(limitedChatResponseErrorHandler())
                        .build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .build())
                // 重试只包裹模型 HTTP 请求，不能在业务服务外层重放完整工具调用。
                .retryTemplate(limitedChatRetryTemplate())
                .build();
    }

    static RetryTemplate limitedChatRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(CHAT_MAX_ATTEMPTS)
                .retryOn(DashScopeCompatibleChatConfiguration::isRetryableChatFailure)
                .fixedBackoff(CHAT_RETRY_BACKOFF)
                .build();
    }

    static boolean isRetryableChatFailure(Throwable throwable) {
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

    static ResponseErrorHandler limitedChatResponseErrorHandler() {
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
