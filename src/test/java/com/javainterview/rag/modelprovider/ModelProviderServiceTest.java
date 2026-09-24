package com.javainterview.rag.modelprovider;

import com.javainterview.rag.knowledge.document.KnowledgeIndexVersionMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;

import java.net.SocketTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ModelProviderServiceTest {

    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec callResponseSpec;
    private EmbeddingModel embeddingModel;
    private ModelProviderService service;

    @BeforeEach
    void setUp() {
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        callResponseSpec = mock(ChatClient.CallResponseSpec.class);
        embeddingModel = mock(EmbeddingModel.class);
        service = new ModelProviderService(
                chatClient,
                embeddingModel,
                new KnowledgeIndexVersionMetadata(
                        "text-embedding-v3", 1024, "chunk-v1", "parser-v1", "schema-v1"),
                "https://client:credential@dashscope.aliyuncs.com/compatible-mode/v1",
                "secret-chat-key",
                "qwen-test",
                "https://dashscope.aliyuncs.com/api/v1",
                "secret-embedding-key",
                "text-embedding-v3");
    }

    @Test
    void exposesResolvedCapabilitiesWithoutSecrets() {
        ModelProviderOverview overview = service.overview();

        assertEquals(2, overview.capabilities().size());
        ModelProviderOverview.Capability chat = overview.capabilities().get(0);
        assertEquals(ModelCapability.CHAT, chat.capability());
        assertEquals("qwen-test", chat.modelName());
        assertEquals("https://dashscope.aliyuncs.com", chat.endpointOrigin());
        assertTrue(chat.apiKeyConfigured());
        assertTrue(chat.usages().contains("知识问答"));
        assertFalse(overview.toString().contains("credential"));

        ModelProviderOverview.Capability embedding = overview.capabilities().get(1);
        assertEquals(ModelCapability.EMBEDDING, embedding.capability());
        assertEquals(1024, embedding.expectedDimensions());
        assertFalse(overview.toString().contains("secret-chat-key"));
        assertFalse(overview.toString().contains("secret-embedding-key"));
    }

    @Test
    void validatesStructuredChatOutput() {
        when(chatClient.prompt(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.entity(ModelProviderService.ChatProbe.class))
                .thenReturn(new ModelProviderService.ChatProbe("MODEL_OK"));

        ModelProviderTestResult result = service.test(ModelCapability.CHAT);

        assertTrue(result.success());
        assertEquals("OK", result.code());
        assertEquals("qwen-test", result.modelName());
        assertNull(result.actualDimensions());
        verify(embeddingModel, never()).embed(anyString());
    }

    @Test
    void validatesEmbeddingDimensions() {
        when(embeddingModel.embed(anyString())).thenReturn(new float[1024]);

        ModelProviderTestResult result = service.test(ModelCapability.EMBEDDING);

        assertTrue(result.success());
        assertEquals("OK", result.code());
        assertEquals(1024, result.actualDimensions());
    }

    @Test
    void reportsDimensionMismatchWithoutRetrying() {
        when(embeddingModel.embed(anyString())).thenReturn(new float[768]);

        ModelProviderTestResult result = service.test(ModelCapability.EMBEDDING);

        assertFalse(result.success());
        assertEquals("DIMENSION_MISMATCH", result.code());
        assertEquals(768, result.actualDimensions());
        verify(embeddingModel).embed(anyString());
    }

    @Test
    void classifiesTimeoutWithoutReturningProviderDetails() {
        when(chatClient.prompt(anyString())).thenThrow(
                new IllegalStateException("private endpoint and key", new SocketTimeoutException("timed out")));

        ModelProviderTestResult result = service.test(ModelCapability.CHAT);

        assertFalse(result.success());
        assertEquals("TIMEOUT", result.code());
        assertEquals("模型服务响应超时。", result.message());
        assertFalse(result.toString().contains("private endpoint"));
        assertFalse(result.toString().contains("key"));
    }
}
