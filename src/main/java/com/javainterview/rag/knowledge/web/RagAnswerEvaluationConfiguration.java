package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.evaluation.FixedRagEvaluationDataset;
import com.javainterview.rag.knowledge.evaluation.RagAnswerEvaluationService;
import com.javainterview.rag.knowledge.rag.RagAnswerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class RagAnswerEvaluationConfiguration {

    @Bean
    RagAnswerEvaluationService ragAnswerEvaluationService(FixedRagEvaluationDataset dataset,
                                                           RagAnswerService answerService) {
        return new RagAnswerEvaluationService(dataset, answerService);
    }
}
