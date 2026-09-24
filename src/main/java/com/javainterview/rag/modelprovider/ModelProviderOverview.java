package com.javainterview.rag.modelprovider;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record ModelProviderOverview(Instant generatedAt, List<Capability> capabilities) {

    public ModelProviderOverview {
        generatedAt = Objects.requireNonNull(generatedAt, "Provider 快照时间不能为空");
        capabilities = List.copyOf(Objects.requireNonNull(capabilities, "Provider 能力列表不能为空"));
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
