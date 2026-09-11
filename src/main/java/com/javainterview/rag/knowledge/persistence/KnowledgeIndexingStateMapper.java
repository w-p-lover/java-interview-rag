package com.javainterview.rag.knowledge.persistence;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface KnowledgeIndexingStateMapper {

    @Insert("INSERT INTO t_knowledge_indexing_state "
            + "(source, document_version, chunk_count, status, error_type) "
            + "VALUES (#{source}, #{documentVersion}, #{chunkCount}, #{status}, #{errorType}) "
            + "ON DUPLICATE KEY UPDATE chunk_count = VALUES(chunk_count), "
            + "status = VALUES(status), error_type = VALUES(error_type), "
            + "updated_at = CURRENT_TIMESTAMP(6)")
    int upsert(@Param("source") String source,
               @Param("documentVersion") String documentVersion,
               @Param("chunkCount") int chunkCount,
               @Param("status") String status,
               @Param("errorType") String errorType);

    @Select("SELECT source, document_version, chunk_count, status, error_type "
            + "FROM t_knowledge_indexing_state "
            + "WHERE BINARY source = BINARY #{source} AND document_version = #{documentVersion}")
    KnowledgeIndexingStateEntity find(@Param("source") String source,
                                      @Param("documentVersion") String documentVersion);

    @Delete("DELETE FROM t_knowledge_indexing_state WHERE BINARY source = BINARY #{source}")
    int deleteBySource(@Param("source") String source);
}
