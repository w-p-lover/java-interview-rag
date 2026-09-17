package com.javainterview.rag.interview.persistence;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ResumeEvidenceAuditEvaluationRunMapper {

    @Insert("INSERT IGNORE INTO t_resume_audit_evaluation_run "
            + "(run_id, contract_key, dataset_version, role_name, prompt_version, model_name, status, "
            + "total_case_count, case_results_json, created_at, updated_at) VALUES "
            + "(#{runId}, #{contractKey}, #{datasetVersion}, #{roleName}, #{promptVersion}, #{modelName}, "
            + "'QUEUED', #{totalCaseCount}, '[]', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))")
    int insert(@Param("runId") String runId,
               @Param("contractKey") String contractKey,
               @Param("datasetVersion") String datasetVersion,
               @Param("roleName") String roleName,
               @Param("promptVersion") String promptVersion,
               @Param("modelName") String modelName,
               @Param("totalCaseCount") int totalCaseCount);

    @Select("SELECT run_id, contract_key, dataset_version, role_name, prompt_version, model_name, status, "
            + "total_case_count, case_results_json, error_type, created_at, updated_at, completed_at "
            + "FROM t_resume_audit_evaluation_run WHERE run_id = #{runId}")
    ResumeEvidenceAuditEvaluationRunEntity find(@Param("runId") String runId);

    @Select("SELECT run_id, contract_key, dataset_version, role_name, prompt_version, model_name, status, "
            + "total_case_count, case_results_json, error_type, created_at, updated_at, completed_at "
            + "FROM t_resume_audit_evaluation_run WHERE contract_key = #{contractKey}")
    ResumeEvidenceAuditEvaluationRunEntity findByContract(@Param("contractKey") String contractKey);

    @Select("SELECT run_id, contract_key, dataset_version, role_name, prompt_version, model_name, status, "
            + "total_case_count, case_results_json, error_type, created_at, updated_at, completed_at "
            + "FROM t_resume_audit_evaluation_run WHERE status IN ('QUEUED','RUNNING') ORDER BY created_at")
    List<ResumeEvidenceAuditEvaluationRunEntity> findRecoverable();

    @Update("UPDATE t_resume_audit_evaluation_run SET status = #{status}, "
            + "case_results_json = #{caseResultsJson}, error_type = #{errorType}, "
            + "updated_at = CURRENT_TIMESTAMP(6), "
            + "completed_at = CASE WHEN #{completed} THEN CURRENT_TIMESTAMP(6) ELSE NULL END "
            + "WHERE run_id = #{runId}")
    int saveProgress(@Param("runId") String runId,
                     @Param("status") String status,
                     @Param("caseResultsJson") String caseResultsJson,
                     @Param("errorType") String errorType,
                     @Param("completed") boolean completed);
}
