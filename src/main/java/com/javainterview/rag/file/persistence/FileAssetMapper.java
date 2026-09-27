package com.javainterview.rag.file.persistence;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface FileAssetMapper {

    String COLUMNS = "id, purpose, object_key, content_hash, original_filename, content_type, file_size, "
            + "status, error_type, created_at, updated_at, deleted_at";

    @Select("SELECT " + COLUMNS + " FROM t_file_asset WHERE id = #{id}")
    FileAssetEntity find(@Param("id") String id);

    @Select("SELECT " + COLUMNS + " FROM t_file_asset "
            + "WHERE purpose = #{purpose} AND content_hash = #{contentHash}")
    FileAssetEntity findByPurposeAndHash(@Param("purpose") String purpose,
                                         @Param("contentHash") String contentHash);

    @Insert("INSERT INTO t_file_asset (id, purpose, object_key, content_hash, original_filename, "
            + "content_type, file_size, status, error_type, created_at, updated_at) VALUES "
            + "(#{id}, #{purpose}, #{objectKey}, #{contentHash}, #{originalFilename}, #{contentType}, "
            + "#{fileSize}, #{status}, #{errorType}, #{createdAt}, #{updatedAt})")
    int insert(FileAssetEntity entity);

    @Update("UPDATE t_file_asset SET original_filename = #{originalFilename}, content_type = #{contentType}, "
            + "file_size = #{fileSize}, status = 'UPLOADING', error_type = NULL, deleted_at = NULL, "
            + "updated_at = #{updatedAt} WHERE id = #{id}")
    int markUploading(@Param("id") String id,
                      @Param("originalFilename") String originalFilename,
                      @Param("contentType") String contentType,
                      @Param("fileSize") long fileSize,
                      @Param("updatedAt") LocalDateTime updatedAt);

    @Update("UPDATE t_file_asset SET status = 'READY', error_type = NULL, updated_at = #{updatedAt} "
            + "WHERE id = #{id} AND status = 'UPLOADING'")
    int markReady(@Param("id") String id, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("UPDATE t_file_asset SET status = 'FAILED', error_type = #{errorType}, updated_at = #{updatedAt} "
            + "WHERE id = #{id} AND status <> 'DELETED'")
    int markFailed(@Param("id") String id, @Param("errorType") String errorType,
                   @Param("updatedAt") LocalDateTime updatedAt);

    @Update("UPDATE t_file_asset SET status = 'DELETE_PENDING', error_type = NULL, updated_at = #{updatedAt} "
            + "WHERE id = #{id} AND status = 'READY'")
    int markDeletePending(@Param("id") String id, @Param("updatedAt") LocalDateTime updatedAt);

    @Update("UPDATE t_file_asset SET status = 'DELETED', error_type = NULL, deleted_at = #{deletedAt}, "
            + "updated_at = #{deletedAt} WHERE id = #{id} AND status IN ('DELETE_PENDING','FAILED','UPLOADING')")
    int markDeleted(@Param("id") String id, @Param("deletedAt") LocalDateTime deletedAt);

    @Select("SELECT "
            + "(SELECT COUNT(*) FROM t_resume_version WHERE file_id = #{id}) + "
            + "(SELECT COUNT(*) FROM t_knowledge_document_version WHERE file_id = #{id}) + "
            + "(SELECT COUNT(*) FROM t_knowledge_document_task WHERE file_id = #{id} "
            + "AND status IN ('QUEUED','RUNNING'))")
    int countBusinessReferences(@Param("id") String id);
}
