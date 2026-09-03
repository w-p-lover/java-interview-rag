package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.evaluation.FixedRagEvaluationDataset;
import com.javainterview.rag.knowledge.evaluation.FixedRagStructureEvaluationDataset;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationService;
import com.javainterview.rag.knowledge.evaluation.HybridRetrievalEvaluationService;
import com.javainterview.rag.knowledge.document.MarkdownImportRegistry;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import com.javainterview.rag.knowledge.vector.MarkdownSparseSearchService;
import com.javainterview.rag.knowledge.vector.MarkdownHybridSearchService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class RagRetrievalEvaluationConfiguration {

    @Bean
    RagRetrievalEvaluationService ragRetrievalEvaluationService(FixedRagEvaluationDataset dataset,
                                                                 FixedRagStructureEvaluationDataset structureDataset,
                                                                 MarkdownSimilaritySearchService searchService,
                                                                 MarkdownImportRegistry importRegistry) {
        return new RagRetrievalEvaluationService(dataset, structureDataset, searchService, importRegistry);
    }

    @Bean
    HybridRetrievalEvaluationService hybridRetrievalEvaluationService(
            FixedRagEvaluationDataset dataset,
            MarkdownSimilaritySearchService denseSearchService,
            MarkdownSparseSearchService sparseSearchService,
            MarkdownHybridSearchService hybridSearchService,
            MarkdownImportRegistry importRegistry) {
        return new HybridRetrievalEvaluationService(dataset, denseSearchService,
                sparseSearchService, hybridSearchService, importRegistry);
    }
}
