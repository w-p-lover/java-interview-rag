package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.deepdive.ProjectDeepDiveService;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class ProjectDeepDiveConfiguration {

    @Bean
    ProjectDeepDiveService projectDeepDiveService(MarkdownSimilaritySearchService searchService) {
        return new ProjectDeepDiveService(searchService);
    }
}
