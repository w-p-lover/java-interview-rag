package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface InterviewSessionMapper extends BaseMapper<InterviewSessionEntity> {

    @Select("SELECT * FROM t_interview_session WHERE request_id = #{requestId} LIMIT 1")
    InterviewSessionEntity selectByRequestId(@Param("requestId") String requestId);

    @Update("UPDATE t_interview_session SET questions_json = #{entity.questionsJson}, "
            + "answers_json = #{entity.answersJson}, report_json = #{entity.reportJson}, "
            + "current_question_index = #{entity.currentQuestionIndex}, status = #{entity.status}, "
            + "version = #{entity.version}, updated_at = #{entity.updatedAt} "
            + "WHERE id = #{entity.id} AND version = #{expectedVersion} AND status = 'IN_PROGRESS'")
    int updateState(@Param("entity") InterviewSessionEntity entity,
                    @Param("expectedVersion") long expectedVersion);
}
