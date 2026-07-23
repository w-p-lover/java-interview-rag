package com.javainterview.rag.knowledge.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
