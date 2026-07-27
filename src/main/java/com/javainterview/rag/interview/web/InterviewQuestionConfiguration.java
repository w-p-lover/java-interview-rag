package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.question.InterviewQuestionGenerationService;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class InterviewQuestionConfiguration {

    @Bean
    InterviewQuestionGenerationService interviewQuestionGenerationService(
            ChatClient ragAnswerChatClient,
            MarkdownSimilaritySearchService searchService) {
        return new InterviewQuestionGenerationService(ragAnswerChatClient, searchService);
    }
}
