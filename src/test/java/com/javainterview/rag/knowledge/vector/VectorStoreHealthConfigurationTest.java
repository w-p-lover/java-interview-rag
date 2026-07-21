package com.javainterview.rag.knowledge.vector;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VectorStoreHealthConfigurationTest {

    @Test
    void doesNotBlockStartupWhenQdrantCheckFails() {
        VectorStore vectorStore = mock(VectorStore.class);
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenThrow(new IllegalStateException("Qdrant unavailable"));
        VectorStoreHealthConfiguration configuration = new VectorStoreHealthConfiguration();

        assertDoesNotThrow(() -> configuration.vectorStoreHealthRunner(vectorStore).run(null));
    }
}
