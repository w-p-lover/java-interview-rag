package com.javainterview.rag.interview.bank;

import com.javainterview.rag.interview.question.InterviewDifficulty;
import com.javainterview.rag.interview.question.InterviewSkill;

import java.time.LocalDateTime;

public record BankGenerationTask(
        String id, String requestId, String source, String documentVersion,
        InterviewSkill skill, InterviewDifficulty difficulty, int questionCount, int followUpCount,
        Status status, int savedCount, int skippedCount, String attemptId,
        String promptVersion, String modelName, String errorCode, long version,
        LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public enum Status { QUEUED, PROCESSING, COMPLETED, FAILED }

    public BankGenerationTask progress(Status nextStatus, int saved, int skipped,
                                        String attempt, String error) {
        return new BankGenerationTask(id, requestId, source, documentVersion, skill, difficulty,
                questionCount, followUpCount, nextStatus, saved, skipped, attempt,
                promptVersion, modelName, error, version + 1, createdAt, LocalDateTime.now());
    }
}
