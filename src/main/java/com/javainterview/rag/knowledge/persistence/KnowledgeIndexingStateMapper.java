package com.javainterview.rag.knowledge.persistence;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface KnowledgeIndexingStateMapper {

    @Insert("INSERT INTO t_knowledge_indexing_state "
            + "(source, document_version, chunk_count, status, error_type) "
            + "VALUES (#{source}, #{documentVersion}, #{chunkCount}, #{status}, #{errorType}) "
            + "ON DUPLICATE KEY UPDATE chunk_count = VALUES(chunk_count), "
            + "status = VALUES(status), error_type = VALUES(error_type), "
            + "published_generation = CASE WHEN VALUES(status) = 'READY' "
            + "AND published_generation IS NULL THEN 'legacy' ELSE published_generation END, "
            + "updated_at = CURRENT_TIMESTAMP(6)")
    int upsert(@Param("source") String source,
               @Param("documentVersion") String documentVersion,
               @Param("chunkCount") int chunkCount,
               @Param("status") String status,
               @Param("errorType") String errorType);

    @Select("SELECT source, document_version, chunk_count, status, error_type, "
            + "published_generation, building_generation, building_chunk_count, attempt_id "
            + "FROM t_knowledge_indexing_state "
            + "WHERE BINARY source = BINARY #{source} AND document_version = #{documentVersion}")
    KnowledgeIndexingStateEntity find(@Param("source") String source,
                                      @Param("documentVersion") String documentVersion);

    @Delete("DELETE FROM t_knowledge_indexing_state WHERE BINARY source = BINARY #{source}")
    int deleteBySource(@Param("source") String source);

    @Insert("INSERT INTO t_knowledge_indexing_state "
            + "(source, document_version, chunk_count, status, error_type, "
            + "published_generation, building_generation, building_chunk_count, attempt_id) "
            + "VALUES (#{source}, #{documentVersion}, 0, 'INDEXING', NULL, NULL, "
            + "#{generation}, #{chunkCount}, #{attemptId}) "
            + "ON DUPLICATE KEY UPDATE status = 'INDEXING', error_type = NULL, "
            + "building_generation = VALUES(building_generation), "
            + "building_chunk_count = VALUES(building_chunk_count), "
            + "attempt_id = VALUES(attempt_id), updated_at = CURRENT_TIMESTAMP(6)")
    int beginBuild(@Param("source") String source,
                   @Param("documentVersion") String documentVersion,
                   @Param("chunkCount") int chunkCount,
                   @Param("generation") String generation,
                   @Param("attemptId") String attemptId);

    @Update("UPDATE t_knowledge_indexing_state SET chunk_count = #{chunkCount}, status = 'READY', "
            + "error_type = NULL, published_generation = #{generation}, "
            + "building_generation = NULL, building_chunk_count = NULL, attempt_id = NULL, "
            + "updated_at = CURRENT_TIMESTAMP(6) "
            + "WHERE BINARY source = BINARY #{source} AND document_version = #{documentVersion} "
            + "AND building_generation = #{generation} AND (attempt_id <=> #{attemptId})")
    int publishBuild(@Param("source") String source,
                     @Param("documentVersion") String documentVersion,
                     @Param("chunkCount") int chunkCount,
                     @Param("generation") String generation,
                     @Param("attemptId") String attemptId);

    @Update("UPDATE t_knowledge_indexing_state SET published_generation = NULL "
            + "WHERE BINARY source = BINARY #{source} AND document_version <> #{documentVersion}")
    int clearOtherPublishedVersions(@Param("source") String source,
                                    @Param("documentVersion") String documentVersion);

    @Update("UPDATE t_knowledge_indexing_state SET status = 'FAILED', error_type = #{errorType}, "
            + "building_generation = CASE WHEN #{temporaryCleaned} THEN NULL ELSE building_generation END, "
            + "building_chunk_count = CASE WHEN #{temporaryCleaned} THEN NULL ELSE building_chunk_count END, "
            + "attempt_id = NULL, updated_at = CURRENT_TIMESTAMP(6) "
            + "WHERE BINARY source = BINARY #{source} AND document_version = #{documentVersion} "
            + "AND building_generation = #{generation} AND (attempt_id <=> #{attemptId})")
    int failBuild(@Param("source") String source,
                  @Param("documentVersion") String documentVersion,
                  @Param("generation") String generation,
                  @Param("attemptId") String attemptId,
                  @Param("errorType") String errorType,
                  @Param("temporaryCleaned") boolean temporaryCleaned);

    @Select("SELECT source, document_version, chunk_count, status, error_type, "
            + "published_generation, building_generation, building_chunk_count, attempt_id "
            + "FROM t_knowledge_indexing_state WHERE published_generation IS NOT NULL")
    List<KnowledgeIndexingStateEntity> findPublished();
}
