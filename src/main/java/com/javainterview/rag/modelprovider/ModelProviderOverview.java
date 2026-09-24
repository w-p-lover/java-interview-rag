package com.javainterview.rag.modelprovider;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ModelProviderOverview(
        Instant generatedAt,
        List<Provider> providers,
        String defaultChatProviderId,
        String defaultEmbeddingProviderId,
        long defaultsVersion,
        List<Capability> capabilities) {

    public ModelProviderOverview {
        generatedAt = Objects.requireNonNull(generatedAt, "Provider 快照时间不能为空");
        providers = List.copyOf(Objects.requireNonNull(providers, "Provider 列表不能为空"));
        defaultChatProviderId = Objects.requireNonNull(defaultChatProviderId, "默认 Chat Provider 不能为空");
        defaultEmbeddingProviderId = Objects.requireNonNull(
                defaultEmbeddingProviderId, "默认 Embedding Provider 不能为空");
        capabilities = List.copyOf(Objects.requireNonNull(capabilities, "Provider 能力列表不能为空"));
    }

    public record Provider(
            String id,
            String name,
            String baseUrl,
            String endpointOrigin,
            boolean apiKeyConfigured,
            String chatModel,
            String embeddingModel,
            Integer embeddingDimensions,
            Double temperature,
            boolean enabled,
            long version,
            boolean defaultForChat,
            boolean defaultForEmbedding) {
    }

    public record Capability(
            ModelCapability capability,
            String providerId,
            String providerName,
            String modelName,
            String endpointOrigin,
            boolean enabled,
            boolean apiKeyConfigured,
            boolean defaultForCapability,
            Integer expectedDimensions,
            List<String> usages) {

        public Capability {
            capability = Objects.requireNonNull(capability, "模型能力不能为空");
            usages = List.copyOf(Objects.requireNonNull(usages, "模型用途不能为空"));
        }
    }
}
