package com.javainterview.rag.knowledge.persistence;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MicrometerRagAnswerCacheObserverTest {

    @Test
    void recordsHitMissAndBypassCounts() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        MicrometerRagAnswerCacheObserver observer = new MicrometerRagAnswerCacheObserver(registry);

        observer.hit();
        observer.miss();
        observer.miss();
        observer.bypass();

        assertEquals(1, MicrometerRagAnswerCacheObserver.count(registry, "HIT"));
        assertEquals(2, MicrometerRagAnswerCacheObserver.count(registry, "MISS"));
        assertEquals(1, MicrometerRagAnswerCacheObserver.count(registry, "BYPASS"));
    }
}
