package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.javainterview.rag.interview.review.InterviewReviewTask;

import java.time.LocalDateTime;

@TableName("t_interview_review_task")
public class InterviewReviewTaskEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    private String sessionId;
    private Integer questionIndex;
    private String question;
    private String objective;
    private String gapsJson;
    private String improvementSuggestion;
    private Boolean knowledgeGrounded;
    private String knowledgeSourcesJson;
    private String status;
    private LocalDateTime createdAt;

    public static InterviewReviewTaskEntity from(InterviewReviewTask task,
                                                 String gapsJson,
                                                 String knowledgeSourcesJson) {
        InterviewReviewTaskEntity entity = new InterviewReviewTaskEntity();
        entity.id = task.id();
        entity.sessionId = task.sessionId();
        entity.questionIndex = task.questionIndex();
        entity.question = task.question();
        entity.objective = task.objective();
        entity.gapsJson = gapsJson;
        entity.improvementSuggestion = task.improvementSuggestion();
        entity.knowledgeGrounded = task.knowledgeGrounded();
        entity.knowledgeSourcesJson = knowledgeSourcesJson;
        entity.status = task.status().name();
        entity.createdAt = task.createdAt();
        return entity;
    }

    public String getId() {
        return id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Integer getQuestionIndex() {
        return questionIndex;
    }

    public String getQuestion() {
        return question;
    }

    public String getObjective() {
        return objective;
    }

    public String getGapsJson() {
        return gapsJson;
    }

    public String getImprovementSuggestion() {
        return improvementSuggestion;
    }

    public Boolean getKnowledgeGrounded() {
        return knowledgeGrounded;
    }

    public String getKnowledgeSourcesJson() {
        return knowledgeSourcesJson;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
