package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ResumeEvidenceAuditRequestLogMapper
        extends BaseMapper<ResumeEvidenceAuditRequestLogEntity> {

    @Select("SELECT id, request_id, role_name, prompt_version, model_name, status, "
            + "prompt_tokens, completion_tokens, total_tokens, elapsed_millis, error_type, created_at "
            + "FROM t_resume_evidence_audit_request_log "
            + "ORDER BY created_at DESC, id DESC LIMIT #{limit}")
    List<ResumeEvidenceAuditRequestLogEntity> findRecent(@Param("limit") int limit);
}
