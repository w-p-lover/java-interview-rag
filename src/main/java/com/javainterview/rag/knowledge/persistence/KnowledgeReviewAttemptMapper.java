package com.javainterview.rag.knowledge.persistence;
import com.baomidou.mybatisplus.core.mapper.BaseMapper; import org.apache.ibatis.annotations.*; import java.util.List;
@Mapper public interface KnowledgeReviewAttemptMapper extends BaseMapper<KnowledgeReviewAttemptEntity> {
 @Insert("INSERT IGNORE INTO t_knowledge_review_attempt (id,task_id,idempotency_key,answer,evaluation_json,created_at) VALUES (#{entity.id},#{entity.taskId},#{entity.idempotencyKey},#{entity.answer},#{entity.evaluationJson},#{entity.createdAt})") int insertAttempt(@Param("entity") KnowledgeReviewAttemptEntity entity);
 @Select("SELECT * FROM t_knowledge_review_attempt WHERE idempotency_key=#{idempotencyKey} LIMIT 1") KnowledgeReviewAttemptEntity findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);
 @Select("SELECT * FROM t_knowledge_review_attempt WHERE task_id=#{taskId} ORDER BY created_at") List<KnowledgeReviewAttemptEntity> findByTaskId(@Param("taskId") String taskId);
}
