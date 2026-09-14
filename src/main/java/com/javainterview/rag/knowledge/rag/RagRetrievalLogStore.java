package com.javainterview.rag.knowledge.rag;

import java.util.List;
import java.util.HexFormat;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public interface RagRetrievalLogStore {

    void record(RagRetrievalLog log);

    List<RagRetrievalLog> findRecent(int limit);

    default List<RagRetrievalLog> findRecentByQuestionHash(String questionHash, int limit) {
        return findRecent(limit).stream().filter(log -> hash(log.question()).equals(questionHash)).toList();
    }

    private static String hash(String question) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(question.strip().getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException("问题标识生成失败", exception); }
    }
}
