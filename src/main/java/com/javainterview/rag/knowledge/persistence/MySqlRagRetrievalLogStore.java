package com.javainterview.rag.knowledge.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javainterview.rag.knowledge.rag.RagRetrievalLog;
import com.javainterview.rag.knowledge.rag.RagRetrievalLogStatus;
import com.javainterview.rag.knowledge.rag.RagRetrievalLogStore;
import com.javainterview.rag.knowledge.rag.RagRetrievalSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public class MySqlRagRetrievalLogStore implements RagRetrievalLogStore {

    private static final Logger log = LoggerFactory.getLogger(MySqlRagRetrievalLogStore.class);
    private static final TypeReference<List<RagRetrievalSource>> SOURCE_LIST_TYPE = new TypeReference<>() {
    };

    private final RagRetrievalLogMapper mapper;
    private final ObjectMapper objectMapper;

    public MySqlRagRetrievalLogStore(RagRetrievalLogMapper mapper, ObjectMapper objectMapper) {
        this.mapper = Objects.requireNonNull(mapper, "RAG 召回日志 Mapper 不能为空");
        this.objectMapper = Objects.requireNonNull(objectMapper, "JSON 处理器不能为空");
    }

    @Override
    public void record(RagRetrievalLog retrievalLog) {
        try {
            mapper.insert(RagRetrievalLogEntity.from(retrievalLog,
                    objectMapper.writeValueAsString(retrievalLog.sources())));
        } catch (Exception exception) {
            log.warn("写入 RAG 召回日志失败，不影响本次回答", exception);
        }
    }

    @Override
    public List<RagRetrievalLog> findRecent(int limit) {
        return mapper.findRecent(limit).stream().map(this::toLog).toList();
    }

    private RagRetrievalLog toLog(RagRetrievalLogEntity entity) {
        try {
            return new RagRetrievalLog(entity.getId(), entity.getQuestion(), entity.getTopK(),
                    entity.getSimilarityThreshold(), entity.getPromptVersion(),
                    RagRetrievalLogStatus.valueOf(entity.getStatus()), Boolean.TRUE.equals(entity.getGrounded()),
                    objectMapper.readValue(entity.getRetrievedSourcesJson(), SOURCE_LIST_TYPE),
                    entity.getElapsedMillis(), entity.getErrorType(), entity.getCreatedAt());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("RAG 召回日志来源数据损坏", exception);
        }
    }
}
