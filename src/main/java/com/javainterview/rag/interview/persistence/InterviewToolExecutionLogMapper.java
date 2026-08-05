package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InterviewToolExecutionLogMapper extends BaseMapper<InterviewToolExecutionLogEntity> {

    @Select("SELECT id, execution_id, tool_name, session_id, status, result_count, "
            + "elapsed_millis, error_type, created_at "
            + "FROM t_interview_tool_execution_log WHERE session_id = #{sessionId} "
            + "ORDER BY created_at DESC, id DESC LIMIT #{limit}")
    List<InterviewToolExecutionLogEntity> findRecent(
            @Param("sessionId") String sessionId,
            @Param("limit") int limit);
}
