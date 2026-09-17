package com.javainterview.rag.interview.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.interview.persistence.MySqlResumeEvidenceAuditEvaluationRunStore;
import com.javainterview.rag.interview.persistence.MySqlResumeEvidenceAuditRequestLogStore;
import com.javainterview.rag.interview.persistence.ResumeEvidenceAuditEvaluationRunMapper;
import com.javainterview.rag.interview.persistence.ResumeEvidenceAuditRequestLogMapper;
import com.javainterview.rag.interview.resume.FixedResumeEvidenceAuditDataset;
import com.javainterview.rag.interview.resume.FixedResumeEvidenceAuditEvaluationService;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditEvaluationRunService;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditEvaluationRunStore;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditRequestLogQueryService;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditRequestLogStore;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.concurrent.Executor;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class ResumeEvidenceAuditConfiguration {

    @Bean(name = "resumeEvidenceAuditExecutor")
    Executor resumeEvidenceAuditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(8);
        executor.setThreadNamePrefix("resume-audit-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }

    @Bean(name = "resumeEvidenceAuditEvaluationRunExecutor")
    Executor resumeEvidenceAuditEvaluationRunExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(8);
        executor.setThreadNamePrefix("resume-audit-evaluation-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }

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
            @Value("${spring.ai.openai.chat.options.model:unknown}") String modelName,
            @Value("${java-interview-rag.resume-audit.timeout:420s}") Duration timeout,
            @org.springframework.beans.factory.annotation.Qualifier("resumeEvidenceAuditExecutor") Executor executor) {
        return new ResumeEvidenceAuditService(ragAnswerChatClient, logStore, modelName, timeout, executor);
    }

    @Bean
    FixedResumeEvidenceAuditEvaluationService fixedResumeEvidenceAuditEvaluationService(
            FixedResumeEvidenceAuditDataset dataset,
            ResumeEvidenceAuditService auditService) {
        return new FixedResumeEvidenceAuditEvaluationService(dataset, auditService);
    }

    @Bean
    ResumeEvidenceAuditEvaluationRunStore resumeEvidenceAuditEvaluationRunStore(
            ResumeEvidenceAuditEvaluationRunMapper mapper,
            ObjectMapper objectMapper) {
        return new MySqlResumeEvidenceAuditEvaluationRunStore(mapper, objectMapper);
    }

    @Bean
    ResumeEvidenceAuditEvaluationRunService resumeEvidenceAuditEvaluationRunService(
            FixedResumeEvidenceAuditEvaluationService evaluationService,
            ResumeEvidenceAuditEvaluationRunStore runStore,
            @org.springframework.beans.factory.annotation.Qualifier(
                    "resumeEvidenceAuditEvaluationRunExecutor") Executor runExecutor) {
        return new ResumeEvidenceAuditEvaluationRunService(
                evaluationService, runStore, runExecutor);
    }
}
