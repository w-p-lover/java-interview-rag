package com.javainterview.rag.knowledge.persistence;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.javainterview.rag.knowledge.feedback.RagAnswerFeedback;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor
@TableName("t_rag_answer_feedback")
public class RagAnswerFeedbackEntity {
    @TableId(type = IdType.AUTO) private Long id;
    private String questionHash;
    private Boolean grounded;
    private String retrievalStrategy;
    private String feedback;
    private LocalDateTime createdAt;

    public static RagAnswerFeedbackEntity from(RagAnswerFeedback value) {
        return new RagAnswerFeedbackEntity(null, value.questionHash(), value.grounded(), value.retrievalStrategy(), value.feedback(), value.createdAt());
    }
}
