package com.javainterview.rag.modelprovider;

import com.javainterview.rag.knowledge.document.KnowledgeIndexVersionMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@Profile("storage")
public class ModelProviderConfiguration {

    @Bean
    ModelProviderStore modelProviderStore(ModelProviderMapper mapper,
                                          PlatformTransactionManager transactionManager) {
        return new MySqlModelProviderStore(mapper, new TransactionTemplate(transactionManager));
    }

    @Bean
    ProviderApiKeyCipher providerApiKeyCipher(
            @Value("${java-interview-rag.provider.encryption-key:}") String encryptionKey) {
        return new ProviderApiKeyCipher(encryptionKey);
    }

    @Bean
    ModelProviderClientFactory modelProviderClientFactory(
            @Qualifier("dashScopeEmbeddingRestClientBuilder") RestClient.Builder restClientBuilder,
            @Qualifier("dashScopeEmbeddingRetryTemplate") RetryTemplate embeddingRetryTemplate) {
        return new OpenAiCompatibleModelProviderClientFactory(restClientBuilder, embeddingRetryTemplate);
    }

    @Bean
    ModelProviderRegistry modelProviderRegistry(
            ModelProviderStore store,
            ProviderApiKeyCipher cipher,
            ModelProviderClientFactory clientFactory,
            KnowledgeIndexVersionMetadata indexVersion,
            @Value("${spring.ai.openai.base-url}") String baseUrl,
            @Value("${spring.ai.openai.api-key}") String apiKey,
            @Value("${spring.ai.openai.chat.options.model}") String chatModel,
            @Value("${spring.ai.openai.chat.options.temperature:0.2}") Double temperature) {
        return new ModelProviderRegistry(store, cipher, clientFactory,
                new ModelProviderRegistry.LegacyProviderSeed(
                        "configured-default", "Configured Default", baseUrl, apiKey,
                        chatModel, indexVersion.embeddingModel(), indexVersion.embeddingDimensions(), temperature));
    }

    @Bean
    @Primary
    ChatModel routingChatModel(ModelProviderRegistry registry) {
        return new RoutingChatModel(registry);
    }

    @Bean
    @Primary
    EmbeddingModel routingEmbeddingModel(ModelProviderRegistry registry) {
        return new RoutingEmbeddingModel(registry);
    }
}
