package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.javainterview.rag.interview.matching.ResumeJobMatchRequestLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("t_resume_job_match_request_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeJobMatchRequestLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String requestId;
    private String roleName;
    private String promptVersion;
    private String modelName;
    private String status;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private Long elapsedMillis;
    private String errorType;
    private LocalDateTime createdAt;

    public static ResumeJobMatchRequestLogEntity from(ResumeJobMatchRequestLog log) {
        return new ResumeJobMatchRequestLogEntity(
                log.id(), log.requestId(), log.roleName(), log.promptVersion(), log.modelName(),
                log.status().name(), log.promptTokens(), log.completionTokens(), log.totalTokens(),
                log.elapsedMillis(), log.errorType(), log.createdAt());
    }
}
