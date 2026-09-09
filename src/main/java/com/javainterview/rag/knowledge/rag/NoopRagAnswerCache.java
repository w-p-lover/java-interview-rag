package com.javainterview.rag.knowledge.rag;

import java.util.Optional;

public final class NoopRagAnswerCache implements RagAnswerCache {

    @Override
    public Optional<RagAnswerResult> get(String key) {
        return Optional.empty();
    }

    @Override
    public void put(String key, RagAnswerResult result) {
        // local profile 不连接 Redis，保持原有零外部依赖行为。
    }
}
