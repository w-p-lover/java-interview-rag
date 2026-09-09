package com.javainterview.rag.knowledge.rag;

public final class NoopRagAnswerCacheObserver implements RagAnswerCacheObserver {

    @Override
    public void hit() {
    }

    @Override
    public void miss() {
    }

    @Override
    public void bypass() {
    }
}
