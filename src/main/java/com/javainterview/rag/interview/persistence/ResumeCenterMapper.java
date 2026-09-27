package com.javainterview.rag.interview.persistence;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ResumeCenterMapper {

    String RECORD_COLUMNS = "id, name, target_role, current_version_id, version, created_at, updated_at";
    String VERSION_COLUMNS = "id, resume_id, version_number, content_hash, original_filename, "
            + "content_type, file_size, file_id, full_text, created_at";

    @Select("SELECT " + RECORD_COLUMNS + " FROM t_resume_record ORDER BY updated_at DESC, id DESC")
    List<ResumeRecordEntity> findAllRecords();

    @Select("SELECT " + RECORD_COLUMNS + " FROM t_resume_record WHERE id = #{id}")
    ResumeRecordEntity findRecord(@Param("id") String id);

    @Select("SELECT " + RECORD_COLUMNS + " FROM t_resume_record WHERE id = #{id} FOR UPDATE")
    ResumeRecordEntity lockRecord(@Param("id") String id);

    @Select("SELECT " + VERSION_COLUMNS + " FROM t_resume_version "
            + "WHERE resume_id = #{resumeId} ORDER BY version_number DESC")
    List<ResumeVersionEntity> findVersions(@Param("resumeId") String resumeId);

    @Select("SELECT " + VERSION_COLUMNS + " FROM t_resume_version WHERE id = #{id}")
    ResumeVersionEntity findVersion(@Param("id") String id);

    @Select("SELECT " + VERSION_COLUMNS + " FROM t_resume_version WHERE content_hash = #{contentHash}")
    ResumeVersionEntity findVersionByHash(@Param("contentHash") String contentHash);

    @Select("SELECT COALESCE(MAX(version_number), 0) FROM t_resume_version WHERE resume_id = #{resumeId}")
    int findLatestVersionNumber(@Param("resumeId") String resumeId);

    @Insert("INSERT INTO t_resume_record (id, name, target_role, current_version_id, version, created_at, updated_at) "
            + "VALUES (#{id}, #{name}, #{targetRole}, #{currentVersionId}, #{version}, #{createdAt}, #{updatedAt})")
    int insertRecord(ResumeRecordEntity entity);

    @Insert("INSERT INTO t_resume_version (id, resume_id, version_number, content_hash, original_filename, "
            + "content_type, file_size, file_id, full_text, created_at) VALUES (#{id}, #{resumeId}, #{versionNumber}, "
            + "#{contentHash}, #{originalFilename}, #{contentType}, #{fileSize}, #{fileId}, #{fullText}, #{createdAt})")
    int insertVersion(ResumeVersionEntity entity);

    @Update("UPDATE t_resume_version SET file_id = COALESCE(file_id, #{fileId}), "
            + "full_text = COALESCE(full_text, #{fullText}) WHERE id = #{versionId}")
    int enrichVersionStorage(@Param("versionId") String versionId,
                             @Param("fileId") String fileId,
                             @Param("fullText") String fullText);

    @Update("UPDATE t_resume_record SET name = #{name}, target_role = #{targetRole}, "
            + "current_version_id = #{currentVersionId}, version = version + 1, updated_at = #{updatedAt} "
            + "WHERE id = #{id} AND version = #{expectedVersion}")
    int updateCurrentVersion(@Param("id") String id,
                             @Param("name") String name,
                             @Param("targetRole") String targetRole,
                             @Param("currentVersionId") String currentVersionId,
                             @Param("expectedVersion") long expectedVersion,
                             @Param("updatedAt") LocalDateTime updatedAt);
}
