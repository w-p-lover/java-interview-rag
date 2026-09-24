package com.javainterview.rag.modelprovider;

import com.javainterview.rag.knowledge.document.KnowledgeIndexVersionMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.embedding.EmbeddingModel;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModelProviderServiceTest {

    private ModelProviderRegistry registry;
    private ModelProviderStore store;
    private ProviderApiKeyCipher cipher;
    private ModelProviderService service;
    private ModelProvider current;
    private ModelProviderDefaults defaults;

    @BeforeEach
    void setUp() {
        registry = mock(ModelProviderRegistry.class);
        store = mock(ModelProviderStore.class);
        cipher = mock(ProviderApiKeyCipher.class);
        current = provider("current", "qwen-test", "text-embedding-v3", 1024, true, 3);
        defaults = new ModelProviderDefaults("current", "current", 2, LocalDateTime.now());
        when(store.findAll()).thenReturn(List.of(current));
        when(store.findById("current")).thenReturn(Optional.of(current));
        when(store.defaults()).thenReturn(defaults);
        when(registry.mutate(any())).thenAnswer(invocation ->
                ((Supplier<?>) invocation.getArgument(0)).get());
        service = new ModelProviderService(registry, store, cipher,
                new KnowledgeIndexVersionMetadata(
                        "text-embedding-v3", 1024, "chunk-v1", "parser-v1", "schema-v1"));
    }

    @Test
    void exposesProvidersAndDefaultsWithoutSecrets() {
        ModelProviderOverview overview = service.overview();

        assertEquals("current", overview.defaultChatProviderId());
        assertEquals(2, overview.capabilities().size());
        assertEquals(1, overview.providers().size());
        assertTrue(overview.providers().get(0).apiKeyConfigured());
        assertFalse(overview.toString().contains("cipher-current"));
        assertFalse(overview.toString().contains("nonce-current"));
    }

    @Test
    void retainsEncryptedApiKeyWhenUpdateKeyIsBlank() {
        when(store.update(any(), eq(3L))).thenReturn(true);
        ModelProviderRequest request = new ModelProviderRequest(
                "current", "Updated", "https://example.com/v1", "",
                "qwen-next", "text-embedding-v3", 1024, 0.3, true, 3L);

        service.update("current", request);

        ArgumentCaptor<ModelProvider> captor = ArgumentCaptor.forClass(ModelProvider.class);
        verify(store).update(captor.capture(), eq(3L));
        assertEquals("nonce-current", captor.getValue().apiKeyNonce());
        assertEquals("cipher-current", captor.getValue().apiKeyCiphertext());
        assertEquals("qwen-next", captor.getValue().chatModel());
    }

    @Test
    void rejectsChangingCurrentProviderWhileModelTaskIsActive() {
        when(store.activeModelTaskCount()).thenReturn(1L);
        ModelProviderRequest request = new ModelProviderRequest(
                "current", "Updated", "https://example.com/v1", "",
                "qwen-next", "text-embedding-v3", 1024, 0.3, true, 3L);

        ModelProviderConflictException exception = assertThrows(
                ModelProviderConflictException.class,
                () -> service.update("current", request));

        assertTrue(exception.getMessage().contains("运行中的模型任务"));
    }

    @Test
    void rejectsChangingCurrentEmbeddingContract() {
        ModelProviderRequest request = new ModelProviderRequest(
                "current", "Updated", "https://example.com/v1", "",
                "qwen-test", "different-space", 1024, 0.2, true, 3L);

        ModelProviderConflictException exception = assertThrows(
                ModelProviderConflictException.class,
                () -> service.update("current", request));

        assertTrue(exception.getMessage().contains("索引模型名和维度"));
    }

    @Test
    void rejectsIncompatibleEmbeddingDefault() {
        ModelProvider incompatible = provider(
                "alternate", "qwen-test", "different-space", 1024, true, 0);
        when(store.findById("alternate")).thenReturn(Optional.of(incompatible));

        ModelProviderConflictException exception = assertThrows(
                ModelProviderConflictException.class,
                () -> service.updateDefaults(
                        new ModelProviderDefaultsRequest("current", "alternate", 2L)));

        assertTrue(exception.getMessage().contains("索引契约"));
    }

    @Test
    void rejectsDefaultSwitchWhileModelTaskIsActive() {
        ModelProvider alternate = provider(
                "alternate", "qwen-next", "text-embedding-v3", 1024, true, 0);
        when(store.findById("alternate")).thenReturn(Optional.of(alternate));
        when(store.activeModelTaskCount()).thenReturn(1L);

        ModelProviderConflictException exception = assertThrows(
                ModelProviderConflictException.class,
                () -> service.updateDefaults(
                        new ModelProviderDefaultsRequest("alternate", "current", 2L)));

        assertTrue(exception.getMessage().contains("运行中的模型任务"));
    }

    @Test
    void rejectsDeletingCurrentDefault() {
        ModelProviderConflictException exception = assertThrows(
                ModelProviderConflictException.class,
                () -> service.delete("current", 3L));

        assertTrue(exception.getMessage().contains("不能删除"));
    }

    @Test
    void validatesProviderSpecificEmbeddingDimensions() {
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);
        when(embeddingModel.embed(anyString())).thenReturn(new float[1024]);
        when(registry.callEmbedding(eq("current"), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Function<EmbeddingModel, float[]> action = invocation.getArgument(1);
            return action.apply(embeddingModel);
        });

        ModelProviderTestResult result = service.test("current", ModelCapability.EMBEDDING);

        assertTrue(result.success());
        assertEquals(1024, result.actualDimensions());
    }

    private ModelProvider provider(String id, String chatModel, String embeddingModel,
                                   Integer dimensions, boolean enabled, long version) {
        LocalDateTime now = LocalDateTime.now();
        return new ModelProvider(id, id, "https://example.com/v1",
                "nonce-" + id, "cipher-" + id, chatModel, embeddingModel,
                dimensions, 0.2, enabled, version, now, now);
    }
}
