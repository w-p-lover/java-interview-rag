package com.javainterview.rag.interview.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.interview.persistence.InterviewSessionMapper;
import com.javainterview.rag.interview.persistence.InterviewReviewTaskMapper;
import com.javainterview.rag.interview.persistence.InterviewReviewAttemptMapper;
import com.javainterview.rag.interview.persistence.MySqlInterviewSessionStore;
import com.javainterview.rag.interview.persistence.MySqlInterviewReviewTaskStore;
import com.javainterview.rag.interview.persistence.MySqlInterviewReviewAttemptStore;
import com.javainterview.rag.interview.review.InterviewReviewTaskService;
import com.javainterview.rag.interview.review.InterviewReviewTaskStore;
import com.javainterview.rag.interview.review.InterviewReviewAttemptStore;
import com.javainterview.rag.interview.session.InterviewSessionService;
import com.javainterview.rag.interview.session.InterviewSessionStartService;
import com.javainterview.rag.interview.session.InterviewSessionStore;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluationService;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluator;
import com.javainterview.rag.interview.preparation.InterviewPreparationService;
import com.javainterview.rag.interview.question.InterviewQuestionGenerationService;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import org.springframework.ai.chat.client.ChatClient;
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
    InterviewReviewTaskService interviewReviewTaskService(
            InterviewSessionStore sessionStore,
            InterviewReviewTaskStore taskStore,
            InterviewReviewAttemptStore attemptStore,
            InterviewAnswerEvaluator answerEvaluator) {
        return new InterviewReviewTaskService(sessionStore, taskStore, attemptStore, answerEvaluator);
    }

    @Bean
    InterviewAnswerEvaluator interviewAnswerEvaluator(
            ChatClient ragAnswerChatClient,
            MarkdownSimilaritySearchService searchService,
            @Value("${spring.ai.openai.chat.options.model:unknown}") String modelName) {
        return new InterviewAnswerEvaluationService(ragAnswerChatClient, searchService, modelName);
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
