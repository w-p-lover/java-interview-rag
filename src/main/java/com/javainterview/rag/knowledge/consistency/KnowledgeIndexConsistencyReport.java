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

    public enum IndexVersionStatus {
        MATCHED,
        PARTIAL_MISMATCH,
        ALL_MISMATCH,
        UNVERIFIABLE
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
            Long currentVersionMetadataMatchCount,
            Long staleVersionChunkCount,
            Long sourceChunkCount,
            Boolean expectedChunkCountMatched,
            Boolean indexVersionMatched,
            IndexVersionStatus indexVersionStatus,
            String errorType
    ) {
        public StoreCheck(StoreStatus status, Long currentVersionChunkCount,
                          Long currentVersionMetadataMatchCount, Long staleVersionChunkCount,
                          Long sourceChunkCount, Boolean expectedChunkCountMatched,
                          Boolean indexVersionMatched, String errorType) {
            this(status, currentVersionChunkCount, currentVersionMetadataMatchCount,
                    staleVersionChunkCount, sourceChunkCount, expectedChunkCountMatched,
                    indexVersionMatched,
                    Boolean.TRUE.equals(indexVersionMatched) ? IndexVersionStatus.MATCHED
                            : IndexVersionStatus.UNVERIFIABLE,
                    errorType);
        }

        public StoreCheck(StoreStatus status, Long currentVersionChunkCount,
                          Long staleVersionChunkCount, Long sourceChunkCount,
                          Boolean expectedChunkCountMatched, String errorType) {
            this(status, currentVersionChunkCount, currentVersionChunkCount,
                    staleVersionChunkCount, sourceChunkCount,
                    expectedChunkCountMatched, true, IndexVersionStatus.MATCHED, errorType);
        }
    }
}
