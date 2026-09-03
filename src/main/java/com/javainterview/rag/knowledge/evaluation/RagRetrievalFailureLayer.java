package com.javainterview.rag.knowledge.evaluation;

public enum RagRetrievalFailureLayer {
    NONE,
    CORPUS_ABSENT,
    PARSING_OR_CHUNK_LOSS,
    INDEX_NOT_READY,
    CANDIDATE_NOT_RECALLED,
    RANKING_TOO_LOW,
    REJECTION_FILTERED,
    GENERATION_NOT_USING_EVIDENCE,
    UNCLASSIFIED
}
