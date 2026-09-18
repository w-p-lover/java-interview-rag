package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.draft.InterviewDraftService;
import com.javainterview.rag.interview.preparation.InterviewPreparationService;
import com.javainterview.rag.interview.question.InterviewQuestionPlanService;
import com.javainterview.rag.interview.session.InterviewSessionStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class InterviewDraftConfiguration {

    @Bean
    InterviewDraftService interviewDraftService(
            InterviewPreparationService preparationService,
            InterviewQuestionPlanService questionPlanService,
            InterviewSessionStore sessionStore) {
        return new InterviewDraftService(preparationService, questionPlanService, sessionStore);
    }
}
