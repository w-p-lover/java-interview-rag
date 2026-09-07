package com.javainterview.rag.knowledge.consistency;

import com.javainterview.rag.knowledge.document.MarkdownIndexingStatus;

public record KnowledgeIndexConsistencyReport(
        String source,
        String documentVersion,
        Integer expectedChunkCount,
        boolean consistent,
        IndexingStateCheck indexingState,
        StoreCheck dense,
        StoreCheck sparse
) {

    public enum StoreStatus {
        CONSISTENT,
        MISSING_CHUNKS,
        EXCESS_CHUNKS,
        STALE_VERSIONS,
        UNVERIFIABLE,
        UNAVAILABLE
    }

    public record IndexingStateCheck(
            boolean tracked,
            MarkdownIndexingStatus status,
            Integer chunkCount,
            Boolean expectedChunkCountMatched,
            String errorType
    ) {
    }

    public record StoreCheck(
            StoreStatus status,
            Long currentVersionChunkCount,
            Long staleVersionChunkCount,
            Long sourceChunkCount,
            Boolean expectedChunkCountMatched,
            String errorType
    ) {
    }
}
