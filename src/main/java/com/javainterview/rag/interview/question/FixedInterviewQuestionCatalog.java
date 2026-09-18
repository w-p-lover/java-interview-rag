package com.javainterview.rag.interview.question;

import com.javainterview.rag.interview.preparation.InterviewFocusSource;

import java.util.List;

public class FixedInterviewQuestionCatalog {

    public List<InterviewQuestion> select(InterviewSkill skill,
                                          InterviewDifficulty difficulty,
                                          int count) {
        if (skill == null || skill == InterviewSkill.GENERAL) {
            throw new IllegalArgumentException("请选择具体的面试 Skill");
        }
        if (difficulty == null) {
            throw new IllegalArgumentException("请选择面试难度");
        }
        List<InterviewQuestion> candidates = switch (skill) {
            case JAVA_CORE -> javaCore(difficulty);
            case SPRING -> spring(difficulty);
            case DATABASE -> database(difficulty);
            case SYSTEM_DESIGN -> systemDesign(difficulty);
            case RAG_AI -> ragAi(difficulty);
            case GENERAL -> throw new IllegalArgumentException("请选择具体的面试 Skill");
        };
        if (count < 1 || count > candidates.size()) {
            throw new IllegalArgumentException("固定题数量超出当前 Skill 可用范围");
        }
        return candidates.subList(0, count);
    }

    private List<InterviewQuestion> javaCore(InterviewDifficulty difficulty) {
        return switch (difficulty) {
            case BASIC -> List.of(
                    question("HashMap 在 Java 8 中如何完成一次 put？", "集合结构与扩容基础", "Java HashMap put 扩容"),
                    question("synchronized 与 ReentrantLock 的主要差异是什么？", "并发同步基础", "synchronized ReentrantLock 区别"),
                    question("Java 异常体系中受检异常与非受检异常如何选择？", "异常设计基础", "Java checked unchecked exception"));
            case INTERMEDIATE -> List.of(
                    question("线上线程池任务持续堆积时，你会如何定位并调整参数？", "线程池容量与故障定位", "Java 线程池 队列 拒绝策略 排查"),
                    question("请说明 AQS 的状态、队列和唤醒流程如何协作。", "并发框架原理", "AQS state CLH 队列 唤醒"),
                    question("CompletableFuture 组合多个任务时如何处理线程池和异常传播？", "异步编排边界", "CompletableFuture 线程池 异常传播"));
            case ADVANCED -> List.of(
                    question("一次 Full GC 后延迟仍未恢复，你会如何区分内存、锁竞争和下游阻塞？", "JVM 与并发综合诊断", "Full GC 延迟 锁竞争 下游阻塞"),
                    question("如何设计一个可证明不会丢任务、也不会无限重复执行的进程内异步任务组件？", "并发状态机与幂等", "异步任务 状态机 幂等 恢复"),
                    question("类加载器泄漏导致 Metaspace 持续增长时，你会如何证明根因？", "类加载与内存诊断", "ClassLoader 泄漏 Metaspace 排查"));
        };
    }

    private List<InterviewQuestion> spring(InterviewDifficulty difficulty) {
        return switch (difficulty) {
            case BASIC -> List.of(
                    question("Spring Bean 从注册到完成初始化经历哪些关键阶段？", "Spring 容器基础", "Spring Bean 生命周期"),
                    question("Spring MVC 如何把一次 HTTP 请求分派到 Controller？", "Web 请求链路", "Spring MVC DispatcherServlet 请求流程"),
                    question("Spring 中构造器注入相比字段注入有什么优势？", "依赖注入基础", "Spring 构造器注入 字段注入"));
            case INTERMEDIATE -> List.of(
                    question("同类方法调用导致事务失效时，根因是什么，应该如何修复？", "AOP 代理与事务边界", "Spring 事务 同类调用 代理失效"),
                    question("如何设计一个既能重试又不会重复写业务数据的 Spring 服务？", "事务、重试与幂等", "Spring 重试 事务 幂等"),
                    question("Spring 事务传播行为在嵌套调用中如何影响提交和回滚？", "事务传播边界", "Spring 事务传播 回滚"));
            case ADVANCED -> List.of(
                    question("Spring 应用滚动发布期间如何避免旧实例继续消费已经迁移的任务？", "实例隔离与租约治理", "Spring 滚动发布 任务 fencing lease"),
                    question("如何定位一次只在高并发下出现的事务连接池耗尽问题？", "连接池与并发诊断", "Spring 事务 连接池耗尽 排查"),
                    question("多个切面同时作用于事务方法时，如何验证执行顺序和异常语义？", "AOP 链路诊断", "Spring AOP 切面 顺序 事务 异常"));
        };
    }

