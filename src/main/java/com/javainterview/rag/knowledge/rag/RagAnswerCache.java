package com.javainterview.rag.knowledge.rag;

import java.util.Optional;

/** 可选的可靠回答缓存；缓存故障不应阻断问答主链路。 */
public interface RagAnswerCache {

    Optional<RagAnswerResult> get(String key);

    void put(String key, RagAnswerResult result);
}
