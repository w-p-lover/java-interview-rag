package com.javainterview.rag.modelprovider;

import com.javainterview.rag.knowledge.document.KnowledgeIndexVersionMetadata;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Profile("storage")
public class ModelProviderService {

    private static final String CHAT_PROBE_PROMPT = "这是固定连通性测试，不包含用户数据。"
            + "请严格返回 JSON：{\"status\":\"MODEL_OK\"}，不要返回其他内容。";
    private static final String EMBEDDING_PROBE_TEXT = "java interview rag provider connectivity probe";

    private final ModelProviderRegistry registry;
    private final ModelProviderStore store;
    private final ProviderApiKeyCipher cipher;
    private final KnowledgeIndexVersionMetadata indexVersion;

    public ModelProviderService(
            ModelProviderRegistry registry,
            ModelProviderStore store,
            ProviderApiKeyCipher cipher,
            KnowledgeIndexVersionMetadata indexVersion) {
        this.registry = registry;
        this.store = store;
        this.cipher = cipher;
        this.indexVersion = indexVersion;
    }

    public ModelProviderOverview overview() {
        ModelProviderDefaults defaults = store.defaults();
        List<ModelProvider> providers = store.findAll();
        List<ModelProviderOverview.Provider> providerViews = providers.stream()
                .map(provider -> providerView(provider, defaults))
                .toList();
        ModelProvider chat = requiredProvider(defaults.chatProviderId());
        ModelProvider embedding = requiredProvider(defaults.embeddingProviderId());
        return new ModelProviderOverview(
                Instant.now(),
                providerViews,
                defaults.chatProviderId(),
                defaults.embeddingProviderId(),
                defaults.version(),
                List.of(
                        capability(chat, ModelCapability.CHAT, true),
                        capability(embedding, ModelCapability.EMBEDDING, true)));
    }

    public ModelProviderTestResult test(ModelCapability capability) {
        ModelProviderDefaults defaults = store.defaults();
        return test(capability == ModelCapability.CHAT
                ? defaults.chatProviderId() : defaults.embeddingProviderId(), capability);
    }

    public ModelProviderTestResult test(String providerId, ModelCapability capability) {
        ModelProvider provider = requiredProvider(providerId);
        long startedAt = System.nanoTime();
        return switch (capability) {
            case CHAT -> testChat(provider, startedAt);
            case EMBEDDING -> testEmbedding(provider, startedAt);
        };
    }

    public ModelProviderOverview create(ModelProviderRequest request) {
        ProviderInput input = validate(request, true);
        ProviderApiKeyCipher.EncryptedValue encrypted = cipher.encrypt(input.apiKey());
        LocalDateTime now = LocalDateTime.now();
        ModelProvider provider = new ModelProvider(input.id(), input.name(), input.baseUrl(),
                encrypted.nonce(), encrypted.ciphertext(), input.chatModel(), input.embeddingModel(),
                input.embeddingDimensions(), input.temperature(), input.enabled(), 0, now, now);
        registry.mutate(() -> {
            if (!store.create(provider)) {
                throw new ModelProviderConflictException("Provider ID 已存在");
            }
            return null;
        });
        return overview();
    }

    public ModelProviderOverview update(String providerId, ModelProviderRequest request) {
        String id = requireId(providerId);
        ProviderInput input = validate(request, false);
        if (request.id() != null && !request.id().isBlank() && !id.equals(request.id().strip())) {
            throw new IllegalArgumentException("Provider ID 不允许修改");
        }
        long expectedVersion = requiredVersion(request.version());
        registry.mutate(() -> {
            ModelProvider existing = requiredProvider(id);
            ModelProviderDefaults defaults = store.defaults();
            boolean currentDefault = id.equals(defaults.chatProviderId())
                    || id.equals(defaults.embeddingProviderId());
            if (currentDefault && store.activeModelTaskCount() > 0) {
                throw new ModelProviderConflictException("存在排队或运行中的模型任务，暂不能更新当前默认 Provider");
            }
            if (id.equals(defaults.chatProviderId()) && (!input.enabled() || input.chatModel() == null)) {
                throw new ModelProviderConflictException("默认 Chat Provider 不能禁用或移除 Chat 能力");
            }
            if (id.equals(defaults.embeddingProviderId())
                    && (!input.enabled() || input.embeddingModel() == null)) {
                throw new ModelProviderConflictException("默认 Embedding Provider 不能禁用或移除 Embedding 能力");
            }
            if (id.equals(defaults.embeddingProviderId())
                    && (!input.embeddingModel().equals(indexVersion.embeddingModel())
                    || !input.embeddingDimensions().equals(indexVersion.embeddingDimensions()))) {
                throw new ModelProviderConflictException("默认 Embedding Provider 必须保持当前索引模型名和维度");
            }
            ProviderApiKeyCipher.EncryptedValue encrypted = input.apiKey().isBlank()
                    ? new ProviderApiKeyCipher.EncryptedValue(
                    existing.apiKeyNonce(), existing.apiKeyCiphertext())
                    : cipher.encrypt(input.apiKey());
            ModelProvider updated = new ModelProvider(id, input.name(), input.baseUrl(),
                    encrypted.nonce(), encrypted.ciphertext(), input.chatModel(), input.embeddingModel(),
                    input.embeddingDimensions(), input.temperature(), input.enabled(),
                    existing.version(), existing.createdAt(), LocalDateTime.now());
            if (!store.update(updated, expectedVersion)) {
                throw new ModelProviderConflictException("Provider 已被其他操作更新，请刷新后重试");
            }
            return null;
        });
        return overview();
    }

