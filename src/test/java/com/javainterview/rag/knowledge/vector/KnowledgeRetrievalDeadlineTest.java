package com.javainterview.rag.knowledge.vector;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class KnowledgeRetrievalDeadlineTest {

    @Test
    void removesTimedOutQueuedWorkToReleaseCapacity() throws InterruptedException {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1));
        CountDownLatch running = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        executor.execute(() -> {
            running.countDown();
            try {
                release.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        });
        running.await();
        try {
            KnowledgeRetrievalDeadline deadline =
                    new KnowledgeRetrievalDeadline(executor, Duration.ofMillis(20));

            assertThrows(KnowledgeRetrievalTimeoutException.class,
                    () -> deadline.execute("Dense 向量检索", () -> "late"));

            assertEquals(0, executor.getQueue().size());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }
}