    private List<InterviewQuestion> database(InterviewDifficulty difficulty) {
        return switch (difficulty) {
            case BASIC -> List.of(
                    question("联合索引为什么需要遵循最左前缀原则？", "索引基础", "MySQL 联合索引 最左前缀"),
                    question("事务的四种隔离级别分别解决了什么问题？", "事务隔离基础", "MySQL 事务 隔离级别"),
                    question("聚簇索引和二级索引在查询路径上有什么差异？", "InnoDB 索引基础", "InnoDB 聚簇索引 二级索引 回表"));
            case INTERMEDIATE -> List.of(
                    question("一条 SQL 突然从毫秒变成秒级，你会按什么顺序排查？", "SQL 性能诊断", "MySQL 慢查询 执行计划 统计信息"),
                    question("如何实现数据库写入与消息发送的最终一致性？", "本地事务与消息一致性", "事务消息 outbox 最终一致性"),
                    question("分页越往后越慢时，如何改写查询并保证结果稳定？", "深分页优化", "MySQL 深分页 游标分页 稳定排序"));
            case ADVANCED -> List.of(
                    question("热点行更新导致锁等待时，如何在不破坏一致性的前提下降低竞争？", "锁竞争与数据建模", "MySQL 热点行 锁等待 分片"),
                    question("一次主从切换后出现读到旧数据，你会如何定义并实现读取一致性边界？", "复制延迟与一致性", "MySQL 主从切换 复制延迟 读一致性"),
                    question("在线 DDL 期间业务延迟升高时，如何判断是否应该继续或回滚？", "数据库变更治理", "MySQL online DDL 延迟 回滚"));
        };
    }

    private List<InterviewQuestion> systemDesign(InterviewDifficulty difficulty) {
        return switch (difficulty) {
            case BASIC -> List.of(
                    question("设计接口幂等时，幂等键应该由谁生成并保存在哪里？", "接口幂等基础", "接口幂等键 去重"),
                    question("缓存穿透、击穿和雪崩分别如何治理？", "缓存稳定性基础", "Redis 缓存穿透 击穿 雪崩"),
                    question("限流、熔断和降级分别应该放在什么位置？", "稳定性基础", "限流 熔断 降级 区别"));
            case INTERMEDIATE -> List.of(
                    question("如何设计一个支持取消、超时和失败恢复的异步任务系统？", "异步任务状态机", "异步任务 取消 超时 恢复"),
                    question("服务降级时如何保证用户看到的结果可解释且不会悄悄变错？", "降级语义与可观测性", "服务降级 语义 可观测性"),
                    question("如何为读多写少接口设计缓存更新与失效策略？", "缓存一致性取舍", "缓存 更新 失效 一致性"));
            case ADVANCED -> List.of(
                    question("跨实例任务如何使用 attempt fencing 防止旧 Worker 覆盖新结果？", "分布式任务并发控制", "attempt fencing Worker 任务恢复"),
                    question("如何为一个读多写少系统设计可回滚的数据与索引双写发布流程？", "多存储原子发布", "数据库 索引 双写 原子发布 回滚"),
                    question("跨区域部署中如何定义可接受的一致性、延迟和故障切换边界？", "多活架构取舍", "跨区域 多活 一致性 延迟 故障切换"));
        };
    }

    private List<InterviewQuestion> ragAi(InterviewDifficulty difficulty) {
        return switch (difficulty) {
            case BASIC -> List.of(
                    question("RAG 中切分、Embedding、召回和生成分别解决什么问题？", "RAG 基础链路", "RAG 切分 Embedding 召回 生成"),
                    question("为什么返回了知识来源仍不能说明回答一定正确？", "引用与正确性边界", "RAG 来源引用 回答正确性"),
                    question("向量相似度阈值过高或过低分别会带来什么问题？", "检索阈值基础", "向量检索 相似度阈值 召回"));
            case INTERMEDIATE -> List.of(
                    question("Dense、BM25 和 Hybrid 检索各自容易在哪些问题上失败？", "混合检索取舍", "Dense BM25 Hybrid 检索 对比"),
                    question("如何把检索失败、证据不足和回答未使用证据区分开？", "RAG 失败分层", "RAG 检索失败 证据不足 引用"),
                    question("如何用 Hard Negative 验证系统不会对知识库外问题强答？", "拒答评估", "RAG Hard Negative 拒答 评估"));
            case ADVANCED -> List.of(
                    question("如何设计一套能区分召回、排序、拒答、生成和引用质量的评估体系？", "RAG 评估治理", "RAG 评估 召回 排序 拒答 引用"),
                    question("知识索引重建时如何实现临时索引、契约校验和原子发布？", "索引生命周期与恢复", "RAG 临时索引 契约 原子发布"),
                    question("如何定位一个引用存在但 claim 与证据并不一致的回答？", "claim 级证据校验", "RAG claim 证据 一致性 校验"));
        };
    }

    private InterviewQuestion question(String text, String objective, String knowledgeQuery) {
        return new InterviewQuestion(
                text,
                objective,
                List.of(InterviewFocusSource.TARGET_ROLE),
                knowledgeQuery,
                false,
                List.of());
    }
}
