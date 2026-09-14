package com.javainterview.rag.knowledge.task;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public final class KnowledgeDocumentTaskMetrics {

    private final AtomicLong queued = new AtomicLong();
    private final AtomicLong running = new AtomicLong();
    private final AtomicLong succeeded = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();
    private final AtomicLong cancelled = new AtomicLong();
    private final AtomicLong rejected = new AtomicLong();
    private final AtomicLong timedOut = new AtomicLong();

    public void submitted() { queued.incrementAndGet(); }

    public void started() {
        decrementIfPositive(queued);
        running.incrementAndGet();
    }

    public void succeeded() { terminal(running, succeeded); }

    public void failed() { terminal(running, failed); }

    public void cancelled() {
        decrementIfPositive(queued);
        cancelled.incrementAndGet();
    }

    public void rejected() {
        decrementIfPositive(queued);
        rejected.incrementAndGet();
    }

    public void timedOut() { timedOut.incrementAndGet(); }

    public void timeoutTask() {
        decrementIfPositive(queued);
        decrementIfPositive(running);
        failed.incrementAndGet();
        timedOut.incrementAndGet();
    }

    public Snapshot snapshot() {
        return new Snapshot(queued.get(), running.get(), succeeded.get(), failed.get(), cancelled.get(), rejected.get(), timedOut.get());
    }

    private void terminal(AtomicLong active, AtomicLong completed) {
        decrementIfPositive(active);
        completed.incrementAndGet();
    }

    private void decrementIfPositive(AtomicLong value) {
        value.updateAndGet(current -> current > 0 ? current - 1 : 0);
    }

    public record Snapshot(long queued, long running, long succeeded, long failed, long cancelled, long rejected,
                           long timedOut) { }
}
