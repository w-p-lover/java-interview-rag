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
    void reportsAcceptedAndCompletedRetrieval() {
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(1));
        try {
            KnowledgeRetrievalDeadline deadline =
                    new KnowledgeRetrievalDeadline(executor, Duration.ofSeconds(1));

            assertEquals("result", deadline.execute("Dense 向量检索", () -> "result"));

            long waitUntil = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
            while (deadline.snapshot().completed() == 0 && System.nanoTime() < waitUntil) {
                Thread.yield();
            }
            assertEquals(new KnowledgeRetrievalDeadline.Snapshot(1, 1, 1, 0, 0, 1, 0, 0),
                    deadline.snapshot());
        } finally {
            executor.shutdownNow();
        }
    }

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
            assertEquals(1, deadline.snapshot().submitted());
            assertEquals(1, deadline.snapshot().timedOut());
            assertEquals(0, deadline.snapshot().rejected());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void reportsRejectedWorkWhenThreadsAndQueueAreFull() throws InterruptedException {
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
        executor.execute(() -> { });
        try {
            KnowledgeRetrievalDeadline deadline =
                    new KnowledgeRetrievalDeadline(executor, Duration.ofSeconds(1));

            assertThrows(KnowledgeRetrievalUnavailableException.class,
                    () -> deadline.execute("Sparse 稀疏检索", () -> "never"));

            assertEquals(0, deadline.snapshot().submitted());
            assertEquals(1, deadline.snapshot().rejected());
            assertEquals(1, deadline.snapshot().active());
            assertEquals(1, deadline.snapshot().queued());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }
}
