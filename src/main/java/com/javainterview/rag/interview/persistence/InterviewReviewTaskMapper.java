package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface InterviewReviewTaskMapper extends BaseMapper<InterviewReviewTaskEntity> {

    @Insert("INSERT INTO t_interview_review_task "
            + "(id, session_id, question_index, question, objective, gaps_json, "
            + "improvement_suggestion, knowledge_grounded, knowledge_sources_json, status, created_at) "
            + "VALUES (#{entity.id}, #{entity.sessionId}, #{entity.questionIndex}, #{entity.question}, "
            + "#{entity.objective}, #{entity.gapsJson}, #{entity.improvementSuggestion}, "
            + "#{entity.knowledgeGrounded}, #{entity.knowledgeSourcesJson}, #{entity.status}, "
            + "#{entity.createdAt}) ON DUPLICATE KEY UPDATE id = id")
    int insertIfAbsent(@Param("entity") InterviewReviewTaskEntity entity);

    @Select("SELECT id, session_id, question_index, question, objective, gaps_json, "
            + "improvement_suggestion, knowledge_grounded, knowledge_sources_json, status, created_at, "
            + "completed_at "
            + "FROM t_interview_review_task WHERE session_id = #{sessionId} ORDER BY question_index")
    List<InterviewReviewTaskEntity> findBySessionId(@Param("sessionId") String sessionId);

    @Update("UPDATE t_interview_review_task SET status = 'COMPLETED', "
            + "completed_at = COALESCE(completed_at, #{completedAt}) "
            + "WHERE session_id = #{sessionId} AND id = #{taskId}")
    int complete(@Param("sessionId") String sessionId,
                 @Param("taskId") String taskId,
                 @Param("completedAt") LocalDateTime completedAt);

    @Select("SELECT id, session_id, question_index, question, objective, gaps_json, "
            + "improvement_suggestion, knowledge_grounded, knowledge_sources_json, status, created_at, "
            + "completed_at FROM t_interview_review_task "
            + "WHERE session_id = #{sessionId} AND id = #{taskId}")
    InterviewReviewTaskEntity findBySessionAndId(@Param("sessionId") String sessionId,
                                                 @Param("taskId") String taskId);
}
