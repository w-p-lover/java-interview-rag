package com.javainterview.rag.knowledge.persistence;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data; import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @TableName("t_knowledge_review_attempt")
public class KnowledgeReviewAttemptEntity {
 @TableId(type= IdType.INPUT) private String id; private String taskId; private String idempotencyKey; private String answer; private String evaluationJson; private LocalDateTime createdAt;
}
