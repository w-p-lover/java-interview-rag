package com.javainterview.rag.modelprovider;

import com.javainterview.rag.knowledge.document.KnowledgeIndexVersionMetadata;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Profile("storage")
public class ModelProviderService {

    private static final String CHAT_PROBE_PROMPT = "这是固定连通性测试，不包含用户数据。"
            + "请严格返回 JSON：{\"status\":\"MODEL_OK\"}，不要返回其他内容。";
    private static final String EMBEDDING_PROBE_TEXT = "java interview rag provider connectivity probe";

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final KnowledgeIndexVersionMetadata indexVersion;
    private final String chatBaseUrl;
    private final String chatApiKey;
    private final String chatModelName;
    private final String embeddingBaseUrl;
    private final String embeddingApiKey;
    private final String embeddingModelName;

    public ModelProviderService(
            ChatClient chatClient,
            EmbeddingModel embeddingModel,
            KnowledgeIndexVersionMetadata indexVersion,
            @Value("${spring.ai.openai.base-url:}") String chatBaseUrl,
            @Value("${spring.ai.openai.api-key:}") String chatApiKey,
            @Value("${spring.ai.openai.chat.options.model:unknown}") String chatModelName,
            @Value("${spring.ai.dashscope.base-url:https://dashscope.aliyuncs.com/api/v1}") String embeddingBaseUrl,
            @Value("${spring.ai.dashscope.api-key:}") String embeddingApiKey,
            @Value("${spring.ai.dashscope.embedding.options.model:text-embedding-v3}") String embeddingModelName) {
        this.chatClient = chatClient;
        this.embeddingModel = embeddingModel;
        this.indexVersion = indexVersion;
        this.chatBaseUrl = text(chatBaseUrl);
        this.chatApiKey = text(chatApiKey);
        this.chatModelName = text(chatModelName);
        this.embeddingBaseUrl = text(embeddingBaseUrl);
        this.embeddingApiKey = text(embeddingApiKey);
        this.embeddingModelName = text(embeddingModelName);
    }

    public ModelProviderOverview overview() {
        return new ModelProviderOverview(Instant.now(), List.of(
                new ModelProviderOverview.Capability(
                        ModelCapability.CHAT,
                        "dashscope-openai-compatible",
                        "DashScope OpenAI Compatible",
                        chatModelName,
                        endpointOrigin(chatBaseUrl),
                        configured(chatApiKey, chatModelName, chatBaseUrl),
                        !chatApiKey.isBlank(),
                        true,
                        null,
                        List.of("知识问答", "面试出题与评分", "简历分析", "复习助手")),
                new ModelProviderOverview.Capability(
                        ModelCapability.EMBEDDING,
                        "dashscope-embedding",
                        "DashScope Embedding",
                        embeddingModelName,
                        endpointOrigin(embeddingBaseUrl),
                        configured(embeddingApiKey, embeddingModelName, embeddingBaseUrl),
                        !embeddingApiKey.isBlank(),
                        true,
                        indexVersion.embeddingDimensions(),
                        List.of("资料导入与重建", "Dense 检索", "索引代际发布"))));
    }

    public ModelProviderTestResult test(ModelCapability capability) {
        long startedAt = System.nanoTime();
        return switch (capability) {
            case CHAT -> testChat(startedAt);
            case EMBEDDING -> testEmbedding(startedAt);
        };
    }

    private ModelProviderTestResult testChat(long startedAt) {
        if (!configured(chatApiKey, chatModelName, chatBaseUrl)) {
            return failure(ModelCapability.CHAT, "CONFIGURATION_MISSING", "Chat Provider 配置不完整。",
                    chatModelName, null, startedAt);
        }
        try {
            ChatProbe probe = chatClient.prompt(CHAT_PROBE_PROMPT).call().entity(ChatProbe.class);
            if (probe == null || !"MODEL_OK".equals(probe.status())) {
                return failure(ModelCapability.CHAT, "STRUCTURED_OUTPUT_INVALID",
                        "模型已响应，但结构化输出不符合约定。", chatModelName, null, startedAt);
            }
            return success(ModelCapability.CHAT, "Chat 结构化输出验证通过。",
                    chatModelName, null, startedAt);
        } catch (Exception exception) {
            return classifiedFailure(ModelCapability.CHAT, chatModelName, null, exception, startedAt);
        }
    }

    private ModelProviderTestResult testEmbedding(long startedAt) {
        if (!configured(embeddingApiKey, embeddingModelName, embeddingBaseUrl)) {
            return failure(ModelCapability.EMBEDDING, "CONFIGURATION_MISSING",
                    "Embedding Provider 配置不完整。", embeddingModelName, null, startedAt);
        }
        try {
            float[] vector = embeddingModel.embed(EMBEDDING_PROBE_TEXT);
            int dimensions = vector == null ? 0 : vector.length;
            if (dimensions != indexVersion.embeddingDimensions()) {
                return failure(ModelCapability.EMBEDDING, "DIMENSION_MISMATCH",
                        "Embedding 已响应，但实际维度与索引契约不一致。",
                        embeddingModelName, dimensions, startedAt);
            }
            return success(ModelCapability.EMBEDDING, "Embedding 维度验证通过。",
                    embeddingModelName, dimensions, startedAt);
        } catch (Exception exception) {
            return classifiedFailure(ModelCapability.EMBEDDING, embeddingModelName,
                    null, exception, startedAt);
        }
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

    private boolean configured(String apiKey, String modelName, String baseUrl) {
        return !apiKey.isBlank() && !modelName.isBlank() && !baseUrl.isBlank();
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
}
