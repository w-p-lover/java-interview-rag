package com.javainterview.rag.knowledge.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    @Select("SELECT current_version.id, current_version.source, current_version.version, "
            + "current_version.content_length, current_version.created_at "
            + "FROM t_knowledge_document_version current_version "
            + "LEFT JOIN t_knowledge_document_version newer_version "
            + "ON BINARY newer_version.source = BINARY current_version.source "
            + "AND newer_version.id > current_version.id "
            + "WHERE newer_version.id IS NULL "
            + "ORDER BY current_version.created_at DESC, current_version.id DESC")
    List<KnowledgeDocumentVersionEntity> selectCurrentVersions();
}
