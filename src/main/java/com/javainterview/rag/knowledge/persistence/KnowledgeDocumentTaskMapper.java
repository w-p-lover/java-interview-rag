package com.javainterview.rag.knowledge.persistence;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface KnowledgeDocumentTaskMapper {

    @Insert("INSERT INTO t_knowledge_document_task "
            + "(task_id, source, operation, status, created_at, updated_at) "
            + "VALUES (#{taskId}, #{source}, #{operation}, #{status}, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))")
    int insert(@Param("taskId") String taskId, @Param("source") String source,
               @Param("operation") String operation, @Param("status") String status);

    @Select("SELECT task_id, source, operation, status, version, imported, chunk_count, format, warning_count, "
            + "error_type, created_at, updated_at, completed_at FROM t_knowledge_document_task "
            + "WHERE task_id = #{taskId}")
    KnowledgeDocumentTaskEntity find(@Param("taskId") String taskId);

    @Select("SELECT task_id, source, operation, status, version, imported, chunk_count, format, warning_count, "
            + "error_type, created_at, updated_at, completed_at FROM t_knowledge_document_task "
            + "ORDER BY created_at DESC LIMIT #{limit}")
    List<KnowledgeDocumentTaskEntity> findRecent(@Param("limit") int limit);

    @Update("UPDATE t_knowledge_document_task SET status = #{status}, version = #{version}, imported = #{imported}, "
            + "chunk_count = #{chunkCount}, format = #{format}, warning_count = #{warningCount}, "
            + "error_type = #{errorType}, updated_at = CURRENT_TIMESTAMP(6), "
            + "completed_at = CURRENT_TIMESTAMP(6) WHERE task_id = #{taskId} "
            + "AND status NOT IN ('SUCCEEDED','FAILED','CANCELLED')")
    int markSucceeded(@Param("taskId") String taskId, @Param("status") String status,
                      @Param("version") String version, @Param("imported") boolean imported,
                      @Param("chunkCount") int chunkCount,
                      @Param("format") String format, @Param("warningCount") int warningCount,
                      @Param("errorType") String errorType);

    @Update("UPDATE t_knowledge_document_task SET status = 'FAILED', error_type = #{errorType}, "
            + "updated_at = CURRENT_TIMESTAMP(6), completed_at = CURRENT_TIMESTAMP(6) "
            + "WHERE task_id = #{taskId} AND status NOT IN ('SUCCEEDED','FAILED','CANCELLED')")
    int markFailed(@Param("taskId") String taskId, @Param("errorType") String errorType);

    @Update("UPDATE t_knowledge_document_task SET status = 'CANCELLED', error_type = 'CANCELLED_BY_USER', "
            + "updated_at = CURRENT_TIMESTAMP(6), completed_at = CURRENT_TIMESTAMP(6) "
            + "WHERE task_id = #{taskId} AND status = 'QUEUED'")
    int cancelQueued(@Param("taskId") String taskId);

    @Update("UPDATE t_knowledge_document_task SET status = #{status}, updated_at = CURRENT_TIMESTAMP(6) "
            + "WHERE task_id = #{taskId} AND status = 'QUEUED'")
    int markRunning(@Param("taskId") String taskId, @Param("status") String status);

    @Update("UPDATE t_knowledge_document_task SET status = 'FAILED', error_type = 'APPLICATION_RESTARTED', "
            + "updated_at = CURRENT_TIMESTAMP(6), completed_at = CURRENT_TIMESTAMP(6) "
            + "WHERE status = 'RUNNING'")
    int failRunningAfterRestart();
}
