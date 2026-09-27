package com.javainterview.rag.knowledge.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("t_knowledge_document_task")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocumentTaskEntity {
    private String taskId;
    private String attemptId;
    private String source;
    private String fileId;
    private Integer recoveryCount;
    private String operation;
    private String status;
    private String version;
    private Boolean imported;
    private Integer chunkCount;
    private String format;
    private Integer warningCount;
    private String errorType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}
