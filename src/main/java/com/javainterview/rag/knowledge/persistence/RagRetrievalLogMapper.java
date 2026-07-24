package com.javainterview.rag.knowledge.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RagRetrievalLogMapper extends BaseMapper<RagRetrievalLogEntity> {

    @Select("SELECT id, question, top_k, similarity_threshold, prompt_version, status, grounded, "
            + "retrieved_sources_json, elapsed_millis, error_type, created_at "
            + "FROM t_rag_retrieval_log ORDER BY id DESC LIMIT #{limit}")
    List<RagRetrievalLogEntity> findRecent(@Param("limit") int limit);
}
