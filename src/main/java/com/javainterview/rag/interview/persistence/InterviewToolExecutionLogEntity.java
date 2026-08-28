package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.javainterview.rag.interview.tool.InterviewToolExecutionLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("t_interview_tool_execution_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewToolExecutionLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String executionId;
    private String assistantRequestId;
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
        entity.assistantRequestId = log.assistantRequestId();
        entity.toolName = log.toolName();
        entity.sessionId = log.sessionId();
        entity.status = log.status().name();
        entity.resultCount = log.resultCount();
        entity.elapsedMillis = log.elapsedMillis();
        entity.errorType = log.errorType();
        entity.createdAt = log.createdAt();
        return entity;
    }

}
