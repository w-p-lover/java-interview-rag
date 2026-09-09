package com.javainterview.rag.knowledge.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.javainterview.rag.knowledge.review.KnowledgeReviewTask;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @TableName("t_knowledge_review_task")
public class KnowledgeReviewTaskEntity {
    @TableId(type = IdType.INPUT) private String id;
    private String question;
    private String claim;
    private String evidenceStatus;
    private String sourceNumbersJson;
    private String status;
    private LocalDate plannedReviewDate;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    public static KnowledgeReviewTaskEntity from(KnowledgeReviewTask task, String sourcesJson) {
        KnowledgeReviewTaskEntity e = new KnowledgeReviewTaskEntity();
        e.id = task.id(); e.question = task.question(); e.claim = task.claim();
        e.evidenceStatus = task.evidenceStatus().name(); e.sourceNumbersJson = sourcesJson;
        e.status = task.status().name(); e.plannedReviewDate = task.plannedReviewDate();
        e.createdAt = task.createdAt(); e.completedAt = task.completedAt();
        return e;
    }
}
