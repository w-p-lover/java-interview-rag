---
id: interview-guide-java-jmm-locks-aqs-v1
title: Java 内存模型、锁、AQS 与并发正确性
source: interview-guide/java/jmm-locks-and-aqs.md
upstream_repository: https://github.com/Snailclimb/interview-guide
upstream_commit: 969e2af8550f680688f5b50f0d895186a661a319
upstream_references: [app/src/main/resources/skills/_shared/references/java.md]
upstream_skill: app/src/main/resources/skills/java-backend/SKILL.md
license: AGPL-3.0
transformation: 基于上游并发知识点独立扩展为 JMM、锁和 AQS 课程，结合本项目调度去重、回答舱壁和状态 fencing；未复制上游原文、Prompt 或源码
skill: JAVA_CORE
usage: 知识库阅读、RAG 问答、JAVA_CORE 题库、专项面试与复习
---

# Java 内存模型、锁、AQS 与并发正确性

## 一、原子性、可见性与有序性分开回答

JMM 定义线程如何通过主内存交互。`volatile` 写与后续读建立 happens-before，提供可见性和特定重排序约束，但 `count++` 仍是读改写复合操作，不具备原子性。`synchronized` 的解锁与后续加锁、线程 start/join、传递性也形成 happens-before。

安全发布要求其他线程看到构造完成的状态：使用静态初始化、锁、volatile 引用、并发容器或 final 字段语义。把可变对象放进普通字段后再让线程竞态读取，即使“测试没出错”也不是证明。

## 二、synchronized、Lock 与 CAS

`synchronized` 提供互斥、可见性和自动释放，适合结构清晰的临界区；`ReentrantLock` 提供可中断获取、超时、公平选项和多个 Condition，但必须在 finally 解锁。公平锁减少插队但吞吐通常更低。

CAS 比较内存值并条件更新，适合低冲突的简单状态；高冲突会自旋浪费 CPU，复合状态还会遇到 ABA。版本戳能识别“变回原值”，但不能替代业务状态机。无锁不等于无竞争，也不等于总是更快。

## 三、AQS 的队列与状态

AQS 用一个 state 表示同步状态，并以 FIFO 等待队列管理竞争线程。独占模式可实现 ReentrantLock，共享模式可实现 Semaphore/CountDownLatch。获取失败的线程入队并可能 park，释放时唤醒合适后继；具体同步器决定 tryAcquire/tryRelease 语义。

Condition 有独立等待队列，await 会释放当前锁并等待 signal 后重新竞争同步队列。signal 不是把锁立即交给等待线程，因此条件必须在循环中重新检查，防止虚假唤醒和状态变化。

## 四、并发正确性落到不变量

选择锁前先写出不变量：例如同一任务只能调度一次、回答并发数不能超过 4、任务终态不能被旧 attempt 改写。进程内锁只能保护当前 JVM；多实例正确性必须依赖数据库条件更新、唯一约束或可被资源端校验的 fencing。

取消与执行存在竞态时，不应依赖“先看状态再做动作”。需要原子状态转换决定谁胜出，失败方读取最新状态并返回明确结果。中断只是协作信号，阻塞库和业务代码是否响应中断要单独验证。

## 五、本项目代码映射

`RagAnswerService` 使用 Semaphore 限制并发回答并在完成路径释放许可，属于 AQS 共享模式的舱壁；线程池队列继续限制等待量。`BankGenerationService` 使用 ConcurrentHashMap 去重调度、同步块保护排队取消复合操作，`volatile recoveryScan` 保证扫描句柄可见。

跨重启正确性不由 JVM 锁承担：题库任务心跳、保存和终态都校验数据库当前 attempt；旧 Worker 恢复后条件更新为 0。这是进程内并发与分布式 fencing 的职责分层。

## 六、练习题

### 主问题 1｜原理与实现

volatile 能解决哪些问题，为什么不能用它直接保证 `count++` 的线程安全？

参考回答：volatile 保证对该变量写入对后续读取可见，并约束相关重排序；`count++` 包含读取、加一、写回，多个线程会丢失更新。计数可用 AtomicLong/LongAdder，复合不变量用锁或原子状态机。选择还要看是否需要严格线性化读。

回答检查点：可见性、happens-before、复合操作、原子类、LongAdder 边界。

追问：LongAdder 为什么不适合直接表示严格余额？

追问参考：它分散热点后汇总，sum 不是与所有并发更新线性化的单点快照，适合统计而非强一致业务状态。

### 主问题 2｜并发与边界

回答服务如何同时限制运行中请求和等待请求，为什么只有 Semaphore 不够？

参考回答：Semaphore 限制进入模型调用的并发数，保护连接和 Provider；有界线程池队列限制尚未运行的等待量，拒绝策略提供过载反馈。只有 Semaphore 时，大量任务仍可能堆在线程池或调用方，增加内存和超时；只有队列时运行并发仍可能压垮下游。

回答检查点：运行中上限、等待上限、拒绝、超时、许可释放和指标。

追问：异常或取消路径漏掉 release 会发生什么？

追问参考：许可永久泄漏，系统可用并发逐步下降直至所有新请求无法进入，因此释放必须覆盖完成、异常和取消。

### 主问题 3｜结论与推理表达

为什么 JVM 内的 synchronized 不能阻止另一个实例的旧 Worker 覆盖任务结果？

参考回答：监视器只协调同一进程内共享对象，不覆盖其他 JVM、重启前线程或数据库。多实例任务应让权威存储保存当前 attempt/版本，所有写入使用条件更新；接管生成新 attempt，旧 attempt 返回 0。唯一键和幂等结果继续限制重复副作用。

回答检查点：锁作用域、权威状态、attempt 条件写、接管、唯一键和外部副作用。

追问：数据库条件更新成功后，是否代表所有外部副作用也只执行一次？

追问参考：不代表；HTTP、对象存储或模型调用可能已发生，需要各自幂等键、Outbox、对账或补偿。
