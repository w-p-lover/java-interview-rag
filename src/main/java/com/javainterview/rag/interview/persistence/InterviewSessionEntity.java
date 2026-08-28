package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.javainterview.rag.interview.session.InterviewSession;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@TableName("t_interview_session")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InterviewSessionEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    private String targetRole;
    private String profileJson;
    private String questionsJson;
    private Integer maxQuestionCount;
    private String answersJson;
    private String reportJson;
    private Integer currentQuestionIndex;
    private String status;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static InterviewSessionEntity from(InterviewSession session, String profileJson,
                                              String questionsJson, String answersJson,
                                              String reportJson) {
        InterviewSessionEntity entity = new InterviewSessionEntity();
        entity.id = session.id();
        entity.targetRole = session.targetRole();
        entity.profileJson = profileJson;
        entity.questionsJson = questionsJson;
        entity.maxQuestionCount = session.maxQuestionCount();
        entity.answersJson = answersJson;
        entity.reportJson = reportJson;
        entity.currentQuestionIndex = session.currentQuestionIndex();
        entity.status = session.status().name();
        entity.version = session.version();
        entity.createdAt = session.createdAt();
        entity.updatedAt = session.updatedAt();
        return entity;
    }

}
