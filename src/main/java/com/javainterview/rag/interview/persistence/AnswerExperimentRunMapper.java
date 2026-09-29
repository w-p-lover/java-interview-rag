package com.javainterview.rag.interview.persistence;
import org.apache.ibatis.annotations.*; import java.util.List;
@Mapper public interface AnswerExperimentRunMapper {
 String COLUMNS="run_id,name,dataset_version,prompt_version,rubric_version,model_name,contract_hash,status,total_case_count,case_results_json,report_json,error_type,baseline_run_id,decision,decision_note,created_at,updated_at,completed_at,decided_at";
 @Insert("INSERT IGNORE INTO t_interview_answer_experiment_run ("+COLUMNS+") VALUES (#{run.runId},#{run.name},#{run.datasetVersion},#{run.promptVersion},#{run.rubricVersion},#{run.modelName},#{run.contractHash},#{run.status},#{run.totalCaseCount},#{run.caseResultsJson},NULL,NULL,NULL,NULL,NULL,#{run.createdAt},#{run.updatedAt},NULL,NULL)") int insert(@Param("run") AnswerExperimentRunEntity run);
 @Select("SELECT "+COLUMNS+" FROM t_interview_answer_experiment_run WHERE run_id=#{id}") AnswerExperimentRunEntity find(@Param("id") String id);
 @Select("SELECT "+COLUMNS+" FROM t_interview_answer_experiment_run WHERE contract_hash=#{hash}") AnswerExperimentRunEntity findByContract(@Param("hash") String hash);
 @Select("SELECT "+COLUMNS+" FROM t_interview_answer_experiment_run ORDER BY created_at DESC LIMIT #{limit}") List<AnswerExperimentRunEntity> recent(@Param("limit") int limit);
 @Select("SELECT "+COLUMNS+" FROM t_interview_answer_experiment_run WHERE status IN ('QUEUED','RUNNING') ORDER BY created_at") List<AnswerExperimentRunEntity> recoverable();
 @Update("UPDATE t_interview_answer_experiment_run SET status=#{status},case_results_json=#{results},report_json=#{report},error_type=#{errorType},updated_at=CURRENT_TIMESTAMP(6),completed_at=CASE WHEN #{completed} THEN CURRENT_TIMESTAMP(6) ELSE NULL END WHERE run_id=#{id}") int progress(@Param("id")String id,@Param("status")String status,@Param("results")String results,@Param("report")String report,@Param("errorType")String errorType,@Param("completed")boolean completed);
 @Update("UPDATE t_interview_answer_experiment_run SET baseline_run_id=#{baseline},decision=#{decision},decision_note=#{note},decided_at=CURRENT_TIMESTAMP(6),updated_at=CURRENT_TIMESTAMP(6) WHERE run_id=#{candidate}") int decision(@Param("candidate")String candidate,@Param("baseline")String baseline,@Param("decision")String decision,@Param("note")String note);
}
