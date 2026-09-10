export const KNOWLEDGE_DEMO_FILE_NAME = "java-thread-pool-demo.md";
export const KNOWLEDGE_DEMO_SOURCE = "demo/java/concurrency/thread-pool.md";

export const KNOWLEDGE_DEMO_CONTENT = `# Java 线程池面试要点

## ThreadPoolExecutor 核心参数

ThreadPoolExecutor 的七个核心参数是 corePoolSize、maximumPoolSize、keepAliveTime、unit、workQueue、threadFactory 和 handler。

提交任务时，线程池先创建核心线程；核心线程达到上限后，任务进入工作队列；队列已满时继续创建非核心线程；线程数达到 maximumPoolSize 后执行拒绝策略。

| 参数 | 作用 |
| --- | --- |
| corePoolSize | 长期保留的核心线程数量 |
| maximumPoolSize | 线程池允许创建的最大线程数量 |
| workQueue | 核心线程繁忙时保存等待任务 |
| handler | 线程数和队列都达到上限后的拒绝策略 |

## 常见拒绝策略

- AbortPolicy：直接抛出 RejectedExecutionException，是默认策略。
- CallerRunsPolicy：由提交任务的调用线程执行任务，可以形成反压，但会增加调用方耗时。
- DiscardPolicy：静默丢弃当前任务。
- DiscardOldestPolicy：丢弃队列中等待时间最长的任务，再尝试提交当前任务。

## 有界队列与容量保护

生产环境通常优先使用有界队列。无界队列会让任务持续堆积，可能增加延迟并最终导致内存溢出；有界队列可以把容量上限显式化，并配合拒绝策略、监控和降级保护系统。

线程数不能只按固定公式设置。CPU 密集任务主要受核心数限制，I/O 密集任务可以适当增加线程，但仍应通过真实压测观察吞吐、P95 延迟、队列长度、拒绝次数和资源占用。

## 安全关闭

shutdown() 会拒绝新任务，并等待已提交任务执行完成。shutdownNow() 会尝试中断正在执行的任务，并返回队列中尚未开始的任务。应用关闭时通常先调用 shutdown()，等待有限时间后再根据业务边界决定是否调用 shutdownNow()。

\`\`\`java
executor.shutdown();
if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
    executor.shutdownNow();
}
\`\`\`

## 面试回答边界

线程池能够复用线程、限制并发和管理任务队列，但不能自动保证业务幂等、任务不丢失或分布式一致性。需要根据任务重要性补充持久化、重试、幂等键、监控和人工补偿。`;