    public ModelProviderOverview delete(String providerId, Long version) {
        String id = requireId(providerId);
        long expectedVersion = requiredVersion(version);
        registry.mutate(() -> {
            requiredProvider(id);
            ModelProviderDefaults defaults = store.defaults();
            if (id.equals(defaults.chatProviderId()) || id.equals(defaults.embeddingProviderId())) {
                throw new ModelProviderConflictException("当前默认 Provider 不能删除，请先切换默认值");
            }
            if (!store.delete(id, expectedVersion)) {
                throw new ModelProviderConflictException("Provider 已被其他操作更新，请刷新后重试");
            }
            return null;
        });
        return overview();
    }

    public ModelProviderOverview updateDefaults(ModelProviderDefaultsRequest request) {
        Objects.requireNonNull(request, "Provider 默认值请求不能为空");
        String chatProviderId = requireId(request.chatProviderId());
        String embeddingProviderId = requireId(request.embeddingProviderId());
        long expectedVersion = requiredVersion(request.version());
        registry.mutate(() -> {
            ModelProviderDefaults existing = store.defaults();
            if (existing.version() != expectedVersion) {
                throw new ModelProviderConflictException("Provider 默认值已被其他操作更新，请刷新后重试");
            }
            ModelProvider chat = requiredProvider(chatProviderId);
            ModelProvider embedding = requiredProvider(embeddingProviderId);
            requireCapability(chat, ModelCapability.CHAT);
            requireCapability(embedding, ModelCapability.EMBEDDING);
            if (!embedding.embeddingModel().equals(indexVersion.embeddingModel())
                    || !embedding.embeddingDimensions().equals(indexVersion.embeddingDimensions())) {
                throw new ModelProviderConflictException("Embedding 模型名和维度必须与当前索引契约完全一致");
            }
            boolean changed = !chatProviderId.equals(existing.chatProviderId())
                    || !embeddingProviderId.equals(existing.embeddingProviderId());
            if (!changed) {
                return null;
            }
            if (store.activeModelTaskCount() > 0) {
                throw new ModelProviderConflictException("存在排队或运行中的模型任务，暂不能切换默认 Provider");
            }
            ModelProviderDefaults updated = new ModelProviderDefaults(
                    chatProviderId, embeddingProviderId, existing.version(), LocalDateTime.now());
            if (!store.updateDefaults(updated, expectedVersion)) {
                throw new ModelProviderConflictException("Provider 默认值已被其他操作更新，请刷新后重试");
            }
            return null;
        });
        return overview();
    }

    private ModelProviderTestResult testChat(ModelProvider provider, long startedAt) {
        if (!provider.enabled() || !provider.supportsChat()) {
            return failure(ModelCapability.CHAT, "CONFIGURATION_MISSING", "Chat Provider 配置不完整。",
                    provider.chatModel(), null, startedAt);
        }
        try {
            ChatProbe probe = registry.callChat(provider.id(), model ->
                    ChatClient.builder(model).build().prompt(CHAT_PROBE_PROMPT)
                            .call().entity(ChatProbe.class));
            if (probe == null || !"MODEL_OK".equals(probe.status())) {
                return failure(ModelCapability.CHAT, "STRUCTURED_OUTPUT_INVALID",
                        "模型已响应，但结构化输出不符合约定。", provider.chatModel(), null, startedAt);
            }
            return success(ModelCapability.CHAT, "Chat 结构化输出验证通过。",
                    provider.chatModel(), null, startedAt);
        } catch (Exception exception) {
            return classifiedFailure(ModelCapability.CHAT, provider.chatModel(), null, exception, startedAt);
        }
    }

