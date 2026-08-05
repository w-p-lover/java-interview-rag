package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.javainterview.rag.interview.tool.InterviewToolExecutionLog;

import java.time.LocalDateTime;

@TableName("t_interview_tool_execution_log")
public class InterviewToolExecutionLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String executionId;
    private String toolName;
    private String sessionId;
    private String status;
    private Integer resultCount;
    private Long elapsedMillis;
    private String errorType;
    private LocalDateTime createdAt;

    public static InterviewToolExecutionLogEntity from(InterviewToolExecutionLog log) {
        InterviewToolExecutionLogEntity entity = new InterviewToolExecutionLogEntity();
        entity.id = log.id();
        entity.executionId = log.executionId();
        entity.toolName = log.toolName();
        entity.sessionId = log.sessionId();
        entity.status = log.status().name();
        entity.resultCount = log.resultCount();
        entity.elapsedMillis = log.elapsedMillis();
        entity.errorType = log.errorType();
        entity.createdAt = log.createdAt();
        return entity;
    }

    public Long getId() {
        return id;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getToolName() {
        return toolName;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getStatus() {
        return status;
    }

    public Integer getResultCount() {
        return resultCount;
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
