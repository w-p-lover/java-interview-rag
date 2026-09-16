package com.javainterview.rag.knowledge.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class DashScopeEmbeddingHttpConfiguration {

    @Bean
    @Primary
    RestClient.Builder dashScopeEmbeddingRestClientBuilder(
            @Value("${java-interview-rag.embedding.connect-timeout:5s}") Duration connectTimeout,
            @Value("${java-interview-rag.embedding.response-timeout:30s}") Duration responseTimeout) {
        requirePositive(connectTimeout, "Embedding 连接超时");
        requirePositive(responseTimeout, "Embedding 响应超时");
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(responseTimeout);
        return RestClient.builder().requestFactory(requestFactory);
    }

    @Bean
    @Primary
    RetryTemplate dashScopeEmbeddingRetryTemplate() {
        // 导入任务可以由用户安全重试；单次请求失败后立即补偿，避免默认重试放大截止时间。
        return RetryTemplate.builder().maxAttempts(1).build();
    }

    private void requirePositive(Duration timeout, String name) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException(name + "必须大于 0");
        }
    }
}
