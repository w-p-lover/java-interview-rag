package com.javainterview.rag.knowledge.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("t_knowledge_document_version")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocumentVersionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String source;
    private String version;
    private Integer contentLength;
    private String outlineJson;
    private LocalDateTime createdAt;

    public static KnowledgeDocumentVersionEntity create(String source, String version,
                                                        int contentLength, String outlineJson) {
        KnowledgeDocumentVersionEntity entity = new KnowledgeDocumentVersionEntity();
        entity.source = source;
        entity.version = version;
        entity.contentLength = contentLength;
        entity.outlineJson = outlineJson;
        entity.createdAt = LocalDateTime.now();
        return entity;
    }

}
