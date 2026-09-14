package com.javainterview.rag.knowledge.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface RagAnswerFeedbackMapper extends BaseMapper<RagAnswerFeedbackEntity> {

    @Select("<script>SELECT id, question_hash, grounded, retrieval_strategy, feedback, review_status, review_note, created_at, reviewed_at "
            + "FROM t_rag_answer_feedback WHERE 1=1 "
            + "<if test='feedback != null'>AND feedback = #{feedback}</if> "
            + "<if test='reviewStatus != null'>AND review_status = #{reviewStatus}</if> "
            + "ORDER BY id DESC LIMIT #{limit}</script>")
    List<RagAnswerFeedbackEntity> findRecent(@Param("limit") int limit,
                                             @Param("feedback") String feedback,
                                             @Param("reviewStatus") String reviewStatus);

    @Update("UPDATE t_rag_answer_feedback SET review_status = #{reviewStatus}, review_note = #{reviewNote}, "
            + "reviewed_at = CURRENT_TIMESTAMP(6) WHERE id = #{id}")
    int review(@Param("id") long id, @Param("reviewStatus") String reviewStatus,
               @Param("reviewNote") String reviewNote);
}
