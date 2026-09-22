package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_knowledge_bank_generation_task")
public class BankGenerationTaskEntity {
    @TableId(type = IdType.INPUT)
    private String id;
    private String requestId;
    private String activeKey;
    private String source;
    private String status;
    private String taskJson;
    private String attemptId;
    private String workerId;
    private LocalDateTime startedAt;
    private LocalDateTime lastHeartbeatAt;
    private Integer recoveryCount;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
