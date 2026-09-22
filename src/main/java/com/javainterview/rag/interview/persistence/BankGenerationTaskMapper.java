package com.javainterview.rag.interview.persistence;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BankGenerationTaskMapper extends BaseMapper<BankGenerationTaskEntity> {
    @Select("SELECT * FROM t_knowledge_bank_generation_task WHERE request_id=#{requestId} LIMIT 1")
    BankGenerationTaskEntity findByRequestId(@Param("requestId") String requestId);

    @Select("SELECT * FROM t_knowledge_bank_generation_task WHERE active_key=#{activeKey} LIMIT 1")
    BankGenerationTaskEntity findByActiveKey(@Param("activeKey") String activeKey);

    @Select("SELECT * FROM t_knowledge_bank_generation_task WHERE BINARY source=#{source} ORDER BY created_at DESC, id LIMIT 20")
    List<BankGenerationTaskEntity> list(@Param("source") String source);

    @Select("SELECT * FROM t_knowledge_bank_generation_task WHERE status IN ('QUEUED','PROCESSING')")
    List<BankGenerationTaskEntity> unfinished();

    @Update("UPDATE t_knowledge_bank_generation_task SET task_json=#{task.taskJson}, status=#{task.status}, "
            + "active_key=#{task.activeKey}, attempt_id=#{task.attemptId}, worker_id=#{task.workerId}, "
            + "started_at=#{task.startedAt}, last_heartbeat_at=#{task.lastHeartbeatAt}, "
            + "recovery_count=#{task.recoveryCount}, version=#{task.version}, updated_at=#{task.updatedAt} "
            + "WHERE id=#{task.id} AND version=#{expectedVersion}")
    int updateVersioned(@Param("task") BankGenerationTaskEntity task,
                        @Param("expectedVersion") long expectedVersion);

    @Update("UPDATE t_knowledge_bank_generation_task SET task_json=#{task.taskJson}, status=#{task.status}, "
            + "active_key=NULL, version=#{task.version}, updated_at=#{task.updatedAt} "
            + "WHERE id=#{task.id} AND version=#{expectedVersion} AND status='QUEUED'")
    int cancelQueued(@Param("task") BankGenerationTaskEntity task,
                     @Param("expectedVersion") long expectedVersion);

    @Update("UPDATE t_knowledge_bank_generation_task SET last_heartbeat_at=#{heartbeatAt}, "
            + "updated_at=#{heartbeatAt} WHERE id=#{taskId} AND status='PROCESSING' "
            + "AND attempt_id=#{attemptId} AND worker_id=#{workerId}")
    int heartbeat(@Param("taskId") String taskId,
                  @Param("attemptId") String attemptId,
                  @Param("workerId") String workerId,
                  @Param("heartbeatAt") LocalDateTime heartbeatAt);

    @Update("UPDATE t_knowledge_bank_generation_task SET task_json=#{task.taskJson}, status=#{task.status}, "
            + "active_key=#{task.activeKey}, attempt_id=#{task.attemptId}, worker_id=#{task.workerId}, "
            + "started_at=#{task.startedAt}, last_heartbeat_at=#{task.lastHeartbeatAt}, "
            + "recovery_count=#{task.recoveryCount}, version=#{task.version}, updated_at=#{task.updatedAt} "
            + "WHERE id=#{task.id} AND version=#{expectedVersion} AND status='PROCESSING' "
            + "AND attempt_id=#{expectedAttemptId} "
            + "AND (last_heartbeat_at IS NULL OR last_heartbeat_at <= #{staleBefore})")
    int recoverStale(@Param("task") BankGenerationTaskEntity task,
                     @Param("expectedVersion") long expectedVersion,
                     @Param("expectedAttemptId") String expectedAttemptId,
                     @Param("staleBefore") LocalDateTime staleBefore);
}
