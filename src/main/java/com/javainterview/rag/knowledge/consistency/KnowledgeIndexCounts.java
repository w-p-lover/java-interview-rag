package com.javainterview.rag.knowledge.consistency;

public record KnowledgeIndexCounts(long currentVersionChunkCount, long sourceChunkCount,
                                   long currentVersionMetadataMatchCount) {

    public KnowledgeIndexCounts(long currentVersionChunkCount, long sourceChunkCount) {
        this(currentVersionChunkCount, sourceChunkCount, currentVersionChunkCount);
    }

    public KnowledgeIndexCounts {
        if (currentVersionChunkCount < 0 || sourceChunkCount < currentVersionChunkCount
                || currentVersionMetadataMatchCount < 0
                || currentVersionMetadataMatchCount > currentVersionChunkCount) {
            throw new IllegalArgumentException("索引 Chunk 计数无效");
        }
    }

    public long staleVersionChunkCount() {
        return sourceChunkCount - currentVersionChunkCount;
    }
}
