package com.javainterview.rag.interview.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.interview.persistence.InterviewSessionMapper;
import com.javainterview.rag.interview.persistence.InterviewReviewTaskMapper;
import com.javainterview.rag.interview.persistence.InterviewReviewAttemptMapper;
import com.javainterview.rag.interview.persistence.InterviewAnswerEvaluationLogMapper;
import com.javainterview.rag.interview.persistence.AnswerExperimentRunMapper;
import com.javainterview.rag.interview.persistence.MySqlAnswerExperimentStore;
import com.javainterview.rag.interview.evaluation.AnswerExperimentStore;
import com.javainterview.rag.interview.evaluation.AnswerExperimentService;
import com.javainterview.rag.interview.persistence.InterviewToolExecutionLogMapper;
import com.javainterview.rag.interview.persistence.InterviewAssistantRequestLogMapper;
import com.javainterview.rag.interview.persistence.MySqlInterviewSessionStore;
import com.javainterview.rag.interview.persistence.MySqlInterviewReviewTaskStore;
import com.javainterview.rag.interview.persistence.MySqlInterviewReviewAttemptStore;
import com.javainterview.rag.interview.persistence.MySqlInterviewAnswerEvaluationLogStore;
import com.javainterview.rag.interview.persistence.MySqlInterviewToolExecutionLogStore;
import com.javainterview.rag.interview.persistence.MySqlInterviewAssistantRequestLogStore;
import com.javainterview.rag.interview.review.InterviewReviewTaskService;
import com.javainterview.rag.interview.review.InterviewReviewPathService;
import com.javainterview.rag.interview.review.InterviewReviewTaskStore;
import com.javainterview.rag.interview.review.InterviewReviewAttemptStore;
import com.javainterview.rag.interview.deepdive.ProjectDeepDiveService;
import com.javainterview.rag.interview.deepdive.ProjectDeepDiveSessionStartService;
import com.javainterview.rag.interview.session.InterviewSessionService;
import com.javainterview.rag.interview.session.InterviewSessionStartService;
import com.javainterview.rag.interview.session.InterviewSessionStore;
import com.javainterview.rag.interview.tool.InterviewReviewTools;
import com.javainterview.rag.interview.tool.InterviewReviewAssistantService;
import com.javainterview.rag.interview.tool.InterviewAssistantRequestLogStore;
import com.javainterview.rag.interview.tool.InterviewAssistantRequestLogQueryService;
import com.javainterview.rag.interview.tool.InterviewToolExecutionLogQueryService;
import com.javainterview.rag.interview.tool.InterviewToolExecutionLogStore;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluationService;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluationLogStore;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluationLogQueryService;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluator;
import com.javainterview.rag.interview.evaluation.FixedInterviewAnswerEvaluationDataset;
import com.javainterview.rag.interview.evaluation.FixedInterviewAnswerEvaluationService;
import com.javainterview.rag.interview.evaluation.ObservableInterviewAnswerEvaluator;
import com.javainterview.rag.interview.preparation.InterviewPreparationService;
import com.javainterview.rag.interview.question.InterviewQuestionGenerationService;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import com.javainterview.rag.modelprovider.ModelProviderRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class InterviewSessionConfiguration {

    @Bean(name = "answerExperimentExecutor", destroyMethod = "shutdown")
    ThreadPoolTaskExecutor answerExperimentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(2);
        executor.setThreadNamePrefix("answer-experiment-");
        executor.initialize();
        return executor;
    }

    @Bean
    InterviewSessionStore interviewSessionStore(InterviewSessionMapper mapper, ObjectMapper objectMapper) {
        return new MySqlInterviewSessionStore(mapper, objectMapper);
    }

    @Bean
    InterviewReviewTaskStore interviewReviewTaskStore(
            InterviewReviewTaskMapper mapper, ObjectMapper objectMapper) {
        return new MySqlInterviewReviewTaskStore(mapper, objectMapper);
    }

    @Bean
    InterviewReviewAttemptStore interviewReviewAttemptStore(
            InterviewReviewAttemptMapper mapper, ObjectMapper objectMapper) {
        return new MySqlInterviewReviewAttemptStore(mapper, objectMapper);
    }

    @Bean
    InterviewAnswerEvaluationLogStore interviewAnswerEvaluationLogStore(
            InterviewAnswerEvaluationLogMapper mapper) {
        return new MySqlInterviewAnswerEvaluationLogStore(mapper);
    }

    @Bean
    InterviewAnswerEvaluationLogQueryService interviewAnswerEvaluationLogQueryService(
            InterviewAnswerEvaluationLogStore logStore) {
        return new InterviewAnswerEvaluationLogQueryService(logStore);
    }

    @Bean
    InterviewToolExecutionLogStore interviewToolExecutionLogStore(
            InterviewToolExecutionLogMapper mapper) {
        return new MySqlInterviewToolExecutionLogStore(mapper);
    }

    @Bean
    InterviewAssistantRequestLogStore interviewAssistantRequestLogStore(
            InterviewAssistantRequestLogMapper mapper) {
        return new MySqlInterviewAssistantRequestLogStore(mapper);
    }

    @Bean
    InterviewAssistantRequestLogQueryService interviewAssistantRequestLogQueryService(
            InterviewAssistantRequestLogStore logStore) {
        return new InterviewAssistantRequestLogQueryService(logStore);
    }

    @Bean
    InterviewToolExecutionLogQueryService interviewToolExecutionLogQueryService(
            InterviewToolExecutionLogStore logStore) {
        return new InterviewToolExecutionLogQueryService(logStore);
    }

    @Bean
    InterviewReviewTaskService interviewReviewTaskService(
            InterviewSessionStore sessionStore,
            InterviewReviewTaskStore taskStore,
            InterviewReviewAttemptStore attemptStore,
            InterviewAnswerEvaluator answerEvaluator) {
        return new InterviewReviewTaskService(sessionStore, taskStore, attemptStore, answerEvaluator);
    }

    @Bean
    InterviewReviewPathService interviewReviewPathService(
            InterviewSessionStore sessionStore,
            InterviewReviewTaskStore taskStore,
            InterviewReviewAttemptStore attemptStore) {
        return new InterviewReviewPathService(sessionStore, taskStore, attemptStore);
    }

    @Bean
    InterviewReviewTools interviewReviewTools(
            InterviewReviewTaskService reviewTaskService,
            InterviewToolExecutionLogStore executionLogStore) {
        return new InterviewReviewTools(reviewTaskService, executionLogStore);
    }

    @Bean
    ToolCallbackProvider interviewReviewToolCallbackProvider(InterviewReviewTools reviewTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(reviewTools)
                .build();
    }

    @Bean
    InterviewReviewAssistantService interviewReviewAssistantService(
            ChatClient ragAnswerChatClient,
            ToolCallbackProvider interviewReviewToolCallbackProvider,
            InterviewAssistantRequestLogStore requestLogStore,
            ModelProviderRegistry modelProviderRegistry) {
        return new InterviewReviewAssistantService(
                ragAnswerChatClient,
                interviewReviewToolCallbackProvider,
                requestLogStore,
                modelProviderRegistry::currentChatContract);
    }

    @Bean
    InterviewAnswerEvaluator interviewAnswerEvaluator(
            ChatClient ragAnswerChatClient,
            MarkdownSimilaritySearchService searchService,
            InterviewAnswerEvaluationLogStore logStore,
            ModelProviderRegistry modelProviderRegistry) {
        InterviewAnswerEvaluator evaluator =
                new InterviewAnswerEvaluationService(
                        ragAnswerChatClient, searchService, modelProviderRegistry::currentChatContract);
        return new ObservableInterviewAnswerEvaluator(evaluator, logStore);
    }

    @Bean
    FixedInterviewAnswerEvaluationService fixedInterviewAnswerEvaluationService(
            FixedInterviewAnswerEvaluationDataset dataset,
            InterviewAnswerEvaluator answerEvaluator) {
        return new FixedInterviewAnswerEvaluationService(dataset, answerEvaluator);
    }

    @Bean
    AnswerExperimentStore answerExperimentStore(AnswerExperimentRunMapper mapper, ObjectMapper objectMapper) {
        return new MySqlAnswerExperimentStore(mapper, objectMapper);
    }

    @Bean
    AnswerExperimentService answerExperimentService(FixedInterviewAnswerEvaluationDataset dataset,
                                                    InterviewAnswerEvaluator evaluator,
                                                    AnswerExperimentStore store,
                                                    @Qualifier("answerExperimentExecutor") java.util.concurrent.Executor taskExecutor) {
        return new AnswerExperimentService(dataset, evaluator, store, taskExecutor);
    }

    @Bean
    InterviewSessionService interviewSessionService(InterviewSessionStore store,
                                                     InterviewAnswerEvaluator answerEvaluator) {
        return new InterviewSessionService(store, answerEvaluator);
    }

    @Bean
    InterviewSessionStartService interviewSessionStartService(
            InterviewPreparationService preparationService,
            InterviewQuestionGenerationService questionGenerationService,
            InterviewSessionService sessionService) {
        return new InterviewSessionStartService(
                preparationService, questionGenerationService, sessionService);
    }

    @Bean
    ProjectDeepDiveSessionStartService projectDeepDiveSessionStartService(
            ProjectDeepDiveService projectDeepDiveService,
            InterviewSessionService sessionService) {
        return new ProjectDeepDiveSessionStartService(projectDeepDiveService, sessionService);
    }
}
