package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.matching.ResumeJobMatchService;
import com.javainterview.rag.interview.matching.ResumeJobMatchRequestLogQueryService;
import com.javainterview.rag.interview.matching.ResumeJobMatchRequestLogStore;
import com.javainterview.rag.interview.persistence.MySqlResumeJobMatchRequestLogStore;
import com.javainterview.rag.interview.persistence.ResumeJobMatchRequestLogMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class ResumeJobMatchConfiguration {

    @Bean
    ResumeJobMatchRequestLogStore resumeJobMatchRequestLogStore(ResumeJobMatchRequestLogMapper mapper) {
        return new MySqlResumeJobMatchRequestLogStore(mapper);
    }

    @Bean
    ResumeJobMatchRequestLogQueryService resumeJobMatchRequestLogQueryService(
            ResumeJobMatchRequestLogStore logStore) {
        return new ResumeJobMatchRequestLogQueryService(logStore);
    }

    @Bean
    ResumeJobMatchService resumeJobMatchService(
            ChatClient ragAnswerChatClient,
            ResumeJobMatchRequestLogStore logStore,
            @Value("${spring.ai.openai.chat.options.model:unknown}") String modelName) {
        return new ResumeJobMatchService(ragAnswerChatClient, logStore, modelName);
    }
}
