package com.javainterview.rag.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import com.javainterview.rag.knowledge.persistence.MicrometerRagAnswerCacheObserver;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskMetrics;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class BusinessObservabilityService {
    private static final String REQUEST_METRIC = "java.interview.business.requests";
    private static final String LATENCY_METRIC = "java.interview.business.request.latency";

    private final MeterRegistry meterRegistry;
    private final KnowledgeDocumentTaskMetrics taskMetrics;

    public BusinessObservabilityService(MeterRegistry meterRegistry, KnowledgeDocumentTaskMetrics taskMetrics) {
        this.meterRegistry = meterRegistry;
        this.taskMetrics = taskMetrics;
    }

    public BusinessObservabilityOverview overview() {
        Map<String, MutableMetric> metrics = new LinkedHashMap<>();
        for (Counter counter : meterRegistry.find(REQUEST_METRIC).counters()) {
            String operation = counter.getId().getTag("operation");
            String outcome = counter.getId().getTag("outcome");
            if (operation == null || outcome == null) {
                continue;
            }
            MutableMetric metric = metrics.computeIfAbsent(operation, ignored -> new MutableMetric());
            long count = Math.round(counter.count());
            metric.requestCount += count;
            if (!"SUCCESS".equals(outcome)) {
                metric.failureCount += count;
            }
        }
        for (Timer timer : meterRegistry.find(LATENCY_METRIC).timers()) {
            String operation = timer.getId().getTag("operation");
            if (operation == null) {
                continue;
            }
            MutableMetric metric = metrics.computeIfAbsent(operation, ignored -> new MutableMetric());
            metric.totalLatencyNanos = timer.totalTime(TimeUnit.NANOSECONDS);
            metric.maxLatencyNanos = timer.max(TimeUnit.NANOSECONDS);
        }
        long totalRequests = metrics.values().stream().mapToLong(metric -> metric.requestCount).sum();
        long totalFailures = metrics.values().stream().mapToLong(metric -> metric.failureCount).sum();
        var operations = new ArrayList<BusinessObservabilityOverview.BusinessOperationMetric>();
        metrics.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            MutableMetric metric = entry.getValue();
            double averageMillis = metric.requestCount == 0 ? 0
                    : metric.totalLatencyNanos / 1_000_000.0 / metric.requestCount;
            operations.add(new BusinessObservabilityOverview.BusinessOperationMetric(
                    entry.getKey(), metric.requestCount, metric.failureCount,
                    metric.requestCount == 0 ? 0 : metric.failureCount * 1.0 / metric.requestCount,
                    averageMillis, metric.maxLatencyNanos / 1_000_000.0));
        });
        return new BusinessObservabilityOverview(Instant.now(), totalRequests, totalFailures, operations,
                new BusinessObservabilityOverview.CacheMetric(
                        MicrometerRagAnswerCacheObserver.count(meterRegistry, "HIT"),
                        MicrometerRagAnswerCacheObserver.count(meterRegistry, "MISS"),
                        MicrometerRagAnswerCacheObserver.count(meterRegistry, "BYPASS")),
                toTaskMetric(taskMetrics.snapshot()));
    }

    private BusinessObservabilityOverview.TaskMetric toTaskMetric(KnowledgeDocumentTaskMetrics.Snapshot snapshot) {
        return new BusinessObservabilityOverview.TaskMetric(snapshot.queued(), snapshot.running(), snapshot.succeeded(),
                snapshot.failed(), snapshot.cancelled(), snapshot.rejected(), snapshot.timedOut());
    }

    private static final class MutableMetric {
        private long requestCount;
        private long failureCount;
        private double totalLatencyNanos;
        private double maxLatencyNanos;
    }
}
