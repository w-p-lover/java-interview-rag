package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.resume.ResumeExpressionCoachService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class ResumeExpressionCoachConfiguration {

    @Bean
    ResumeExpressionCoachService resumeExpressionCoachService(
            ChatClient ragAnswerChatClient,
            @Value("${spring.ai.openai.chat.options.model:unknown}") String modelName) {
        return new ResumeExpressionCoachService(ragAnswerChatClient, modelName);
    }
}
