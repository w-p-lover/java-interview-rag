package com.javainterview.rag.knowledge.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.javainterview.rag.knowledge.evaluation.RagEvaluationCandidate;
import com.javainterview.rag.knowledge.evaluation.RagEvaluationCandidateStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor
@TableName("t_rag_evaluation_candidate")
public class RagEvaluationCandidateEntity {
    @TableId(type = IdType.INPUT) private String id;
    private Long feedbackId;
    private String question;
    private String expectedSourcesJson;
    private String expectedAnswerPointsJson;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;
    public static RagEvaluationCandidateEntity from(RagEvaluationCandidate value, String sources, String points) {
        return new RagEvaluationCandidateEntity(value.id(), value.feedbackId(), value.question(), sources, points,
                value.status().name(), value.createdAt(), value.reviewedAt());
    }
}
