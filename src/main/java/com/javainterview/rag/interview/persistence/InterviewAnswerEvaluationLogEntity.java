package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluationLog;

import java.time.LocalDateTime;

@TableName("t_interview_answer_evaluation_log")
public class InterviewAnswerEvaluationLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String evaluationMode;
    private String baselineRunId;
    private String baselineCaseId;
    private String promptVersion;
    private String rubricVersion;
    private String modelName;
    private String status;
    private Boolean knowledgeGrounded;
    private Integer technicalScore;
    private Integer completenessScore;
    private Integer communicationScore;
    private Integer overallScore;
    private Long elapsedMillis;
    private String errorType;
    private LocalDateTime createdAt;

    public static InterviewAnswerEvaluationLogEntity from(InterviewAnswerEvaluationLog log) {
        InterviewAnswerEvaluationLogEntity entity = new InterviewAnswerEvaluationLogEntity();
        entity.id = log.id();
        entity.evaluationMode = log.mode().name();
        entity.baselineRunId = log.baselineRunId();
        entity.baselineCaseId = log.baselineCaseId();
        entity.promptVersion = log.promptVersion();
        entity.rubricVersion = log.rubricVersion();
        entity.modelName = log.modelName();
        entity.status = log.status().name();
        entity.knowledgeGrounded = log.knowledgeGrounded();
        entity.technicalScore = log.technicalScore();
        entity.completenessScore = log.completenessScore();
        entity.communicationScore = log.communicationScore();
        entity.overallScore = log.overallScore();
        entity.elapsedMillis = log.elapsedMillis();
        entity.errorType = log.errorType();
        entity.createdAt = log.createdAt();
        return entity;
    }

    public Long getId() {
        return id;
    }

    public String getEvaluationMode() {
        return evaluationMode;
    }

    public String getBaselineRunId() {
        return baselineRunId;
    }

    public String getBaselineCaseId() {
        return baselineCaseId;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public String getRubricVersion() {
        return rubricVersion;
    }

    public String getModelName() {
        return modelName;
    }

    public String getStatus() {
        return status;
    }

    public Boolean getKnowledgeGrounded() {
        return knowledgeGrounded;
    }

    public Integer getTechnicalScore() {
        return technicalScore;
    }

    public Integer getCompletenessScore() {
        return completenessScore;
    }

    public Integer getCommunicationScore() {
        return communicationScore;
    }

    public Integer getOverallScore() {
        return overallScore;
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
