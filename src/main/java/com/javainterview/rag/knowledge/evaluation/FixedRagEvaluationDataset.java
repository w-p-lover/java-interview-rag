package com.javainterview.rag.knowledge.evaluation;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FixedRagEvaluationDataset {

    private static final List<RagEvaluationCase> CASES = List.of(
            new RagEvaluationCase(
                    "java-collections-hashmap",
                    "HashMap 在什么条件下树化，平均查询复杂度是多少？",
                    List.of("java/collections.md"),
                    List.of("Java 8 的桶可由链表树化为红黑树", "链表长度达到 8", "数组容量至少为 64", "常见平均查询复杂度为 O(1)")
            ),
            new RagEvaluationCase(
                    "java-concurrency-thread-pool",
                    "ThreadPoolExecutor 接收到新任务后如何决定创建线程、入队和拒绝？",
                    List.of("java/concurrency.md"),
                    List.of("先创建核心线程", "核心线程满后任务进入工作队列", "队列满后在最大线程数内创建非核心线程", "超过最大线程数后执行拒绝策略")
            ),
            new RagEvaluationCase(
                    "java-concurrency-volatile",
                    "volatile 能保证什么，为什么不能保证自增操作的原子性？",
                    List.of("java/concurrency.md"),
                    List.of("保证可见性", "禁止特定指令重排序", "自增包含读取、计算和写回多个步骤", "多线程自增需要原子类或锁")
            ),
            new RagEvaluationCase(
                    "java-jvm-gc-roots",
                    "JVM 如何通过 GC Roots 判断对象是否可回收？",
                    List.of("java/jvm.md"),
                    List.of("从 GC Roots 开始进行可达性分析", "可达对象不会被回收", "不可达对象会进入回收判定", "栈帧局部变量和静态字段可以作为 GC Roots")
            ),
            new RagEvaluationCase(
                    "mysql-transaction-mvcc",
                    "MySQL 的 MVCC 如何让普通查询读取一致性快照？",
                    List.of("mysql/transactions.md"),
                    List.of("普通查询通常是快照读", "通过 Read View 判断可见性", "行记录保留事务版本信息", "undo log 支持读取历史版本")
            ),
            new RagEvaluationCase(
                    "rag-retrieval-basics",
                    "RAG 问答为什么需要保留来源、topK 和相似度阈值？",
                    List.of("rag/rag-basics.md"),
                    List.of("来源用于核验回答依据", "topK 控制进入上下文的召回数量", "相似度阈值过滤不可靠资料", "参数与来源可用于评估和排查")
            )
    );

    public List<RagEvaluationCase> findAll() {
        return CASES;
    }
}
