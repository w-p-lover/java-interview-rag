package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_knowledge_bank_question")
public class BankQuestionEntity {
    @TableId(type = IdType.INPUT)
    private String id;
    private String generationTaskId;
    private String source;
    private String documentVersion;
    private String contentKey;
    private String status;
    private String questionJson;
    private Long version;
    private Integer useCount;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
