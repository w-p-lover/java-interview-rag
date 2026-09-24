package com.javainterview.rag.modelprovider;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ModelProviderMapper {
    String COLUMNS = "id, name, base_url, api_key_nonce, api_key_ciphertext, chat_model, "
            + "embedding_model, embedding_dimensions, temperature, enabled, version, created_at, updated_at";

    @Select("SELECT " + COLUMNS + " FROM t_model_provider ORDER BY id")
    List<ModelProviderEntity> findAll();

    @Select("SELECT " + COLUMNS + " FROM t_model_provider WHERE BINARY id = BINARY #{id}")
    ModelProviderEntity findById(@Param("id") String id);

    @Select("SELECT id, chat_provider_id, embedding_provider_id, version, updated_at "
            + "FROM t_model_provider_defaults WHERE id = 1")
    ModelProviderDefaultsEntity defaults();

    @Insert("INSERT INTO t_model_provider (" + COLUMNS + ") VALUES (#{id}, #{name}, #{baseUrl}, "
            + "#{apiKeyNonce}, #{apiKeyCiphertext}, #{chatModel}, #{embeddingModel}, "
            + "#{embeddingDimensions}, #{temperature}, #{enabled}, #{version}, #{createdAt}, #{updatedAt})")
    int insert(ModelProviderEntity entity);

    @Insert("INSERT IGNORE INTO t_model_provider_defaults "
            + "(id, chat_provider_id, embedding_provider_id, version, updated_at) "
            + "VALUES (1, #{chatProviderId}, #{embeddingProviderId}, #{version}, #{updatedAt})")
    int insertDefaults(ModelProviderDefaultsEntity entity);

    @Update("UPDATE t_model_provider SET name=#{provider.name}, base_url=#{provider.baseUrl}, "
            + "api_key_nonce=#{provider.apiKeyNonce}, api_key_ciphertext=#{provider.apiKeyCiphertext}, "
            + "chat_model=#{provider.chatModel}, embedding_model=#{provider.embeddingModel}, "
            + "embedding_dimensions=#{provider.embeddingDimensions}, temperature=#{provider.temperature}, "
            + "enabled=#{provider.enabled}, version=version+1, updated_at=#{provider.updatedAt} "
            + "WHERE BINARY id = BINARY #{provider.id} AND version=#{expectedVersion}")
    int update(@Param("provider") ModelProviderEntity provider, @Param("expectedVersion") long expectedVersion);

    @Delete("DELETE FROM t_model_provider WHERE BINARY id = BINARY #{id} AND version=#{expectedVersion}")
    int delete(@Param("id") String id, @Param("expectedVersion") long expectedVersion);

    @Update("UPDATE t_model_provider_defaults SET chat_provider_id=#{defaults.chatProviderId}, "
            + "embedding_provider_id=#{defaults.embeddingProviderId}, version=version+1, "
            + "updated_at=#{defaults.updatedAt} WHERE id=1 AND version=#{expectedVersion}")
    int updateDefaults(@Param("defaults") ModelProviderDefaultsEntity defaults,
                       @Param("expectedVersion") long expectedVersion);

    @Select("SELECT ("
            + "(SELECT COUNT(*) FROM t_knowledge_document_task WHERE status IN ('QUEUED','RUNNING')) + "
            + "(SELECT COUNT(*) FROM t_knowledge_bank_generation_task WHERE status IN ('QUEUED','PROCESSING')) + "
            + "(SELECT COUNT(*) FROM t_resume_audit_evaluation_run WHERE status IN ('QUEUED','RUNNING'))"
            + ")")
    long activeModelTaskCount();
}
