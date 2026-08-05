package com.javainterview.rag.interview.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.interview.persistence.InterviewSessionMapper;
import com.javainterview.rag.interview.persistence.InterviewReviewTaskMapper;
import com.javainterview.rag.interview.persistence.InterviewReviewAttemptMapper;
import com.javainterview.rag.interview.persistence.InterviewAnswerEvaluationLogMapper;
import com.javainterview.rag.interview.persistence.InterviewToolExecutionLogMapper;
import com.javainterview.rag.interview.persistence.MySqlInterviewSessionStore;
import com.javainterview.rag.interview.persistence.MySqlInterviewReviewTaskStore;
import com.javainterview.rag.interview.persistence.MySqlInterviewReviewAttemptStore;
import com.javainterview.rag.interview.persistence.MySqlInterviewAnswerEvaluationLogStore;
import com.javainterview.rag.interview.persistence.MySqlInterviewToolExecutionLogStore;
import com.javainterview.rag.interview.review.InterviewReviewTaskService;
import com.javainterview.rag.interview.review.InterviewReviewTaskStore;
import com.javainterview.rag.interview.review.InterviewReviewAttemptStore;
import com.javainterview.rag.interview.session.InterviewSessionService;
import com.javainterview.rag.interview.session.InterviewSessionStartService;
import com.javainterview.rag.interview.session.InterviewSessionStore;
import com.javainterview.rag.interview.tool.InterviewReviewTools;
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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class InterviewSessionConfiguration {

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
    InterviewAnswerEvaluator interviewAnswerEvaluator(
            ChatClient ragAnswerChatClient,
            MarkdownSimilaritySearchService searchService,
            InterviewAnswerEvaluationLogStore logStore,
            @Value("${spring.ai.openai.chat.options.model:unknown}") String modelName) {
        InterviewAnswerEvaluator evaluator =
                new InterviewAnswerEvaluationService(ragAnswerChatClient, searchService, modelName);
        return new ObservableInterviewAnswerEvaluator(evaluator, logStore);
    }

    @Bean
    FixedInterviewAnswerEvaluationService fixedInterviewAnswerEvaluationService(
            FixedInterviewAnswerEvaluationDataset dataset,
            InterviewAnswerEvaluator answerEvaluator) {
        return new FixedInterviewAnswerEvaluationService(dataset, answerEvaluator);
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
}