    private ModelProviderTestResult testEmbedding(ModelProvider provider, long startedAt) {
        if (!provider.enabled() || !provider.supportsEmbedding()) {
            return failure(ModelCapability.EMBEDDING, "CONFIGURATION_MISSING",
                    "Embedding Provider 配置不完整。", provider.embeddingModel(), null, startedAt);
        }
        try {
            float[] vector = registry.callEmbedding(
                    provider.id(), model -> model.embed(EMBEDDING_PROBE_TEXT));
            int dimensions = vector == null ? 0 : vector.length;
            if (dimensions != provider.embeddingDimensions()) {
                return failure(ModelCapability.EMBEDDING, "DIMENSION_MISMATCH",
                        "Embedding 已响应，但实际维度与 Provider 配置不一致。",
                        provider.embeddingModel(), dimensions, startedAt);
            }
            return success(ModelCapability.EMBEDDING, "Embedding 维度验证通过。",
                    provider.embeddingModel(), dimensions, startedAt);
        } catch (Exception exception) {
            return classifiedFailure(ModelCapability.EMBEDDING, provider.embeddingModel(),
                    null, exception, startedAt);
        }
    }

    private ModelProviderOverview.Provider providerView(
            ModelProvider provider, ModelProviderDefaults defaults) {
        return new ModelProviderOverview.Provider(
                provider.id(), provider.name(), provider.baseUrl(), endpointOrigin(provider.baseUrl()),
                true, provider.chatModel(), provider.embeddingModel(), provider.embeddingDimensions(),
                provider.temperature(), provider.enabled(), provider.version(),
                provider.id().equals(defaults.chatProviderId()),
                provider.id().equals(defaults.embeddingProviderId()));
    }

    private ModelProviderOverview.Capability capability(
            ModelProvider provider, ModelCapability capability, boolean defaultForCapability) {
        String modelName = capability == ModelCapability.CHAT
                ? provider.chatModel() : provider.embeddingModel();
        return new ModelProviderOverview.Capability(
                capability, provider.id(), provider.name(), modelName,
                endpointOrigin(provider.baseUrl()), provider.enabled(), true,
                defaultForCapability,
                capability == ModelCapability.EMBEDDING ? indexVersion.embeddingDimensions() : null,
                capability == ModelCapability.CHAT
                        ? List.of("知识问答", "面试出题与评分", "简历分析", "复习助手")
                        : List.of("资料导入与重建", "Dense 检索", "索引代际发布"));
    }

    private ProviderInput validate(ModelProviderRequest request, boolean creating) {
        Objects.requireNonNull(request, "Provider 请求不能为空");
        String id = creating ? requireId(request.id()) : text(request.id());
        String name = requireText(request.name(), 100, "Provider 名称不能为空");
        String baseUrl = requireBaseUrl(request.baseUrl());
        String apiKey = text(request.apiKey());
        if (creating && apiKey.isBlank()) {
            throw new IllegalArgumentException("Provider API Key 不能为空");
        }
        String chatModel = optionalText(request.chatModel(), 100, "Chat 模型名不能超过 100 个字符");
        String embeddingModel = optionalText(
                request.embeddingModel(), 100, "Embedding 模型名不能超过 100 个字符");
        if (chatModel == null && embeddingModel == null) {
            throw new IllegalArgumentException("Provider 至少需要配置一种模型能力");
        }
        Integer dimensions = request.embeddingDimensions();
        if (embeddingModel != null && (dimensions == null || dimensions <= 0)) {
            throw new IllegalArgumentException("Embedding Provider 必须配置有效维度");
        }
        if (embeddingModel == null) {
            dimensions = null;
        }
        Double temperature = request.temperature();
        if (temperature != null && (temperature < 0 || temperature > 2)) {
            throw new IllegalArgumentException("模型温度必须在 0 到 2 之间");
        }
        return new ProviderInput(id, name, baseUrl, apiKey, chatModel,
                embeddingModel, dimensions, temperature, request.enabled());
    }

    private ModelProvider requiredProvider(String id) {
        return store.findById(requireId(id))
                .orElseThrow(() -> new NoSuchElementException("模型 Provider 不存在: " + id));
    }

    private void requireCapability(ModelProvider provider, ModelCapability capability) {
        if (!provider.enabled()) {
            throw new ModelProviderConflictException("默认 Provider 必须处于启用状态");
        }
        if (capability == ModelCapability.CHAT && !provider.supportsChat()) {
            throw new ModelProviderConflictException("Provider 未配置 Chat 能力: " + provider.id());
        }
        if (capability == ModelCapability.EMBEDDING && !provider.supportsEmbedding()) {
            throw new ModelProviderConflictException("Provider 未配置 Embedding 能力: " + provider.id());
        }
    }

