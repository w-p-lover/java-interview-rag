package com.javainterview.rag.knowledge.vector;

public final class KnowledgeRetrievalTimeoutException extends RuntimeException {

    public KnowledgeRetrievalTimeoutException(String operation, long timeoutMillis) {
        super(operation + "超过截止时间 " + timeoutMillis + "ms");
    }
}
