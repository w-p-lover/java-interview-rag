package com.javainterview.rag.knowledge.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.javainterview.rag.knowledge.rag.RagRetrievalLog;
import com.javainterview.rag.knowledge.rag.RagRetrievalLogStatus;

import java.time.LocalDateTime;

@TableName("t_rag_retrieval_log")
public class RagRetrievalLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String question;
    private Integer topK;
    private Double similarityThreshold;
    private String promptVersion;
    private String status;
    private Boolean grounded;
    private String retrievedSourcesJson;
    private Long elapsedMillis;
    private String errorType;
    private LocalDateTime createdAt;

    public static RagRetrievalLogEntity from(RagRetrievalLog log, String retrievedSourcesJson) {
        RagRetrievalLogEntity entity = new RagRetrievalLogEntity();
        entity.id = log.id();
        entity.question = log.question();
        entity.topK = log.topK();
        entity.similarityThreshold = log.similarityThreshold();
        entity.promptVersion = log.promptVersion();
        entity.status = log.status().name();
        entity.grounded = log.grounded();
        entity.retrievedSourcesJson = retrievedSourcesJson;
        entity.elapsedMillis = log.elapsedMillis();
        entity.errorType = log.errorType();
        entity.createdAt = log.createdAt();
        return entity;
    }

    public Long getId() {
        return id;
    }

    public String getQuestion() {
        return question;
    }

    public Integer getTopK() {
        return topK;
    }

    public Double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public String getStatus() {
        return status;
    }

    public Boolean getGrounded() {
        return grounded;
    }

    public String getRetrievedSourcesJson() {
        return retrievedSourcesJson;
    }

    public Long getElapsedMillis() {
        return elapsedMillis;
    }

    public String getErrorType() {
        return errorType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
