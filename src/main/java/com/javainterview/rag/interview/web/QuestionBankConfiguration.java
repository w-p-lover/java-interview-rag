package com.javainterview.rag.interview.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.interview.bank.BankGenerationService;
import com.javainterview.rag.interview.bank.BankQuestionGenerator;
import com.javainterview.rag.interview.bank.BankSources;
import com.javainterview.rag.interview.bank.QuestionBankService;
import com.javainterview.rag.interview.bank.QuestionBankStore;
import com.javainterview.rag.interview.persistence.BankGenerationTaskMapper;
import com.javainterview.rag.interview.persistence.MySqlQuestionBankStore;
import com.javainterview.rag.interview.persistence.QuestionBankMapper;
import com.javainterview.rag.interview.session.InterviewSessionService;
import com.javainterview.rag.interview.session.InterviewSessionStore;
import com.javainterview.rag.knowledge.document.MarkdownImportRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class QuestionBankConfiguration {
    @Bean
    QuestionBankStore questionBankStore(QuestionBankMapper questions, BankGenerationTaskMapper tasks,
                                         ObjectMapper json, PlatformTransactionManager transactionManager) {
        return new MySqlQuestionBankStore(questions, tasks, json, new TransactionTemplate(transactionManager));
    }

    @Bean
    BankSources bankSources(MarkdownImportRegistry registry,
                             @Value("${java-interview-rag.knowledge.chunk.max-tokens:600}") int maxChunkTokens) {
        return new BankSources(registry, maxChunkTokens);
    }

    @Bean(destroyMethod = "shutdownNow")
    ExecutorService bankModelExecutor() {
        return Executors.newFixedThreadPool(1, runnable -> {
            Thread thread = new Thread(runnable, "question-bank-model-");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Bean
    BankQuestionGenerator bankQuestionGenerator(
            ChatClient ragAnswerChatClient,
            ExecutorService bankModelExecutor,
            @Value("${java-interview-rag.question-bank.model-timeout-ms:120000}") long timeoutMillis) {
        return new BankQuestionGenerator(ragAnswerChatClient, bankModelExecutor, timeoutMillis);
    }

    @Bean
    ThreadPoolTaskExecutor bankGenerationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(8);
        executor.setThreadNamePrefix("question-bank-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        return executor;
    }

    @Bean
    BankGenerationService bankGenerationService(QuestionBankStore store, BankSources sources,
                                                 BankQuestionGenerator generator,
                                                 @Qualifier("bankGenerationExecutor") Executor executor,
                                                 @Value("${spring.ai.openai.chat.options.model:unknown}") String modelName) {
        return new BankGenerationService(store, sources, generator, executor, modelName);
    }

    @Bean
    QuestionBankService questionBankService(QuestionBankStore store, BankSources sources,
                                             InterviewSessionService sessions, InterviewSessionStore sessionStore,
                                             @Value("${spring.ai.openai.chat.options.model:unknown}") String modelName) {
        return new QuestionBankService(store, sources, sessions, sessionStore, modelName);
    }
}
