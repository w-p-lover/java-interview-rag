package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface InterviewSessionMapper extends BaseMapper<InterviewSessionEntity> {

    @Update("UPDATE t_interview_session SET answers_json = #{entity.answersJson}, "
            + "current_question_index = #{entity.currentQuestionIndex}, status = #{entity.status}, "
            + "version = #{entity.version}, updated_at = #{entity.updatedAt} "
            + "WHERE id = #{entity.id} AND version = #{expectedVersion} AND status = 'IN_PROGRESS'")
    int updateState(@Param("entity") InterviewSessionEntity entity,
                    @Param("expectedVersion") long expectedVersion);
}
