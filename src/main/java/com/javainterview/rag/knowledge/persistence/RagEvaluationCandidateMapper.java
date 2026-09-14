package com.javainterview.rag.knowledge.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface RagEvaluationCandidateMapper extends BaseMapper<RagEvaluationCandidateEntity> {
    @Insert("INSERT INTO t_rag_evaluation_candidate (id, feedback_id, question, expected_sources_json, expected_answer_points_json, status, created_at, reviewed_at) VALUES (#{entity.id},#{entity.feedbackId},#{entity.question},#{entity.expectedSourcesJson},#{entity.expectedAnswerPointsJson},#{entity.status},#{entity.createdAt},#{entity.reviewedAt}) ON DUPLICATE KEY UPDATE id=id")
    int insertIfAbsent(@Param("entity") RagEvaluationCandidateEntity entity);
    @Select("<script>SELECT * FROM t_rag_evaluation_candidate <where><if test='status != null'>status=#{status}</if></where> ORDER BY created_at DESC LIMIT #{limit}</script>")
    List<RagEvaluationCandidateEntity> findRecent(@Param("limit") int limit, @Param("status") String status);
    @Update("UPDATE t_rag_evaluation_candidate SET status=#{status}, reviewed_at=CURRENT_TIMESTAMP(6) WHERE id=#{id}")
    int review(@Param("id") String id, @Param("status") String status);
}
