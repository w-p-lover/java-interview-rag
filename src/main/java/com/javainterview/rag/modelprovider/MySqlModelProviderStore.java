package com.javainterview.rag.modelprovider;

import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;

public class MySqlModelProviderStore implements ModelProviderStore {
    private final ModelProviderMapper mapper;
    private final TransactionTemplate transactions;

    public MySqlModelProviderStore(ModelProviderMapper mapper, TransactionTemplate transactions) {
        this.mapper = mapper;
        this.transactions = transactions;
    }

    @Override
    public List<ModelProvider> findAll() {
        return mapper.findAll().stream().map(this::domain).toList();
    }

    @Override
    public Optional<ModelProvider> findById(String id) {
        return Optional.ofNullable(mapper.findById(id)).map(this::domain);
    }

    @Override
    public ModelProviderDefaults defaults() {
        ModelProviderDefaultsEntity value = mapper.defaults();
        if (value == null) {
            throw new IllegalStateException("模型 Provider 默认值尚未初始化");
        }
        return defaults(value);
    }

    @Override
    public void initialize(ModelProvider provider, ModelProviderDefaults defaults) {
        transactions.executeWithoutResult(status -> {
            if (mapper.findAll().isEmpty()) {
                mapper.insert(entity(provider));
            }
            mapper.insertDefaults(entity(defaults));
        });
    }

    @Override
    public boolean create(ModelProvider provider) {
        return mapper.insert(entity(provider)) == 1;
    }

    @Override
    public boolean update(ModelProvider provider, long expectedVersion) {
        return mapper.update(entity(provider), expectedVersion) == 1;
    }

    @Override
    public boolean delete(String id, long expectedVersion) {
        return mapper.delete(id, expectedVersion) == 1;
    }

    @Override
    public boolean updateDefaults(ModelProviderDefaults defaults, long expectedVersion) {
        return mapper.updateDefaults(entity(defaults), expectedVersion) == 1;
    }

    @Override
    public long activeModelTaskCount() {
        return mapper.activeModelTaskCount();
    }

    private ModelProviderEntity entity(ModelProvider value) {
        return new ModelProviderEntity(value.id(), value.name(), value.baseUrl(), value.apiKeyNonce(),
                value.apiKeyCiphertext(), value.chatModel(), value.embeddingModel(),
                value.embeddingDimensions(), value.temperature(), value.enabled(), value.version(),
                value.createdAt(), value.updatedAt());
    }

    private ModelProviderDefaultsEntity entity(ModelProviderDefaults value) {
        return new ModelProviderDefaultsEntity(1, value.chatProviderId(), value.embeddingProviderId(),
                value.version(), value.updatedAt());
    }

    private ModelProvider domain(ModelProviderEntity value) {
        return new ModelProvider(value.getId(), value.getName(), value.getBaseUrl(),
                value.getApiKeyNonce(), value.getApiKeyCiphertext(), value.getChatModel(),
                value.getEmbeddingModel(), value.getEmbeddingDimensions(), value.getTemperature(),
                value.isEnabled(), value.getVersion(), value.getCreatedAt(), value.getUpdatedAt());
    }

    private ModelProviderDefaults defaults(ModelProviderDefaultsEntity value) {
        return new ModelProviderDefaults(value.getChatProviderId(), value.getEmbeddingProviderId(),
                value.getVersion(), value.getUpdatedAt());
    }
}
