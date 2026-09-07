package com.javainterview.rag.knowledge.evaluation;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FixedRagEvaluationDataset {

    private static final String COLLECTIONS = "java/collections.md";
    private static final String CONCURRENCY = "java/concurrency.md";
    private static final String CONCURRENT_COLLECTIONS = "java/concurrent-collections.md";
    private static final String JVM = "java/jvm.md";
    private static final String TRANSACTIONS = "mysql/transactions.md";
    private static final String INDEXES = "mysql/indexes.md";
    private static final String RAG_BASICS = "rag/rag-basics.md";
    private static final String AGENT_BASICS = "rag/agent-basics.md";

    private static final List<RagEvaluationCase> CASES = List.of(
            evaluationCase("java-collections-hashmap",
                    "HashMap 在什么条件下树化，平均查询复杂度是多少？",
                    RagEvaluationCaseType.EXACT_TERM,
                    List.of(COLLECTIONS),
                    List.of(path("Java 集合", "HashMap 的查询过程"), path("Java 集合", "链表树化条件")),
                    List.of(chunk(COLLECTIONS, 0), chunk(COLLECTIONS, 1)),
                    List.of(chunk(CONCURRENCY, 0)),
                    "Java 8 的桶可由链表树化为红黑树", "链表长度达到 8", "数组容量至少为 64", "常见平均查询复杂度为 O(1)"),
            evaluationCase("java-concurrency-thread-pool",
                    "ThreadPoolExecutor 接收到新任务后如何决定创建线程、入队和拒绝？",
                    RagEvaluationCaseType.EXACT_TERM,
                    List.of(CONCURRENCY),
                    List.of(path("Java 并发", "ThreadPoolExecutor 接收任务")),
                    List.of(chunk(CONCURRENCY, 0)),
                    List.of(chunk(CONCURRENCY, 1)),
                    "先创建核心线程", "核心线程满后任务进入工作队列", "队列满后在最大线程数内创建非核心线程", "超过最大线程数后执行拒绝策略"),
            evaluationCase("java-concurrency-volatile",
                    "volatile 能保证什么，为什么不能保证自增操作的原子性？",
                    RagEvaluationCaseType.EXACT_TERM,
                    List.of(CONCURRENCY),
                    List.of(path("Java 并发", "volatile 的可见性与有序性")),
                    List.of(chunk(CONCURRENCY, 1)),
                    List.of(chunk(CONCURRENCY, 0)),
                    "保证可见性", "禁止特定指令重排序", "自增包含读取、计算和写回多个步骤", "多线程自增需要原子类或锁"),
            evaluationCase("java-jvm-gc-roots",
                    "JVM 如何通过 GC Roots 判断对象是否可回收？",
                    RagEvaluationCaseType.EXACT_TERM,
                    List.of(JVM),
                    List.of(path("JVM 内存管理", "GC Roots 与可达性分析")),
                    "从 GC Roots 开始进行可达性分析", "可达对象不会被回收", "不可达对象会进入回收判定", "栈帧局部变量和静态字段可以作为 GC Roots"),

            evaluationCase("java-collections-resize-or-treeify",
                    "哈希桶冲突变多时，什么情况下优先扩容而不是转换成树？",
                    RagEvaluationCaseType.SIMILAR_SECTION,
                    List.of(COLLECTIONS),
                    List.of(path("Java 集合", "链表树化条件")),
                    "桶内节点数量达到 8", "数组容量小于 64 时优先扩容", "重新分布可以减少冲突"),
            evaluationCase("java-concurrent-map-read",
                    "高并发读写映射时，为什么不应直接用普通 HashMap，常见替代结构如何降低竞争？",
                    RagEvaluationCaseType.SIMILAR_SECTION,
                    List.of(CONCURRENT_COLLECTIONS),
                    List.of(path("Java 并发集合", "ConcurrentHashMap 的并发控制")),
                    "普通 HashMap 不提供并发安全", "ConcurrentHashMap 使用更细粒度的并发控制", "读取通常不需要锁住整个容器"),
            evaluationCase("mysql-snapshot-or-current-read",
                    "为什么同一事务里的普通查询和带 FOR UPDATE 的查询可能采用不同的版本读取方式？",
                    RagEvaluationCaseType.SIMILAR_SECTION,
                    List.of(TRANSACTIONS),
                    List.of(path("MySQL 事务", "快照读与当前读")),
                    "普通查询通常是快照读", "FOR UPDATE 属于当前读", "当前读读取最新可用版本并配合锁"),
            evaluationCase("rag-filter-noisy-context",
                    "检索候选太多导致上下文混入不相关内容，应由哪两个参数先控制数量和可靠性？",
                    RagEvaluationCaseType.SIMILAR_SECTION,
                    List.of(RAG_BASICS),
                    List.of(path("RAG 检索基础", "topK 与相似度阈值")),
                    "topK 控制候选片段数量", "相似度阈值过滤相关性不足的资料", "候选过多会引入噪声"),

            evaluationCase("java-heading-expansion-load-factor",
                    "请定位讲解默认负载因子、触发扩容和节点重新分布的章节。",
                    RagEvaluationCaseType.HEADING_PATH,
                    List.of(COLLECTIONS),
                    List.of(path("Java 集合", "扩容与负载因子")),
                    "默认负载因子为 0.75", "元素数量超过容量与负载因子的乘积时触发扩容", "节点会重新分布"),
            evaluationCase("mysql-heading-composite-index",
                    "请找到说明联合索引最左前缀以及跳过前导列影响的章节。",
                    RagEvaluationCaseType.HEADING_PATH,
                    List.of(INDEXES),
                    List.of(path("MySQL 索引", "联合索引与最左前缀")),
                    "联合索引按列顺序组织", "查询应从最左列开始匹配", "跳过前导列通常不能充分利用索引"),
            evaluationCase("rag-heading-tool-boundary",
                    "请定位讲解工具参数校验、权限边界和人工确认的章节。",
                    RagEvaluationCaseType.HEADING_PATH,
                    List.of(AGENT_BASICS),
                    List.of(path("Agent 与工具调用", "工具治理边界")),
                    "服务端校验工具参数", "限制工具权限", "高风险写操作需要人工确认"),

            evaluationCase("java-ambiguous-map-performance",
                    "Map 查询慢一定是时间复杂度变了吗？应检查哪些与冲突和容量有关的因素？",
                    RagEvaluationCaseType.AMBIGUOUS,
                    List.of(COLLECTIONS),
                    List.of(path("Java 集合", "HashMap 的查询过程"), path("Java 集合", "扩容与负载因子")),
                    "哈希冲突会增加桶内查找成本", "合理实现 hashCode", "扩容会重新分布节点", "初始化容量应结合数据量"),
            evaluationCase("mysql-ambiguous-query-consistency",
                    "查询看到旧数据是索引失效还是事务可见性造成的，应该依据什么判断？",
                    RagEvaluationCaseType.AMBIGUOUS,
                    List.of(TRANSACTIONS),
                    List.of(path("MySQL 事务", "MVCC 一致性读取")),
                    "通过 Read View 判断版本可见性", "undo log 支持读取历史版本", "隔离级别影响 Read View 的创建与复用"),
            evaluationCase("rag-ambiguous-grounded-answer",
                    "系统已经召回了片段但回答仍不可信，如何区分检索问题和生成问题？",
                    RagEvaluationCaseType.AMBIGUOUS,
                    List.of(RAG_BASICS),
                    List.of(path("RAG 检索基础", "来源引用"), path("RAG 检索基础", "评估与排查")),
                    "保留实际召回来源", "核验回答依据", "区分没有召回正确资料和模型没有正确使用资料"),

            evaluationCase("no-answer-redis-persistence",
                    "Redis 的 RDB 和 AOF 重写流程分别是什么？",
                    RagEvaluationCaseType.NO_ANSWER,
                    List.of(), List.of(),
                    "知识库中没有足够可靠的资料"),
            evaluationCase("no-answer-spring-transaction",
                    "Spring 事务传播行为 REQUIRES_NEW 和 NESTED 有什么区别？",
                    RagEvaluationCaseType.NO_ANSWER,
                    List.of(), List.of(),
                    "知识库中没有足够可靠的资料"),
            evaluationCase("no-answer-kafka-rebalance",
                    "Kafka 消费组再均衡有哪些触发条件，如何减少停顿？",
                    RagEvaluationCaseType.NO_ANSWER,
                    List.of(), List.of(),
                    "知识库中没有足够可靠的资料"),
            evaluationCase("no-answer-completable-future-timeout",
                    "CompletableFuture 的 orTimeout 和 completeOnTimeout 有什么区别？",
                    RagEvaluationCaseType.NO_ANSWER,
                    List.of(), List.of(),
                    "知识库中没有足够可靠的资料"),
            evaluationCase("no-answer-innodb-gap-lock",
                    "InnoDB 的 next-key lock 和 gap lock 在什么情况下生效？",
                    RagEvaluationCaseType.NO_ANSWER,
                    List.of(), List.of(),
                    "知识库中没有足够可靠的资料"),
            evaluationCase("no-answer-g1-remembered-set",
                    "G1 的 Remembered Set 与写屏障如何配合？",
                    RagEvaluationCaseType.NO_ANSWER,
                    List.of(), List.of(),
                    "知识库中没有足够可靠的资料"),
            evaluationCase("no-answer-thread-pool-scheduling",
                    "线程池如何实现定时任务调度？",
                    RagEvaluationCaseType.NO_ANSWER,
                    List.of(), List.of(),
                    "知识库中没有足够可靠的资料"),
            evaluationCase("no-answer-fulltext-index",
                    "联合索引能否替代全文索引？",
                    RagEvaluationCaseType.NO_ANSWER,
                    List.of(), List.of(),
                    "知识库中没有足够可靠的资料"),
            evaluationCase("no-answer-gc-pause-tuning",
                    "垃圾收集器如何调优停顿时间？",
                    RagEvaluationCaseType.NO_ANSWER,
                    List.of(), List.of(),
                    "知识库中没有足够可靠的资料"),

            evaluationCase("java-cross-map-choice",
                    "单线程映射与高并发映射应如何选择，并分别关注哪些查询和竞争成本？",
                    RagEvaluationCaseType.CROSS_SECTION,
                    List.of(COLLECTIONS, CONCURRENT_COLLECTIONS),
                    List.of(path("Java 集合", "HashMap 的查询过程"), path("Java 并发集合", "ConcurrentHashMap 的并发控制")),
                    "HashMap 常见平均查询复杂度为 O(1)", "HashMap 不提供并发安全", "ConcurrentHashMap 降低并发竞争"),
            evaluationCase("mysql-cross-read-and-index",
                    "排查一条事务内查询既慢又看到历史版本时，需要分别从索引和 MVCC 哪些机制分析？",
                    RagEvaluationCaseType.CROSS_SECTION,
                    List.of(TRANSACTIONS, INDEXES),
                    List.of(path("MySQL 事务", "MVCC 一致性读取"), path("MySQL 索引", "联合索引与最左前缀")),
                    "Read View 决定版本可见性", "undo log 提供历史版本", "联合索引遵循最左前缀"),
            evaluationCase("rag-cross-retrieval-and-tools",
                    "知识库回答与可执行 Agent 的可信边界分别依赖哪些检索证据和工具治理？",
                    RagEvaluationCaseType.CROSS_SECTION,
                    List.of(RAG_BASICS, AGENT_BASICS),
                    List.of(path("RAG 检索基础", "来源引用"), path("Agent 与工具调用", "工具治理边界")),
                    "RAG 回答保留来源用于核验", "知识不足时明确拒答", "服务端校验工具参数", "高风险写操作需要人工确认")
    );

    public List<RagEvaluationCase> findAll() {
        return CASES;
    }

    private static RagEvaluationCase evaluationCase(String id, String question, RagEvaluationCaseType type,
                                                     List<String> expectedSources,
                                                     List<List<String>> expectedHeadingPaths,
                                                     String... expectedAnswerPoints) {
        return new RagEvaluationCase(id, question, type, expectedSources, expectedHeadingPaths,
                List.of(expectedAnswerPoints));
    }

    private static RagEvaluationCase evaluationCase(String id, String question, RagEvaluationCaseType type,
                                                     List<String> expectedSources,
                                                     List<List<String>> expectedHeadingPaths,
                                                     List<RagEvaluationChunkExpectation> expectedChunks,
                                                     List<RagEvaluationChunkExpectation> hardNegativeChunks,
                                                     String... expectedAnswerPoints) {
        return new RagEvaluationCase(id, question, type, expectedSources, expectedHeadingPaths,
                List.of(), List.of(expectedAnswerPoints), expectedChunks, hardNegativeChunks);
    }

    private static RagEvaluationChunkExpectation chunk(String source, int chunkOrder) {
        return new RagEvaluationChunkExpectation(source, chunkOrder);
    }

    private static List<String> path(String... headings) {
        return List.of(headings);
    }
}
