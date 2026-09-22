package com.javainterview.rag.interview.bank;

public record BankGenerationRecoveryPolicy(
        long heartbeatIntervalMillis,
        long staleTimeoutMillis,
        int maxRecoveryCount
) {
    public BankGenerationRecoveryPolicy {
        if (heartbeatIntervalMillis < 1_000) {
            throw new IllegalArgumentException("题库任务心跳间隔不能小于 1000 毫秒");
        }
        if (staleTimeoutMillis < heartbeatIntervalMillis * 2) {
            throw new IllegalArgumentException("题库任务失联时间必须至少为两倍心跳间隔");
        }
        if (maxRecoveryCount < 0 || maxRecoveryCount > 3) {
            throw new IllegalArgumentException("题库任务最大恢复次数必须在 0 到 3 之间");
        }
    }
}
