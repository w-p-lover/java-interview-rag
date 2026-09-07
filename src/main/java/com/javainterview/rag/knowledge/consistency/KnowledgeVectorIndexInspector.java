package com.javainterview.rag.knowledge.consistency;

@FunctionalInterface
public interface KnowledgeVectorIndexInspector {

    KnowledgeIndexCounts inspect(String source, String documentVersion);
}
