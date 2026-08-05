package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InterviewAssistantRequestLogMapper extends BaseMapper<InterviewAssistantRequestLogEntity> {

    @Select("SELECT id, request_id, model_name, status, tool_execution_count, "
            + "prompt_tokens, completion_tokens, total_tokens, elapsed_millis, "
            + "final_response_present, error_type, created_at "
            + "FROM t_interview_assistant_request_log "
            + "ORDER BY created_at DESC, id DESC LIMIT #{limit}")
    List<InterviewAssistantRequestLogEntity> findRecent(@Param("limit") int limit);
}
