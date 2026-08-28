package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.javainterview.rag.interview.review.InterviewReviewTask;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("t_interview_review_task")
@Data
@NoArgsConstructor
@AllArgsConstructor
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
    private LocalDate plannedReviewDate;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

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
        entity.plannedReviewDate = task.plannedReviewDate();
        entity.createdAt = task.createdAt();
        entity.completedAt = task.completedAt();
        return entity;
    }

}
