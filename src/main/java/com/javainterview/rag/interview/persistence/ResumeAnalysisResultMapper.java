package com.javainterview.rag.interview.persistence;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ResumeAnalysisResultMapper {

    String COLUMNS = "id, resume_version_id, analysis_type, input_fingerprint, "
            + "contract_version, model_name, result_json, created_at, updated_at";

    @Insert("INSERT INTO t_resume_analysis_result (" + COLUMNS + ") VALUES ("
            + "#{id}, #{resumeVersionId}, #{analysisType}, #{inputFingerprint}, "
            + "#{contractVersion}, #{modelName}, #{resultJson}, #{createdAt}, #{updatedAt}) "
            + "ON DUPLICATE KEY UPDATE result_json = VALUES(result_json), "
            + "contract_version = VALUES(contract_version), model_name = VALUES(model_name), "
            + "updated_at = VALUES(updated_at)")
    int upsert(ResumeAnalysisResultEntity entity);

    @Select("SELECT " + COLUMNS + " FROM t_resume_analysis_result "
            + "WHERE resume_version_id = #{resumeVersionId} AND analysis_type = #{analysisType} "
            + "ORDER BY updated_at DESC, id DESC LIMIT 1")
    ResumeAnalysisResultEntity findLatest(
            @Param("resumeVersionId") String resumeVersionId,
            @Param("analysisType") String analysisType);

    @Select("SELECT " + COLUMNS + " FROM t_resume_analysis_result "
            + "WHERE resume_version_id = #{resumeVersionId} ORDER BY updated_at DESC, id DESC")
    List<ResumeAnalysisResultEntity> findAll(@Param("resumeVersionId") String resumeVersionId);
}
