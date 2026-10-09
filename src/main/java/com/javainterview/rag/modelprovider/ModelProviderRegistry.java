package com.javainterview.rag.modelprovider;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingModel;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Supplier;

public class ModelProviderRegistry {
    private final ModelProviderStore store;
    private final ProviderApiKeyCipher cipher;
    private final ModelProviderClientFactory clientFactory;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final Map<String, ChatModel> chatModels = new ConcurrentHashMap<>();
    private final Map<String, EmbeddingModel> embeddingModels = new ConcurrentHashMap<>();
    private volatile Snapshot snapshot;

    public ModelProviderRegistry(ModelProviderStore store,
                                 ProviderApiKeyCipher cipher,
                                 ModelProviderClientFactory clientFactory) {
        this(store, cipher, clientFactory, null);
    }

    public ModelProviderRegistry(ModelProviderStore store,
                                 ProviderApiKeyCipher cipher,
                                 ModelProviderClientFactory clientFactory,
                                 LegacyProviderSeed legacySeed) {
        this.store = store;
        this.cipher = cipher;
        this.clientFactory = clientFactory;
        if (store.findAll().isEmpty()) {
            if (legacySeed == null) {
                throw new IllegalStateException("模型 Provider 尚未初始化");
            }
            initializeLegacy(legacySeed);
        }
        reloadUnlocked();
    }

    public ModelProviderRuntimeConfig currentChatProvider() {
        Snapshot current = snapshot;
        return requiredProvider(current.defaults().chatProviderId(), current.providers(), ModelCapability.CHAT);
    }

    public ModelProviderRuntimeConfig currentEmbeddingProvider() {
        Snapshot current = snapshot;
        return requiredProvider(current.defaults().embeddingProviderId(), current.providers(), ModelCapability.EMBEDDING);
    }

    public String currentChatContract() {
        return currentChatProvider().chatContract();
    }

    public ChatModel chatModel(String providerId) {
        ModelProviderRuntimeConfig provider = requiredProvider(
                providerId, snapshot.providers(), ModelCapability.CHAT);
        return chatModels.computeIfAbsent(cacheKey(provider), ignored -> clientFactory.createChatModel(provider));
    }

    public EmbeddingModel embeddingModel(String providerId) {
        ModelProviderRuntimeConfig provider = requiredProvider(
                providerId, snapshot.providers(), ModelCapability.EMBEDDING);
        return embeddingModels.computeIfAbsent(cacheKey(provider), ignored -> clientFactory.createEmbeddingModel(provider));
    }

    public EmbeddingModel currentEmbeddingModel() {
        return embeddingModel(snapshot.defaults().embeddingProviderId());
    }

    public <T> T callChat(Function<ChatModel, T> invocation) {
        lock.readLock().lock();
        try {
            return invocation.apply(chatModel(snapshot.defaults().chatProviderId()));
        } finally {
            lock.readLock().unlock();
        }
    }

    public <T> T callChat(String providerId, Function<ChatModel, T> invocation) {
        lock.readLock().lock();
        try {
            return invocation.apply(chatModel(providerId));
        } finally {
            lock.readLock().unlock();
        }
    }

    public Flux<ChatResponse> streamChat(Prompt prompt) {
        return Flux.defer(() -> {
            ChatModel selectedModel;
            lock.readLock().lock();
            try {
                selectedModel = chatModel(snapshot.defaults().chatProviderId());
            } finally {
                lock.readLock().unlock();
            }
            // 固定本次模型实例；响应和取消可能切换线程，不能跨线程持有读锁。
            return selectedModel.stream(prompt);
        });
    }

    public <T> T callEmbedding(Function<EmbeddingModel, T> invocation) {
        lock.readLock().lock();
        try {
            return invocation.apply(embeddingModel(snapshot.defaults().embeddingProviderId()));
        } finally {
            lock.readLock().unlock();
        }
    }

    public <T> T callEmbedding(String providerId, Function<EmbeddingModel, T> invocation) {
        lock.readLock().lock();
        try {
            return invocation.apply(embeddingModel(providerId));
        } finally {
            lock.readLock().unlock();
        }
    }

    public <T> T mutate(Supplier<T> mutation) {
        return exclusiveMutation(() -> {
            T result = mutation.get();
            reloadUnlocked();
            return result;
        });
    }

    public <T> T exclusiveMutation(Supplier<T> mutation) {
        lock.writeLock().lock();
        try {
            return mutation.get();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void reload() {
        exclusiveMutation(() -> {
            reloadUnlocked();
            return null;
        });
    }

    private void initializeLegacy(LegacyProviderSeed seed) {
        ProviderApiKeyCipher.EncryptedValue encrypted = cipher.encrypt(seed.apiKey());
        LocalDateTime now = LocalDateTime.now();
        ModelProvider provider = new ModelProvider(seed.id(), seed.name(), seed.baseUrl(),
                encrypted.nonce(), encrypted.ciphertext(), seed.chatModel(), seed.embeddingModel(),
                seed.embeddingDimensions(), seed.temperature(), true, 0, now, now);
        store.initialize(provider, new ModelProviderDefaults(seed.id(), seed.id(), 0, now));
    }

    private void reloadUnlocked() {
        Map<String, ModelProviderRuntimeConfig> providers = new LinkedHashMap<>();
        for (ModelProvider provider : store.findAll()) {
            if (!provider.enabled()) {
                continue;
            }
            providers.put(provider.id(), new ModelProviderRuntimeConfig(
                    provider.id(), provider.version(), provider.baseUrl(),
                    cipher.decrypt(provider.apiKeyNonce(), provider.apiKeyCiphertext()),
                    provider.chatModel(), provider.embeddingModel(), provider.embeddingDimensions(),
                    provider.temperature()));
        }
        ModelProviderDefaults defaults = store.defaults();
        requiredProvider(defaults.chatProviderId(), providers, ModelCapability.CHAT);
        requiredProvider(defaults.embeddingProviderId(), providers, ModelCapability.EMBEDDING);
        snapshot = new Snapshot(Map.copyOf(providers), defaults);
        chatModels.clear();
        embeddingModels.clear();
    }

    private ModelProviderRuntimeConfig requiredProvider(
            String id, Map<String, ModelProviderRuntimeConfig> providers, ModelCapability capability) {
        ModelProviderRuntimeConfig provider = providers.get(id);
        if (provider == null) {
            throw new NoSuchElementException("模型 Provider 不存在或未启用: " + id);
        }
        if (capability == ModelCapability.CHAT && provider.chatModel() == null) {
            throw new IllegalStateException("Provider 未配置 Chat 能力: " + id);
        }
        if (capability == ModelCapability.EMBEDDING && provider.embeddingModel() == null) {
            throw new IllegalStateException("Provider 未配置 Embedding 能力: " + id);
        }
        return provider;
    }

    private String cacheKey(ModelProviderRuntimeConfig provider) {
        return provider.id() + "#" + provider.version();
    }

    private record Snapshot(Map<String, ModelProviderRuntimeConfig> providers,
                            ModelProviderDefaults defaults) {
    }

    public record LegacyProviderSeed(
            String id,
            String name,
            String baseUrl,
            String apiKey,
            String chatModel,
            String embeddingModel,
            Integer embeddingDimensions,
            Double temperature) {
    }
}
