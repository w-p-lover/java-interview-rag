package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
            + "improvement_suggestion, knowledge_grounded, knowledge_sources_json, status, created_at "
            + "FROM t_interview_review_task WHERE session_id = #{sessionId} ORDER BY question_index")
    List<InterviewReviewTaskEntity> findBySessionId(@Param("sessionId") String sessionId);
}
