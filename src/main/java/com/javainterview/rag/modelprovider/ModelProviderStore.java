package com.javainterview.rag.modelprovider;

import java.util.List;
import java.util.Optional;

public interface ModelProviderStore {
    List<ModelProvider> findAll();
    Optional<ModelProvider> findById(String id);
    ModelProviderDefaults defaults();
    void initialize(ModelProvider provider, ModelProviderDefaults defaults);
    boolean create(ModelProvider provider);
    boolean update(ModelProvider provider, long expectedVersion);
    boolean delete(String id, long expectedVersion);
    boolean updateDefaults(ModelProviderDefaults defaults, long expectedVersion);
    long activeModelTaskCount();
}
