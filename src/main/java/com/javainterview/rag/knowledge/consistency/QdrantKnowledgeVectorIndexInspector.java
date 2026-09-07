package com.javainterview.rag.knowledge.consistency;

import io.qdrant.client.ConditionFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** 使用 Qdrant 原生过滤计数巡检向量索引，不触发查询 Embedding。 */
public final class QdrantKnowledgeVectorIndexInspector implements KnowledgeVectorIndexInspector {

    private static final long TIMEOUT_SECONDS = 5;

    private final QdrantClient client;
    private final String collectionName;

    public QdrantKnowledgeVectorIndexInspector(QdrantClient client, String collectionName) {
        this.client = Objects.requireNonNull(client, "Qdrant 客户端不能为空");
        if (collectionName == null || collectionName.isBlank()) {
            throw new IllegalArgumentException("Qdrant 集合名称不能为空");
        }
        this.collectionName = collectionName.strip();
    }

    @Override
    public KnowledgeIndexCounts inspect(String source, String documentVersion) {
        Points.Filter sourceFilter = Points.Filter.newBuilder()
                .addMust(ConditionFactory.matchKeyword("source", source))
                .build();
        Points.Filter versionFilter = Points.Filter.newBuilder()
                .addMust(ConditionFactory.matchKeyword("source", source))
                .addMust(ConditionFactory.matchKeyword("document_version", documentVersion))
                .build();
        return new KnowledgeIndexCounts(count(versionFilter), count(sourceFilter));
    }

    private long count(Points.Filter filter) {
        try {
            return client.countAsync(collectionName, filter, true)
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Qdrant 索引巡检被中断", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("Qdrant 索引巡检失败", exception);
        }
    }
}
