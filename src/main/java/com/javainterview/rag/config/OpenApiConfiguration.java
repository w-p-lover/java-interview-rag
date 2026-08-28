package com.javainterview.rag.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class OpenApiConfiguration {

    @Bean
    public OpenAPI javaInterviewRagOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Java 面试知识库与智能训练平台 API")
                .description("知识资料、RAG 检索、模拟面试与复习训练接口")
                .version("v1.0")
                .contact(new Contact().name("Java Interview RAG")));
    }
}
