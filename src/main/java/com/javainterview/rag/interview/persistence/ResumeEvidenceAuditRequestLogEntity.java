package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditRequestLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("t_resume_evidence_audit_request_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeEvidenceAuditRequestLogEntity {

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

    public static ResumeEvidenceAuditRequestLogEntity from(ResumeEvidenceAuditRequestLog log) {
        ResumeEvidenceAuditRequestLogEntity entity = new ResumeEvidenceAuditRequestLogEntity();
        entity.id = log.id();
        entity.requestId = log.requestId();
        entity.roleName = log.roleName();
        entity.promptVersion = log.promptVersion();
        entity.modelName = log.modelName();
        entity.status = log.status().name();
        entity.promptTokens = log.promptTokens();
        entity.completionTokens = log.completionTokens();
        entity.totalTokens = log.totalTokens();
        entity.elapsedMillis = log.elapsedMillis();
        entity.errorType = log.errorType();
        entity.createdAt = log.createdAt();
        return entity;
    }

}
