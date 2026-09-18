package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface QuestionBankMapper extends BaseMapper<BankQuestionEntity> {
    @Select("SELECT * FROM t_knowledge_bank_question WHERE BINARY source = #{source} ORDER BY updated_at DESC, id LIMIT 500")
    List<BankQuestionEntity> list(@Param("source") String source);

    @Update("UPDATE t_knowledge_bank_question SET question_json=#{question.questionJson}, content_key=#{question.contentKey}, status=#{question.status}, version=#{question.version}, updated_at=#{question.updatedAt} WHERE id=#{question.id} AND version=#{expectedVersion}")
    int updateVersioned(@Param("question") BankQuestionEntity question,
                        @Param("expectedVersion") long expectedVersion);

    @Delete("DELETE FROM t_knowledge_bank_question WHERE id=#{id} AND version=#{version}")
    int deleteVersioned(@Param("id") String id, @Param("version") long version);

    @Insert("INSERT IGNORE INTO t_knowledge_bank_question_usage (session_id, question_id, question_index, created_at) VALUES (#{sessionId}, #{questionId}, #{questionIndex}, NOW())")
    int insertUsage(@Param("sessionId") String sessionId, @Param("questionId") String questionId,
                    @Param("questionIndex") int questionIndex);

    @Update("UPDATE t_knowledge_bank_question SET use_count=use_count+1, last_used_at=NOW() WHERE id=#{id}")
    int incrementUsage(@Param("id") String id);
}
