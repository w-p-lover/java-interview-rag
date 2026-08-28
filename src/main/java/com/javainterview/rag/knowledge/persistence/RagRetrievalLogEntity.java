package com.javainterview.rag.knowledge.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.javainterview.rag.knowledge.rag.RagRetrievalLog;
import com.javainterview.rag.knowledge.rag.RagRetrievalLogStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("t_rag_retrieval_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
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

}
