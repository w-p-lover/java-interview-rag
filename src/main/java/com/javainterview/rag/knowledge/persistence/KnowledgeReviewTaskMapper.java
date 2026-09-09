package com.javainterview.rag.knowledge.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.javainterview.rag.knowledge.review.KnowledgeReviewTaskStatus;
import org.apache.ibatis.annotations.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface KnowledgeReviewTaskMapper extends BaseMapper<KnowledgeReviewTaskEntity> {
    @Insert("INSERT INTO t_knowledge_review_task (id, question, claim, evidence_status, source_numbers_json, status, planned_review_date, created_at, completed_at) "
            + "VALUES (#{entity.id},#{entity.question},#{entity.claim},#{entity.evidenceStatus},#{entity.sourceNumbersJson},#{entity.status},#{entity.plannedReviewDate,jdbcType=DATE},#{entity.createdAt},#{entity.completedAt}) ON DUPLICATE KEY UPDATE id=id")
    int insertIfAbsent(@Param("entity") KnowledgeReviewTaskEntity entity);

    @Select("<script>SELECT * FROM t_knowledge_review_task <where><if test='status != null'>status = #{status}</if><if test='plannedDateFrom != null'>AND planned_review_date &gt;= #{plannedDateFrom}</if><if test='plannedDateTo != null'>AND planned_review_date &lt;= #{plannedDateTo}</if></where> ORDER BY planned_review_date IS NULL, planned_review_date, created_at</script>")
    List<KnowledgeReviewTaskEntity> find(@Param("status") KnowledgeReviewTaskStatus status,
                                         @Param("plannedDateFrom") LocalDate plannedDateFrom,
                                         @Param("plannedDateTo") LocalDate plannedDateTo);

    @Update("UPDATE t_knowledge_review_task SET status='COMPLETED', completed_at=COALESCE(completed_at, #{completedAt}) WHERE id=#{taskId}")
    int complete(@Param("taskId") String taskId, @Param("completedAt") LocalDateTime completedAt);

    @Update("UPDATE t_knowledge_review_task SET planned_review_date=#{plannedReviewDate,jdbcType=DATE} WHERE id=#{taskId}")
    int schedule(@Param("taskId") String taskId, @Param("plannedReviewDate") LocalDate plannedReviewDate);

    @Select("SELECT * FROM t_knowledge_review_task WHERE id=#{taskId}")
    KnowledgeReviewTaskEntity findById(@Param("taskId") String taskId);
}
