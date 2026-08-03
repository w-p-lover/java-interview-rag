package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InterviewAnswerEvaluationLogMapper extends BaseMapper<InterviewAnswerEvaluationLogEntity> {

    @Select("SELECT id, evaluation_mode, baseline_run_id, baseline_case_id, "
            + "prompt_version, rubric_version, model_name, status, "
            + "knowledge_grounded, technical_score, completeness_score, communication_score, "
            + "overall_score, elapsed_millis, error_type, created_at "
            + "FROM t_interview_answer_evaluation_log WHERE evaluation_mode = #{evaluationMode} "
            + "ORDER BY created_at DESC, id DESC LIMIT #{limit}")
    List<InterviewAnswerEvaluationLogEntity> findRecent(
            @Param("evaluationMode") String evaluationMode,
            @Param("limit") int limit);
}
