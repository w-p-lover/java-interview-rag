package com.javainterview.rag.knowledge.consistency;

import com.google.common.util.concurrent.Futures;
import io.qdrant.client.ConditionFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QdrantKnowledgeVectorIndexInspectorTest {

    @Test
    @SuppressWarnings("unchecked")
    void countsCurrentVersionAndWholeSourceWithExactFilters() {
        QdrantClient client = mock(QdrantClient.class);
        when(client.countAsync(eq("chunks"), org.mockito.ArgumentMatchers.any(), eq(true)))
                .thenReturn(Futures.immediateFuture(2L), Futures.immediateFuture(3L),
                        Futures.immediateFuture(2L));
        QdrantKnowledgeVectorIndexInspector inspector =
                new QdrantKnowledgeVectorIndexInspector(client, "chunks");

        KnowledgeIndexCounts counts = inspector.inspect("java/basic.md", "version-1");

        assertEquals(2L, counts.currentVersionChunkCount());
        assertEquals(1L, counts.staleVersionChunkCount());
        ArgumentCaptor<Points.Filter> filters = ArgumentCaptor.forClass(Points.Filter.class);
        verify(client, times(3)).countAsync(eq("chunks"), filters.capture(), eq(true));
        assertEquals(Points.Filter.newBuilder()
                .addMust(ConditionFactory.matchKeyword("source", "java/basic.md"))
                .addMust(ConditionFactory.matchKeyword("document_version", "version-1"))
                .build(), filters.getAllValues().get(0));
        assertEquals(Points.Filter.newBuilder()
                .addMust(ConditionFactory.matchKeyword("source", "java/basic.md"))
                .build(), filters.getAllValues().get(1));
    }
}
