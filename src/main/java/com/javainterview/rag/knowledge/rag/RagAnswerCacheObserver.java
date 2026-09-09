package com.javainterview.rag.knowledge.rag;

public interface RagAnswerCacheObserver {

    void hit();

    void miss();

    void bypass();
}
