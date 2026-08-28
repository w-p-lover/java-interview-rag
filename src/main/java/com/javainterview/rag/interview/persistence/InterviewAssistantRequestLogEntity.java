package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.javainterview.rag.interview.tool.InterviewAssistantRequestLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("t_interview_assistant_request_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewAssistantRequestLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String requestId;
    private String modelName;
    private String status;
    private Integer toolExecutionCount;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Long elapsedMillis;
    private Boolean finalResponsePresent;
    private String errorType;
    private LocalDateTime createdAt;

    public static InterviewAssistantRequestLogEntity from(InterviewAssistantRequestLog log) {
        InterviewAssistantRequestLogEntity entity = new InterviewAssistantRequestLogEntity();
        entity.id = log.id();
        entity.requestId = log.requestId();
        entity.modelName = log.modelName();
        entity.status = log.status().name();
        entity.toolExecutionCount = log.toolExecutionCount();
        entity.promptTokens = log.promptTokens();
        entity.completionTokens = log.completionTokens();
        entity.totalTokens = log.totalTokens();
        entity.elapsedMillis = log.elapsedMillis();
        entity.finalResponsePresent = log.finalResponsePresent();
        entity.errorType = log.errorType();
        entity.createdAt = log.createdAt();
        return entity;
    }

}
