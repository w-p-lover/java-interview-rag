package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.javainterview.rag.interview.review.InterviewReviewAttempt;

import java.time.LocalDateTime;

@TableName("t_interview_review_attempt")
public class InterviewReviewAttemptEntity {

    @TableId(type = IdType.INPUT)
    private String id;
    private String sessionId;
    private String reviewTaskId;
    private Integer questionIndex;
    private Integer attemptNumber;
    private String answer;
    private String evaluationJson;
    private Boolean knowledgeGrounded;
    private String knowledgeSourcesJson;
    private String promptVersion;
    private String rubricVersion;
    private String modelName;
    private Integer originalOverallScore;
    private Integer previousOverallScore;
    private Integer scoreDeltaFromOriginal;
    private Integer scoreDeltaFromPrevious;
    private LocalDateTime createdAt;

    public static InterviewReviewAttemptEntity from(InterviewReviewAttempt attempt,
                                                    String evaluationJson,
                                                    String knowledgeSourcesJson) {
        InterviewReviewAttemptEntity entity = new InterviewReviewAttemptEntity();
        entity.id = attempt.id();
        entity.sessionId = attempt.sessionId();
        entity.reviewTaskId = attempt.reviewTaskId();
        entity.questionIndex = attempt.questionIndex();
        entity.attemptNumber = attempt.attemptNumber();
        entity.answer = attempt.answer();
        entity.evaluationJson = evaluationJson;
        entity.knowledgeGrounded = attempt.knowledgeGrounded();
        entity.knowledgeSourcesJson = knowledgeSourcesJson;
        entity.promptVersion = attempt.promptVersion();
        entity.rubricVersion = attempt.rubricVersion();
        entity.modelName = attempt.modelName();
        entity.originalOverallScore = attempt.originalOverallScore();
        entity.previousOverallScore = attempt.previousOverallScore();
        entity.scoreDeltaFromOriginal = attempt.scoreDeltaFromOriginal();
        entity.scoreDeltaFromPrevious = attempt.scoreDeltaFromPrevious();
        entity.createdAt = attempt.createdAt();
        return entity;
    }

    public String getId() { return id; }
    public String getSessionId() { return sessionId; }
    public String getReviewTaskId() { return reviewTaskId; }
    public Integer getQuestionIndex() { return questionIndex; }
    public Integer getAttemptNumber() { return attemptNumber; }
    public String getAnswer() { return answer; }
    public String getEvaluationJson() { return evaluationJson; }
    public Boolean getKnowledgeGrounded() { return knowledgeGrounded; }
    public String getKnowledgeSourcesJson() { return knowledgeSourcesJson; }
    public String getPromptVersion() { return promptVersion; }
    public String getRubricVersion() { return rubricVersion; }
    public String getModelName() { return modelName; }
    public Integer getOriginalOverallScore() { return originalOverallScore; }
    public Integer getPreviousOverallScore() { return previousOverallScore; }
    public Integer getScoreDeltaFromOriginal() { return scoreDeltaFromOriginal; }
    public Integer getScoreDeltaFromPrevious() { return scoreDeltaFromPrevious; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