    private String requireId(String value) {
        String id = requireText(value, 32, "Provider ID 不能为空");
        if (!id.matches("[a-z0-9][a-z0-9-]*")) {
            throw new IllegalArgumentException("Provider ID 只能包含小写字母、数字和连字符");
        }
        return id;
    }

    private long requiredVersion(Long value) {
        if (value == null || value < 0) {
            throw new IllegalArgumentException("必须提交有效的 Provider 版本");
        }
        return value;
    }

    private String requireBaseUrl(String value) {
        String baseUrl = requireText(value, 500, "Provider 地址不能为空");
        try {
            URI uri = URI.create(baseUrl);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getRawQuery() != null || uri.getRawFragment() != null) {
                throw new IllegalArgumentException("Provider 地址必须是无凭据和查询参数的 HTTP(S) 地址");
            }
            return baseUrl;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Provider 地址必须是无凭据和查询参数的 HTTP(S) 地址");
        }
    }

    private String requireText(String value, int maxLength, String message) {
        String normalized = text(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(message.replace("不能为空", "不能超过 " + maxLength + " 个字符"));
        }
        return normalized;
    }

    private String optionalText(String value, int maxLength, String message) {
        String normalized = text(value);
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private ModelProviderTestResult classifiedFailure(
            ModelCapability capability,
            String modelName,
            Integer actualDimensions,
            Exception exception,
            long startedAt) {
        if (hasCause(exception, SocketTimeoutException.class)
                || hasCause(exception, HttpTimeoutException.class)
                || hasCause(exception, TimeoutException.class)) {
            return failure(capability, "TIMEOUT", "模型服务响应超时。",
                    modelName, actualDimensions, startedAt);
        }
        if (hasCause(exception, ConnectException.class) || hasCause(exception, UnknownHostException.class)) {
            return failure(capability, "NETWORK", "无法连接模型服务。",
                    modelName, actualDimensions, startedAt);
        }
        String messages = causeMessages(exception).toLowerCase(Locale.ROOT);
        if (messages.contains("http 401") || messages.contains("http 403")
                || messages.contains("unauthorized") || messages.contains("forbidden")) {
            return failure(capability, "AUTHENTICATION", "模型服务凭据无效或无权访问。",
                    modelName, actualDimensions, startedAt);
        }
        if (messages.contains("http 429") || messages.contains("too many requests")) {
            return failure(capability, "RATE_LIMITED", "模型服务触发限流。",
                    modelName, actualDimensions, startedAt);
        }
        if (hasCause(exception, TransientAiException.class)
                || hasCause(exception, NonTransientAiException.class)
                || messages.matches("(?s).*http 5\\d\\d.*")) {
            return failure(capability, "PROVIDER_ERROR", "模型服务返回错误。",
                    modelName, actualDimensions, startedAt);
        }
        return failure(capability, "UNKNOWN", "模型能力验证失败。",
                modelName, actualDimensions, startedAt);
    }

    private ModelProviderTestResult success(
            ModelCapability capability, String message, String modelName,
            Integer actualDimensions, long startedAt) {
        return new ModelProviderTestResult(capability, true, "OK", message, modelName,
                actualDimensions, elapsedMillis(startedAt), Instant.now());
    }

    private ModelProviderTestResult failure(
            ModelCapability capability, String code, String message, String modelName,
            Integer actualDimensions, long startedAt) {
        return new ModelProviderTestResult(capability, false, code, message, modelName,
                actualDimensions, elapsedMillis(startedAt), Instant.now());
    }

    private long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private String endpointOrigin(String baseUrl) {
        if (baseUrl.isBlank()) {
            return "未配置";
        }
        try {
            URI uri = URI.create(baseUrl);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return "配置无效";
            }
            String host = uri.getHost().contains(":") ? "[" + uri.getHost() + "]" : uri.getHost();
            String port = uri.getPort() < 0 ? "" : ":" + uri.getPort();
            return uri.getScheme() + "://" + host + port;
        } catch (IllegalArgumentException exception) {
            return "配置无效";
        }
    }

    private String text(String value) {
        return value == null ? "" : value.strip();
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String causeMessages(Throwable throwable) {
        StringBuilder messages = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null) {
                messages.append(' ').append(current.getMessage());
            }
            current = current.getCause();
        }
        return messages.toString();
    }

    record ChatProbe(String status) {
    }

    private record ProviderInput(
            String id,
            String name,
            String baseUrl,
            String apiKey,
            String chatModel,
            String embeddingModel,
            Integer embeddingDimensions,
            Double temperature,
            boolean enabled) {
    }
}
