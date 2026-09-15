package com.javainterview.rag.knowledge.consistency;

public record KnowledgeIndexRepairResult(
        String source,
        String documentVersion,
        int chunkCount,
        KnowledgeIndexConsistencyReport consistency
) {
}
