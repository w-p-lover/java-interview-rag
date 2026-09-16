package com.javainterview.rag.observability;

import java.time.Instant;
import java.util.List;

public record BusinessObservabilityOverview(Instant generatedAt, long totalRequests,
                                            long totalFailures, List<BusinessOperationMetric> operations,
                                            CacheMetric cache, TaskMetric tasks, RetrievalMetric retrieval,
                                            FeedbackMetric feedback) {
    public BusinessObservabilityOverview {
        operations = List.copyOf(operations);
    }

    public record BusinessOperationMetric(String operation, long requestCount, long failureCount,
                                          double failureRate, double averageLatencyMillis,
                                          double maxLatencyMillis) {
    }

    public record CacheMetric(long hitCount, long missCount, long bypassCount) {
    }

    public record TaskMetric(long queued, long running, long succeeded, long failed,
                             long cancelled, long rejected, long timedOut) {
    }

    public record RetrievalMetric(RetrievalChannelMetric dense, RetrievalChannelMetric sparse) { }

    public record RetrievalChannelMetric(int threadCapacity, int queueCapacity, long submitted,
                                         int active, int queued, long completed,
                                         long timedOut, long rejected) { }

    public record FeedbackMetric(long total, long helpful, long notHelpful, long pending, long evaluation) { }
}
