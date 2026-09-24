package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.matching.ResumeJobMatchService;
import com.javainterview.rag.interview.matching.ResumeJobMatchRequestLogQueryService;
import com.javainterview.rag.interview.matching.ResumeJobMatchRequestLogStore;
import com.javainterview.rag.interview.persistence.MySqlResumeJobMatchRequestLogStore;
import com.javainterview.rag.interview.persistence.ResumeJobMatchRequestLogMapper;
import com.javainterview.rag.modelprovider.ModelProviderRegistry;
import org.springframework.ai.chat.client.ChatClient;
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
            ModelProviderRegistry modelProviderRegistry) {
        return new ResumeJobMatchService(
                ragAnswerChatClient, logStore, modelProviderRegistry::currentChatContract);
    }
}
