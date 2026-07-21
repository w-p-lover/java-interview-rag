package com.javainterview.rag.knowledge.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("t_knowledge_document_version")
public class KnowledgeDocumentVersionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String source;
    private String version;
    private Integer contentLength;
    private LocalDateTime createdAt;

    public static KnowledgeDocumentVersionEntity create(String source, String version,
                                                        int contentLength) {
        KnowledgeDocumentVersionEntity entity = new KnowledgeDocumentVersionEntity();
        entity.source = source;
        entity.version = version;
        entity.contentLength = contentLength;
        entity.createdAt = LocalDateTime.now();
        return entity;
    }

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getVersion() {
        return version;
    }

    public Integer getContentLength() {
        return contentLength;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
