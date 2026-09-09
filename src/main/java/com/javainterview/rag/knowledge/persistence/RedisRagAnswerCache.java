package com.javainterview.rag.knowledge.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.knowledge.rag.RagAnswerCache;
import com.javainterview.rag.knowledge.rag.RagAnswerResult;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;

/** Redis 仅缓存通过证据校验的完整回答，连接失败时旁路回源。 */
public final class RedisRagAnswerCache implements RagAnswerCache {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Duration ttl;

    public RedisRagAnswerCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, Duration ttl) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.ttl = ttl;
    }

    @Override
    public Optional<RagAnswerResult> get(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value == null ? Optional.empty()
                    : Optional.of(objectMapper.readValue(value, RagAnswerResult.class));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    @Override
    public void put(String key, RagAnswerResult result) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(result), ttl);
        } catch (Exception ignored) {
            // 缓存不可用时继续保留模型结果，不把 Redis 变成问答单点。
        }
    }
}
