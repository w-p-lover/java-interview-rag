package com.javainterview.rag.interview.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.interview.persistence.InterviewSessionMapper;
import com.javainterview.rag.interview.persistence.MySqlInterviewSessionStore;
import com.javainterview.rag.interview.session.InterviewSessionService;
import com.javainterview.rag.interview.session.InterviewSessionStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class InterviewSessionConfiguration {

    @Bean
    InterviewSessionStore interviewSessionStore(InterviewSessionMapper mapper, ObjectMapper objectMapper) {
        return new MySqlInterviewSessionStore(mapper, objectMapper);
    }

    @Bean
    InterviewSessionService interviewSessionService(InterviewSessionStore store) {
        return new InterviewSessionService(store);
    }
}
