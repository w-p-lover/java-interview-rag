package com.javainterview.rag.interview.bank;

import com.javainterview.rag.interview.question.InterviewDifficulty;
import com.javainterview.rag.interview.question.InterviewSkill;

import java.time.LocalDateTime;

public record BankGenerationTask(
        String id, String requestId, String source, String documentVersion,
        InterviewSkill skill, InterviewDifficulty difficulty, int questionCount, int followUpCount,
        Status status, int savedCount, int skippedCount, String attemptId,
        String workerId, LocalDateTime startedAt, LocalDateTime lastHeartbeatAt, int recoveryCount,
        String promptVersion, String modelName, String errorCode, long version,
        LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public enum Status { QUEUED, PROCESSING, COMPLETED, FAILED }

    public BankGenerationTask {
        if (recoveryCount < 0) {
            throw new IllegalArgumentException("任务恢复次数不能小于 0");
        }
    }

    public BankGenerationTask progress(Status nextStatus, int saved, int skipped,
                                        String attempt, String error) {
        return new BankGenerationTask(id, requestId, source, documentVersion, skill, difficulty,
                questionCount, followUpCount, nextStatus, saved, skipped, attempt,
                workerId, startedAt, lastHeartbeatAt, recoveryCount,
                promptVersion, modelName, error, version + 1, createdAt, LocalDateTime.now());
    }

    public BankGenerationTask startAttempt(String attempt, String worker, LocalDateTime now) {
        return new BankGenerationTask(id, requestId, source, documentVersion, skill, difficulty,
                questionCount, followUpCount, Status.PROCESSING, savedCount, skippedCount, attempt,
                worker, now, now, recoveryCount, promptVersion, modelName, null,
                version + 1, createdAt, now);
    }

    public BankGenerationTask queueForResume(boolean recovered, LocalDateTime now) {
        return new BankGenerationTask(id, requestId, source, documentVersion, skill, difficulty,
                questionCount, followUpCount, Status.QUEUED, savedCount, skippedCount, null,
                null, null, null, recoveryCount + (recovered ? 1 : 0),
                promptVersion, modelName, null, version + 1, createdAt, now);
    }
}
