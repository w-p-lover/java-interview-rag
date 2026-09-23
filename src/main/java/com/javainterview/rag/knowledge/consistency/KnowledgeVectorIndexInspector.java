package com.javainterview.rag.knowledge.consistency;

@FunctionalInterface
public interface KnowledgeVectorIndexInspector {

    KnowledgeIndexCounts inspect(String source, String documentVersion);

    default long countGeneration(String source, String documentVersion, String generation) {
        throw new UnsupportedOperationException("当前向量巡检器不支持按代际计数");
    }
}
