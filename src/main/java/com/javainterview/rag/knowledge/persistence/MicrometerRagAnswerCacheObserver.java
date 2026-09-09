package com.javainterview.rag.knowledge.persistence;

import com.javainterview.rag.knowledge.rag.RagAnswerCacheObserver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

public final class MicrometerRagAnswerCacheObserver implements RagAnswerCacheObserver {

    private static final String METRIC = "java.interview.rag.answer.cache";

    private final Counter hit;
    private final Counter miss;
    private final Counter bypass;

    public MicrometerRagAnswerCacheObserver(MeterRegistry meterRegistry) {
        this.hit = counter(meterRegistry, "HIT");
        this.miss = counter(meterRegistry, "MISS");
        this.bypass = counter(meterRegistry, "BYPASS");
    }

    public static long count(MeterRegistry meterRegistry, String outcome) {
        Counter counter = meterRegistry.find(METRIC).tag("outcome", outcome).counter();
        return counter == null ? 0 : Math.round(counter.count());
    }

    @Override
    public void hit() {
        hit.increment();
    }

    @Override
    public void miss() {
        miss.increment();
    }

    @Override
    public void bypass() {
        bypass.increment();
    }

    private Counter counter(MeterRegistry meterRegistry, String outcome) {
        return Counter.builder(METRIC)
                .tag("outcome", outcome)
                .description("RAG 问答缓存结果数量")
                .register(meterRegistry);
    }
}
