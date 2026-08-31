package com.javainterview.rag.observability;

import java.time.Instant;
import java.util.List;

public record BusinessObservabilityOverview(Instant generatedAt, long totalRequests,
                                            long totalFailures, List<BusinessOperationMetric> operations) {
    public BusinessObservabilityOverview {
        operations = List.copyOf(operations);
    }

    public record BusinessOperationMetric(String operation, long requestCount, long failureCount,
                                          double failureRate, double averageLatencyMillis,
                                          double maxLatencyMillis) {
    }
}
