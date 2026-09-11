package com.javainterview.rag.knowledge.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@TableName("t_knowledge_indexing_state")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeIndexingStateEntity {

    private String source;
    private String documentVersion;
    private Integer chunkCount;
    private String status;
    private String errorType;
}
