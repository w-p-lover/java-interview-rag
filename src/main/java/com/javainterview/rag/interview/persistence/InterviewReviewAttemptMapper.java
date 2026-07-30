package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InterviewReviewAttemptMapper extends BaseMapper<InterviewReviewAttemptEntity> {

    @Insert("INSERT INTO t_interview_review_attempt "
            + "(id, session_id, review_task_id, question_index, attempt_number, answer, "
            + "evaluation_json, knowledge_grounded, knowledge_sources_json, prompt_version, "
            + "rubric_version, model_name, original_overall_score, previous_overall_score, "
            + "score_delta_from_original, score_delta_from_previous, created_at) "
            + "VALUES (#{entity.id}, #{entity.sessionId}, #{entity.reviewTaskId}, "
            + "#{entity.questionIndex}, #{entity.attemptNumber}, #{entity.answer}, "
            + "#{entity.evaluationJson}, #{entity.knowledgeGrounded}, #{entity.knowledgeSourcesJson}, "
            + "#{entity.promptVersion}, #{entity.rubricVersion}, #{entity.modelName}, "
            + "#{entity.originalOverallScore}, #{entity.previousOverallScore}, "
            + "#{entity.scoreDeltaFromOriginal}, #{entity.scoreDeltaFromPrevious}, #{entity.createdAt})")
    int insertAttempt(@Param("entity") InterviewReviewAttemptEntity entity);

    @Select("SELECT id, session_id, review_task_id, question_index, attempt_number, answer, "
            + "evaluation_json, knowledge_grounded, knowledge_sources_json, prompt_version, "
            + "rubric_version, model_name, original_overall_score, previous_overall_score, "
            + "score_delta_from_original, score_delta_from_previous, created_at "
            + "FROM t_interview_review_attempt WHERE session_id = #{sessionId} "
            + "AND review_task_id = #{reviewTaskId} ORDER BY attempt_number")
    List<InterviewReviewAttemptEntity> findByReviewTaskId(@Param("sessionId") String sessionId,
                                                          @Param("reviewTaskId") String reviewTaskId);
}
