package com.javainterview.rag.modelprovider;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ModelProviderRegistryTest {

    @Test
    void routesDefaultsAndPublishesVersionedContract() {
        InMemoryStore store = new InMemoryStore(provider("primary", "chat-a", "embed-a", 3));
        ModelProviderClientFactory factory = new StubFactory();
        ModelProviderRegistry registry = new ModelProviderRegistry(store,
                new ProviderApiKeyCipher("test-key"), factory);

        assertEquals("primary/chat-a#3", registry.currentChatContract());
        assertEquals(3, registry.currentEmbeddingModel().dimensions());
        assertEquals("chat-a", registry.currentChatProvider().chatModel());
    }

    @Test
    void exclusiveMutationWaitsForActiveChatCall() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        InMemoryStore store = new InMemoryStore(provider("primary", "chat-a", "embed-a", 3));
        ModelProviderClientFactory factory = new StubFactory(entered, release);
        ModelProviderRegistry registry = new ModelProviderRegistry(store,
                new ProviderApiKeyCipher("test-key"), factory);

        CompletableFuture<Void> call = CompletableFuture.runAsync(
                () -> registry.callChat(model -> model.call(new Prompt("probe"))));
        assertTrue(entered.await(2, TimeUnit.SECONDS));
        CompletableFuture<Void> mutation = CompletableFuture.runAsync(
                () -> registry.exclusiveMutation(() -> null));

        Thread.sleep(100);
        assertFalse(mutation.isDone());
        release.countDown();
        call.get(2, TimeUnit.SECONDS);
        mutation.get(2, TimeUnit.SECONDS);
    }

    private ModelProvider provider(String id, String chatModel, String embeddingModel, long version) {
        ProviderApiKeyCipher.EncryptedValue encrypted =
                new ProviderApiKeyCipher("test-key").encrypt("sk-test");
        LocalDateTime now = LocalDateTime.now();
        return new ModelProvider(id, id, "https://example.com/compatible-mode",
                encrypted.nonce(), encrypted.ciphertext(), chatModel, embeddingModel,
                3, 0.2, true, version, now, now);
    }

    private static final class InMemoryStore implements ModelProviderStore {
        private final ModelProvider provider;
        private final ModelProviderDefaults defaults;

        private InMemoryStore(ModelProvider provider) {
            this.provider = provider;
            this.defaults = new ModelProviderDefaults(provider.id(), provider.id(), 0, LocalDateTime.now());
        }

        @Override public List<ModelProvider> findAll() { return List.of(provider); }
        @Override public Optional<ModelProvider> findById(String id) { return Optional.of(provider).filter(p -> p.id().equals(id)); }
        @Override public ModelProviderDefaults defaults() { return defaults; }
        @Override public void initialize(ModelProvider provider, ModelProviderDefaults defaults) { throw new UnsupportedOperationException(); }
        @Override public boolean create(ModelProvider provider) { throw new UnsupportedOperationException(); }
        @Override public boolean update(ModelProvider provider, long expectedVersion) { throw new UnsupportedOperationException(); }
        @Override public boolean delete(String id, long expectedVersion) { throw new UnsupportedOperationException(); }
        @Override public boolean updateDefaults(ModelProviderDefaults defaults, long expectedVersion) { throw new UnsupportedOperationException(); }
        @Override public long activeModelTaskCount() { return 0; }
    }

    private static final class StubFactory implements ModelProviderClientFactory {
        private final CountDownLatch entered;
        private final CountDownLatch release;

        private StubFactory() {
            this(null, null);
        }

        private StubFactory(CountDownLatch entered, CountDownLatch release) {
            this.entered = entered;
            this.release = release;
        }

        @Override
        public ChatModel createChatModel(ModelProviderRuntimeConfig provider) {
            ChatModel model = mock(ChatModel.class);
            if (entered == null) {
                return model;
            }
            return new ChatModel() {
                @Override
                public ChatResponse call(Prompt prompt) {
                    entered.countDown();
                    try {
                        release.await(2, TimeUnit.SECONDS);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                    return null;
                }
            };
        }

        @Override
        public EmbeddingModel createEmbeddingModel(ModelProviderRuntimeConfig provider) {
            return new EmbeddingModel() {
                @Override public EmbeddingResponse call(EmbeddingRequest request) { return null; }
                @Override public float[] embed(Document document) { return new float[provider.embeddingDimensions()]; }
                @Override public int dimensions() { return provider.embeddingDimensions(); }
            };
        }
    }
}
