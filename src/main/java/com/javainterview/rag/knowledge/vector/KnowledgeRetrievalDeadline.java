package com.javainterview.rag.knowledge.vector;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/** 为只读检索调用提供有界等待；底层任务迟到完成不会改变业务数据。 */
public final class KnowledgeRetrievalDeadline {

    private final Executor executor;
    private final ThreadPoolExecutor threadPoolExecutor;
    private final long timeoutMillis;
    private final AtomicLong submitted = new AtomicLong();
    private final AtomicLong timedOut = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();

    public KnowledgeRetrievalDeadline(Executor executor, Duration timeout) {
        this.executor = Objects.requireNonNull(executor, "检索执行器不能为空");
        this.threadPoolExecutor = executor instanceof ThreadPoolExecutor pool ? pool : null;
        Objects.requireNonNull(timeout, "检索截止时间不能为空");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("检索截止时间必须大于 0");
        }
        this.timeoutMillis = timeout.toMillis();
        if (timeoutMillis < 1) {
            throw new IllegalArgumentException("检索截止时间不能小于 1ms");
        }
    }

    public <T> T execute(String operation, Supplier<T> supplier) {
        Objects.requireNonNull(operation, "检索操作名称不能为空");
        Objects.requireNonNull(supplier, "检索操作不能为空");
        FutureTask<T> task = new FutureTask<>(supplier::get);
        try {
            executor.execute(task);
            submitted.incrementAndGet();
        } catch (RuntimeException exception) {
            rejected.incrementAndGet();
            throw new KnowledgeRetrievalUnavailableException(operation + "执行队列已满", exception);
        }
        try {
            return task.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            timedOut.incrementAndGet();
            cancel(task);
            throw new KnowledgeRetrievalTimeoutException(operation, timeoutMillis);
        } catch (InterruptedException exception) {
            cancel(task);
            Thread.currentThread().interrupt();
            throw new KnowledgeRetrievalUnavailableException(operation + "等待被中断", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new KnowledgeRetrievalUnavailableException(operation + "执行失败", cause);
        }
    }

    private void cancel(FutureTask<?> task) {
        task.cancel(true);
        if (threadPoolExecutor != null) {
            threadPoolExecutor.remove(task);
        }
    }

    public Snapshot snapshot() {
        if (threadPoolExecutor == null) {
            return new Snapshot(0, 0, submitted.get(), 0, 0, 0,
                    timedOut.get(), rejected.get());
        }
        return new Snapshot(threadPoolExecutor.getMaximumPoolSize(),
                threadPoolExecutor.getQueue().size() + threadPoolExecutor.getQueue().remainingCapacity(),
                submitted.get(), threadPoolExecutor.getActiveCount(), threadPoolExecutor.getQueue().size(),
                threadPoolExecutor.getCompletedTaskCount(), timedOut.get(), rejected.get());
    }

    public record Snapshot(int threadCapacity, int queueCapacity, long submitted,
                           int active, int queued, long completed, long timedOut, long rejected) {
    }
}
