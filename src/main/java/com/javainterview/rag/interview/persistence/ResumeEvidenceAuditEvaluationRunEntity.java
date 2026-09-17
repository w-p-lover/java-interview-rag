package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("t_resume_audit_evaluation_run")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResumeEvidenceAuditEvaluationRunEntity {
    private String runId;
    private String contractKey;
    private String datasetVersion;
    private String roleName;
    private String promptVersion;
    private String modelName;
    private String status;
    private Integer totalCaseCount;
    private String caseResultsJson;
    private String errorType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}
