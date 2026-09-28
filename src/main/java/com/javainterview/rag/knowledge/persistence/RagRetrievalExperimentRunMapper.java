package com.javainterview.rag.knowledge.persistence;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RagRetrievalExperimentRunMapper {

    String COLUMNS = "run_id, name, strategy, dataset_version, dataset_split, corpus_hash, "
            + "index_contract, git_sha, contract_hash, status, total_case_count, case_results_json, "
            + "report_json, error_type, baseline_run_id, decision, decision_note, created_at, "
            + "updated_at, completed_at, decided_at";

    @Insert("INSERT IGNORE INTO t_rag_retrieval_experiment_run (" + COLUMNS + ") VALUES ("
            + "#{run.runId}, #{run.name}, #{run.strategy}, #{run.datasetVersion}, #{run.datasetSplit}, "
            + "#{run.corpusHash}, #{run.indexContract}, #{run.gitSha}, #{run.contractHash}, "
            + "#{run.status}, #{run.totalCaseCount}, #{run.caseResultsJson}, NULL, NULL, NULL, NULL, "
            + "NULL, #{run.createdAt}, #{run.updatedAt}, NULL, NULL)")
    int insert(@Param("run") RagRetrievalExperimentRunEntity run);

    @Select("SELECT " + COLUMNS + " FROM t_rag_retrieval_experiment_run WHERE run_id=#{runId}")
    RagRetrievalExperimentRunEntity find(@Param("runId") String runId);

    @Select("SELECT " + COLUMNS + " FROM t_rag_retrieval_experiment_run "
            + "WHERE contract_hash=#{contractHash}")
    RagRetrievalExperimentRunEntity findByContract(@Param("contractHash") String contractHash);

    @Select("SELECT " + COLUMNS + " FROM t_rag_retrieval_experiment_run "
            + "ORDER BY created_at DESC LIMIT #{limit}")
    List<RagRetrievalExperimentRunEntity> findRecent(@Param("limit") int limit);

    @Select("SELECT " + COLUMNS + " FROM t_rag_retrieval_experiment_run "
            + "WHERE status IN ('QUEUED','RUNNING') ORDER BY created_at")
    List<RagRetrievalExperimentRunEntity> findRecoverable();

    @Update("UPDATE t_rag_retrieval_experiment_run SET status=#{status}, "
            + "case_results_json=#{caseResultsJson}, report_json=#{reportJson}, "
            + "error_type=#{errorType}, updated_at=CURRENT_TIMESTAMP(6), "
            + "completed_at=CASE WHEN #{completed} THEN CURRENT_TIMESTAMP(6) ELSE NULL END "
            + "WHERE run_id=#{runId}")
    int saveProgress(@Param("runId") String runId,
                     @Param("status") String status,
                     @Param("caseResultsJson") String caseResultsJson,
                     @Param("reportJson") String reportJson,
                     @Param("errorType") String errorType,
                     @Param("completed") boolean completed);

    @Update("UPDATE t_rag_retrieval_experiment_run SET baseline_run_id=#{baselineRunId}, "
            + "decision=#{decision}, decision_note=#{note}, decided_at=CURRENT_TIMESTAMP(6), "
            + "updated_at=CURRENT_TIMESTAMP(6) WHERE run_id=#{candidateRunId}")
    int saveDecision(@Param("candidateRunId") String candidateRunId,
                     @Param("baselineRunId") String baselineRunId,
                     @Param("decision") String decision,
                     @Param("note") String note);
}
