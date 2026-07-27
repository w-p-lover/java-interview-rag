package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.preparation.InterviewPreparationService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class InterviewPreparationConfiguration {

    @Bean
    InterviewPreparationService interviewPreparationService(ChatClient ragAnswerChatClient) {
        return new InterviewPreparationService(ragAnswerChatClient);
    }
}
