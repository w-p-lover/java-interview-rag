package com.javainterview.rag.knowledge.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.knowledge.document.KnowledgeIndexVersionMetadata;
import com.javainterview.rag.knowledge.document.MarkdownImportRegistry;
import com.javainterview.rag.knowledge.evaluation.RagRetrievalEvaluationService;
import com.javainterview.rag.knowledge.experiment.RagRetrievalExperimentContractProvider;
import com.javainterview.rag.knowledge.experiment.RagRetrievalExperimentEvaluator;
import com.javainterview.rag.knowledge.experiment.RagRetrievalExperimentService;
import com.javainterview.rag.knowledge.experiment.RagRetrievalExperimentStore;
import com.javainterview.rag.knowledge.experiment.GitRevisionResolver;
import com.javainterview.rag.knowledge.persistence.MySqlRagRetrievalExperimentStore;
import com.javainterview.rag.knowledge.persistence.RagRetrievalExperimentRunMapper;
import com.javainterview.rag.knowledge.vector.MarkdownHybridSearchService;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import com.javainterview.rag.knowledge.vector.MarkdownSparseSearchService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class RagRetrievalExperimentConfiguration {

    @Bean
    RagRetrievalExperimentStore ragRetrievalExperimentStore(
            RagRetrievalExperimentRunMapper mapper,
            ObjectMapper objectMapper) {
        return new MySqlRagRetrievalExperimentStore(mapper, objectMapper);
    }

    @Bean
    RagRetrievalExperimentEvaluator ragRetrievalExperimentEvaluator(
            RagRetrievalEvaluationService evaluationService,
            MarkdownSimilaritySearchService denseSearchService,
            MarkdownSparseSearchService sparseSearchService,
            MarkdownHybridSearchService hybridSearchService) {
        return new RagRetrievalExperimentEvaluator(evaluationService, denseSearchService,
                sparseSearchService, hybridSearchService);
    }

    @Bean
    RagRetrievalExperimentContractProvider ragRetrievalExperimentContractProvider(
            RagRetrievalExperimentEvaluator evaluator,
            MarkdownImportRegistry importRegistry,
            KnowledgeIndexVersionMetadata indexVersion,
            @Value("${java-interview-rag.evaluation.git-sha:auto}") String gitSha) {
        return new RagRetrievalExperimentContractProvider(
                evaluator, importRegistry, indexVersion, GitRevisionResolver.resolve(gitSha));
    }

    @Bean(name = "ragRetrievalExperimentExecutor")
    Executor ragRetrievalExperimentExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(2);
        executor.setThreadNamePrefix("rag-retrieval-experiment-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }

    @Bean
    RagRetrievalExperimentService ragRetrievalExperimentService(
            RagRetrievalExperimentEvaluator evaluator,
            RagRetrievalExperimentContractProvider contractProvider,
            RagRetrievalExperimentStore store,
            @Qualifier("ragRetrievalExperimentExecutor") Executor executor) {
        return new RagRetrievalExperimentService(evaluator, contractProvider, store, executor);
    }
}
