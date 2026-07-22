package com.javainterview.rag.knowledge.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface KnowledgeDocumentVersionMapper extends BaseMapper<KnowledgeDocumentVersionEntity> {

    @Delete("DELETE FROM t_knowledge_document_version WHERE source = #{source} AND version = #{version}")
    int deleteBySourceAndVersion(@Param("source") String source, @Param("version") String version);
}
