package com.javainterview.rag.knowledge.consistency;

public record KnowledgeIndexCounts(long currentVersionChunkCount, long sourceChunkCount) {

    public KnowledgeIndexCounts {
        if (currentVersionChunkCount < 0 || sourceChunkCount < currentVersionChunkCount) {
            throw new IllegalArgumentException("索引 Chunk 计数无效");
        }
    }

    public long staleVersionChunkCount() {
        return sourceChunkCount - currentVersionChunkCount;
    }
}
