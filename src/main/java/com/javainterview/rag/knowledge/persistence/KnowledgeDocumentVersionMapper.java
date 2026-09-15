package com.javainterview.rag.knowledge.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface KnowledgeDocumentVersionMapper extends BaseMapper<KnowledgeDocumentVersionEntity> {

    @Select("SELECT COUNT(*) FROM t_knowledge_document_version "
            + "WHERE BINARY source = BINARY #{source} AND version = #{version}")
    int countBySourceAndVersion(@Param("source") String source, @Param("version") String version);

    @Delete("DELETE FROM t_knowledge_document_version "
            + "WHERE BINARY source = BINARY #{source} AND version = #{version}")
    int deleteBySourceAndVersion(@Param("source") String source, @Param("version") String version);

    @Delete("DELETE FROM t_knowledge_document_version WHERE BINARY source = BINARY #{source}")
    int deleteBySource(@Param("source") String source);

    @Update("UPDATE t_knowledge_document_version SET outline_json = #{outlineJson} "
            + "WHERE BINARY source = BINARY #{source} AND version = #{version}")
    int updateOutline(@Param("source") String source, @Param("version") String version,
                      @Param("outlineJson") String outlineJson);

    @Update("UPDATE t_knowledge_document_version SET activated_at = ("
            + "SELECT next_activated_at FROM ("
            + "SELECT GREATEST(CURRENT_TIMESTAMP(6), "
            + "DATE_ADD(MAX(activated_at), INTERVAL 1 MICROSECOND)) AS next_activated_at "
            + "FROM t_knowledge_document_version WHERE BINARY source = BINARY #{source}"
            + ") activation_clock) "
            + "WHERE BINARY source = BINARY #{source} AND version = #{version}")
    int activate(@Param("source") String source, @Param("version") String version);

    @Select("SELECT COUNT(*) FROM t_knowledge_document_version "
            + "WHERE BINARY source = BINARY #{source} AND version = #{version} "
            + "AND content IS NOT NULL AND sections_json IS NOT NULL")
    int countSnapshotBySourceAndVersion(@Param("source") String source,
                                        @Param("version") String version);

    @Select("SELECT id, source, version, content_length, outline_json, content, sections_json, activated_at, created_at "
            + "FROM t_knowledge_document_version WHERE BINARY source = BINARY #{source} AND version = #{version}")
    KnowledgeDocumentVersionEntity selectBySourceAndVersion(@Param("source") String source,
                                                             @Param("version") String version);

    @Select("SELECT current_version.id, current_version.source, current_version.version, "
            + "current_version.content_length, current_version.outline_json, current_version.content, "
            + "current_version.sections_json, current_version.activated_at, current_version.created_at "
            + "FROM t_knowledge_document_version current_version "
            + "LEFT JOIN t_knowledge_document_version newer_version "
            + "ON BINARY newer_version.source = BINARY current_version.source "
            + "AND (newer_version.activated_at > current_version.activated_at "
            + "OR (newer_version.activated_at = current_version.activated_at AND newer_version.id > current_version.id)) "
            + "WHERE newer_version.id IS NULL "
            + "ORDER BY current_version.activated_at DESC, current_version.id DESC")
    List<KnowledgeDocumentVersionEntity> selectCurrentVersions();

    @Select("SELECT id, source, version, content_length, outline_json, content, sections_json, activated_at, created_at "
            + "FROM t_knowledge_document_version "
            + "WHERE BINARY source = BINARY #{source} "
            + "ORDER BY activated_at DESC, id DESC")
    List<KnowledgeDocumentVersionEntity> selectVersionsBySource(@Param("source") String source);
}
