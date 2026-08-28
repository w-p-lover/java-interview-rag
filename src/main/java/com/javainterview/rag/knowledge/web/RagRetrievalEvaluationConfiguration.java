package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.evaluation.FixedRagEvaluationDataset;
import com.javainterview.rag.knowledge.evaluation.FixedRagStructureEvaluationDataset;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationService;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class RagRetrievalEvaluationConfiguration {

    @Bean
    RagRetrievalEvaluationService ragRetrievalEvaluationService(FixedRagEvaluationDataset dataset,
                                                                 FixedRagStructureEvaluationDataset structureDataset,
                                                                 MarkdownSimilaritySearchService searchService) {
        return new RagRetrievalEvaluationService(dataset, structureDataset, searchService);
    }
}
