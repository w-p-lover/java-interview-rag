package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InterviewAnswerEvaluationLogMapper extends BaseMapper<InterviewAnswerEvaluationLogEntity> {

    @Select("<script>SELECT id, evaluation_mode, baseline_run_id, baseline_case_id, "
            + "prompt_version, rubric_version, model_name, status, "
            + "knowledge_grounded, technical_score, completeness_score, communication_score, "
            + "overall_score, elapsed_millis, error_type, created_at "
            + "FROM t_interview_answer_evaluation_log WHERE evaluation_mode = #{evaluationMode} "
            + "<if test='baselineRunId != null'>AND baseline_run_id = #{baselineRunId} </if>"
            + "<if test='baselineCaseId != null'>AND baseline_case_id = #{baselineCaseId} </if>"
            + "ORDER BY created_at DESC, id DESC LIMIT #{limit}</script>")
    List<InterviewAnswerEvaluationLogEntity> findRecent(
            @Param("evaluationMode") String evaluationMode,
            @Param("baselineRunId") String baselineRunId,
            @Param("baselineCaseId") String baselineCaseId,
            @Param("limit") int limit);
}
