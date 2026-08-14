package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.persistence.MySqlResumeEvidenceAuditRequestLogStore;
import com.javainterview.rag.interview.persistence.ResumeEvidenceAuditRequestLogMapper;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditRequestLogQueryService;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditRequestLogStore;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class ResumeEvidenceAuditConfiguration {

    @Bean
    ResumeEvidenceAuditRequestLogStore resumeEvidenceAuditRequestLogStore(
            ResumeEvidenceAuditRequestLogMapper mapper) {
        return new MySqlResumeEvidenceAuditRequestLogStore(mapper);
    }

    @Bean
    ResumeEvidenceAuditRequestLogQueryService resumeEvidenceAuditRequestLogQueryService(
            ResumeEvidenceAuditRequestLogStore logStore) {
        return new ResumeEvidenceAuditRequestLogQueryService(logStore);
    }

    @Bean
    ResumeEvidenceAuditService resumeEvidenceAuditService(
            ChatClient ragAnswerChatClient,
            ResumeEvidenceAuditRequestLogStore logStore,
            @Value("${spring.ai.openai.chat.options.model:unknown}") String modelName) {
        return new ResumeEvidenceAuditService(ragAnswerChatClient, logStore, modelName);
    }
}
