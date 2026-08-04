package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.javainterview.rag.interview.review.InterviewReviewTaskStatus;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface InterviewReviewTaskMapper extends BaseMapper<InterviewReviewTaskEntity> {

    @Insert("INSERT INTO t_interview_review_task "
            + "(id, session_id, question_index, question, objective, gaps_json, "
            + "improvement_suggestion, knowledge_grounded, knowledge_sources_json, status, "
            + "planned_review_date, created_at) "
            + "VALUES (#{entity.id}, #{entity.sessionId}, #{entity.questionIndex}, #{entity.question}, "
            + "#{entity.objective}, #{entity.gapsJson}, #{entity.improvementSuggestion}, "
            + "#{entity.knowledgeGrounded}, #{entity.knowledgeSourcesJson}, #{entity.status}, "
            + "#{entity.plannedReviewDate,jdbcType=DATE}, #{entity.createdAt}) "
            + "ON DUPLICATE KEY UPDATE id = id")
    int insertIfAbsent(@Param("entity") InterviewReviewTaskEntity entity);

    @Select("<script>SELECT id, session_id, question_index, question, objective, gaps_json, "
            + "improvement_suggestion, knowledge_grounded, knowledge_sources_json, status, created_at, "
            + "completed_at, planned_review_date "
            + "FROM t_interview_review_task WHERE session_id = #{sessionId} "
            + "<if test='status != null'>AND status = #{status} </if>"
            + "<if test='plannedDateFrom != null'>AND planned_review_date &gt;= #{plannedDateFrom} </if>"
            + "<if test='plannedDateTo != null'>AND planned_review_date &lt;= #{plannedDateTo} </if>"
            + "ORDER BY question_index</script>")
    List<InterviewReviewTaskEntity> findBySessionId(
            @Param("sessionId") String sessionId,
            @Param("status") InterviewReviewTaskStatus status,
            @Param("plannedDateFrom") LocalDate plannedDateFrom,
            @Param("plannedDateTo") LocalDate plannedDateTo);

    @Update("UPDATE t_interview_review_task SET status = 'COMPLETED', "
            + "completed_at = COALESCE(completed_at, #{completedAt}) "
            + "WHERE session_id = #{sessionId} AND id = #{taskId}")
    int complete(@Param("sessionId") String sessionId,
                 @Param("taskId") String taskId,
                 @Param("completedAt") LocalDateTime completedAt);

    @Select("SELECT id, session_id, question_index, question, objective, gaps_json, "
            + "improvement_suggestion, knowledge_grounded, knowledge_sources_json, status, created_at, "
            + "completed_at, planned_review_date FROM t_interview_review_task "
            + "WHERE session_id = #{sessionId} AND id = #{taskId}")
    InterviewReviewTaskEntity findBySessionAndId(@Param("sessionId") String sessionId,
                                                 @Param("taskId") String taskId);

    @Update("UPDATE t_interview_review_task SET planned_review_date = "
            + "#{plannedReviewDate,jdbcType=DATE} "
            + "WHERE session_id = #{sessionId} AND id = #{taskId}")
    int schedule(@Param("sessionId") String sessionId,
                 @Param("taskId") String taskId,
                 @Param("plannedReviewDate") LocalDate plannedReviewDate);
}
