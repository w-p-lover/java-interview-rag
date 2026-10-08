---
id: interview-guide-project-executor-jvm-diagnostics-defense-v1
title: 并发执行器与 JVM 故障诊断项目答辩
source: interview-guide/project/executor-jvm-diagnostics-defense.md
upstream_repository: https://github.com/Snailclimb/interview-guide
upstream_commit: 969e2af8550f680688f5b50f0d895186a661a319
upstream_references:
  - app/src/main/resources/skills/java-backend/SKILL.md
  - app/src/main/resources/skills/_shared/references/java.md
license: 本地独立编写；上游为 AGPL-3.0，仅参考公开概念与提问结构，不复制正文、Prompt 或源码
transformation: 以本仓库资料任务真实代码讲解执行器和恢复边界，参考 blog-springboot 的线程池配置作只读对照
skill: JAVA_CORE
usage: 正式后端 Q5 的知识阅读、知识问答、独立题库专项训练与证据化再练
---

# 并发执行器与 JVM 故障诊断项目答辩

## 一、真实经历与能力边界

本项目的 `KnowledgeDocumentTaskExecutorConfiguration` 配置资料任务专用 `ThreadPoolTaskExecutor`：core=2、max=2、queueCapacity=8，线程名为 `knowledge-task-`。`KnowledgeDocumentTaskService` 先保存任务，再创建一个 `FutureTask` 并交给执行器；提交时注册独立超时调度，默认 600000ms，排队与执行共用这段预算。队列满时通过默认拒绝策略抛出异常，服务标记 `FAILED/TASK_REJECTED`，保留任务历史。

工作线程读持久文件，再调用导入或重建。终态使用本进程 completion guard 防止正常完成和超时重复记账；数据库更新携带 attemptId，索引发布再通过 generation/attempt 所有权校验阻止旧执行发布。排队任务可取消，运行中的任务不支持用户强制取消；超时会尝试中断。重启恢复依赖 fileId 和新 attempt，最多一次接管，并非无限重试。

已实现的是上述业务隔离、有限容量、终态与恢复链路。资料执行器没有配置 MDC/ThreadLocal 的 `TaskDecorator`，没有按真实负载得出的最佳线程数，也没有本轮测得的生产 OOM、线程耗尽事故。当前运行观测的 queued/running 是业务状态计数，不等于底层队列长度和活跃线程数；超时任务若不响应中断，业务 running 可以归零而线程仍被占用。面试必须区分现有实现、诊断方法和改进建议。

## 二、30 秒结论

> 我会先按工作性质隔离短请求、长资料任务和定时触发，不让长外部调用占满公共池。项目资料池是两线程、八槽位，满载明确拒绝；提交时开始统一超时，超时尝试中断，同时用 attempt 和索引发布所有权防止迟到结果覆盖。排障先对齐队列、真实活跃线程、任务状态和下游延迟，再用多次线程快照与 GC/堆外指标判断阻塞、死锁或内存保留，最后有控制地减载与恢复，不盲目扩大线程池或堆。

## 三、90 秒标准回答

> 隔离按资源和时延目标，而不是每个方法都新建线程池。短请求、外部 I/O 长任务和定时触发分别限容量；调度线程只触发，不长时间执行业务。线程数要受 CPU、数据库连接、模型配额和等待比例约束，队列受等待预算与单任务内存约束。我这里 core=max=2，所以不存在排队满后再扩到更大的 max；两线程和八队列满后拒绝。它是保护边界，不是经过压测的最优参数。
>
> ThreadPoolExecutor 一般先补 core，再入队，入队失败才尝试扩到 max，最后执行拒绝策略。无界队列通常使 max 难以生效，还会积压大对象。CallerRunsPolicy 可以把压力传给提交者，但也可能让 HTTP 或调度线程执行长任务，破坏快速返回语义；当前资料任务采用明确拒绝并保存失败状态。
>
> 任务标识显式传递；如果增加日志上下文，在提交时只复制必要字段，工作开始设置，finally 恢复之前的上下文，不能靠 InheritableThreadLocal 或忘记清理的 ThreadLocal。超时从提交开始，含排队；Future.cancel(true) 只是协作中断，外部 I/O 仍需连接/读取超时，不能保证立即释放线程。completion guard、数据库 attempt 条件和索引 generation fencing 各负责不同层级，不能用一个原子布尔变量宣称跨重启安全。
>
> 故障时先看真实 active、队列长度/最老等待、拒绝、下游耗时和任务状态。连续线程快照能区分锁竞争、同池等待子任务、网络阻塞与 CPU 热点。内存持续增长要看 GC 后存活堆和 RSS 是否一起增长：堆增长再检查队列闭包、ThreadLocal、缓存保留链，堆稳定而 RSS 增长则看线程栈、直接内存、元空间和原生分配。先暂停新任务、降低并发或隔离坏下游，再按有界恢复策略接管，不盲目重放。

## 四、3 分钟讲解：机制、取舍与恢复

### 1. 队列顺序、容量与拒绝

线程池接收顺序是 core → 尝试入队 → 入队失败后扩到 max → 拒绝；不是“先把 max 开满才排队”。本项目 core=max=2、queue=8，在工作线程都被占用且队列塞满的稳态下只能容纳约十个未完成任务，后续提交拒绝；竞态、任务取消和瞬时完成会改变当次结果。

有界队列限制任务数，不保证内存有界：一个 Runnable 若捕获整个上传正文、巨大对象图或上下文，即使只有八项也可能占用很多堆。当前任务以 source/fileId 为输入，正文由 worker 加载，降低排队正文滞留；解析、Chunk、Embedding 请求仍消耗运行内存，不能说 fileId 已解决所有 OOM。

稳态可用 L≈λW 估计系统内任务数，排队粗估 Wq≈Q×平均服务时间/并发数，但它们不能替代突发和长尾测量。如果耗时主要来自模型/数据库，提高线程数可能只是提高排队和超时，应与下游并发上限一起约束。CPU 密集线程接近可用核心数是起点，I/O 任务按等待比例估算也是起点；线程数增加会消耗线程栈、切换成本和连接资源。

拒绝策略的业务选择：AbortPolicy 提供可观察的拒绝；CallerRunsPolicy 在未关闭时由提交线程执行，shutdown 时可能丢弃，不是持久可靠队列；Discard/DiscardOldest 可能静默丢任务。对于“立即返回任务 ID”的长任务，不能盲目使用 CallerRunsPolicy。只读参考 `blog-springboot@46bfc235` 的 `ThreadPoolConfig` 给公共池、热门文章池和缓存刷新池配置 CallerRunsPolicy，这是一项真实配置，不证明所有调用链都适合它。

### 2. 同池等待、定时触发与任务隔离

两个工作线程都在执行父任务；每个父任务向同一个池提交子任务，然后同步 `get()`。子任务都排在队列里，父任务占着仅有的线程等待它们，CPU 可能很低，却没有进展。线程快照会看到两个线程等待 Future，队列增长；它不一定是 JVM monitor 死锁，线程工具可能不报告死锁。

改进优先取消同步等待或直接顺序执行；确有并行需求时使用独立且有界的资源池和组合式完成，仍须处理总截止时间。不能用无限加线程掩盖依赖循环，也不能把 `CompletableFuture` 默认公共池当作隔离。当前资料服务只创建一层 FutureTask 再 execute，不把同池 submit/get 套在 worker 内；这不代表所有其他业务链都自动安全。

定时线程应短时间触发业务，定时执行器通常使用无界延迟队列，maxPoolSize 与普通有界队列池的扩容语义不同。本项目超时调度使用独立单线程、取消后移除策略，负责状态超时和取消信号；数据库慢写也可能拖延这条调度线程。不能把注册了 timeout 描述成硬实时截止保证。

### 3. 上下文传递与 ThreadLocal 清理

线程池复用工作线程，不会随着每次请求结束自动清理 ThreadLocal。ThreadLocalMap 的 key 是弱引用，value 仍可能被存活线程强引用；弱 key 不等于 value 立即释放。即使 key 仍存在，忘记 remove 也会串请求、保留用户信息。

建议只复制 traceId/taskId 等必要字段，不捕获 ServletRequest、大正文、全部认证对象或密钥。在提交时快照，在执行前保存原上下文并设置，在 finally 恢复原值；如果使用 CallerRunsPolicy，任务可能在提交者线程执行，简单 clear 反而会清掉调用者原上下文。异步后续阶段要逐跳检查，数据库事务上下文不会因复制日志字段就跨线程延续。现有资料执行器未实现这套 decorator，应以建议表达。

### 4. 超时、中断与旧结果防护

本项目 schedule 在 execute 前设置超时，预算包含排队。超时路径写 `FAILED/TASK_TIMEOUT`，调用 `FutureTask.cancel(true)`，从队列移除尚未执行的 work；已运行任务只能接收中断。部分阻塞 I/O 不响应中断，外部客户端需自己的 connect/read timeout，计算循环需要主动检查中断。不能使用 Thread.stop 强杀，也不能靠 future 的 cancelled 状态判断线程已退出。

completion guard 只管当前进程内一次终态；数据库状态更新同时匹配 taskId/attemptId；索引暂存与发布检查 generation/attempt 防旧 worker 发布。超时或重启不能回滚已经写出的外部副作用，任何新增外部接口都要单独考虑幂等。重启时从持久文件恢复、旧 RUNNING 更换 attempt，一次上限用尽后失败；这与活着的进程主动扫描心跳不是一回事，当前资料任务没有存活进程心跳扫描。

### 5. 线程池耗尽的证据与恢复顺序

先按同一时段取请求量、完成量、真实 active/poolSize、queue.size、最老任务等待、reject/timeout 和下游 P95。业务 queued/running 与底层执行器指标分开：FAILED 但物理线程仍卡住时，“running=0”不能证明容量恢复。

在同一 PID 连续取几份线程快照，比较 `knowledge-task-` 栈和持有/等待资源。`RUNNABLE` 也可能在 native socket I/O，不能只看状态；`WAITING` 常是正常空闲或 Future 等待，`BLOCKED` 结合锁持有者判断竞争。高 CPU 热线程可结合 OS 线程 ID 与栈定位；反复卡在相同外部调用且队列上升时先查下游连接、超时和限额。

恢复先限制新提交、暂停非关键任务或隔离异常下游，再保存必要证据，修复阻塞原因。排队任务可取消，运行任务当前只有超时中断；确需重启时接受进行中调用中断的事实，并用已有 fileId/attempt 接管一次，核对索引一致性和任务状态。不要把失败任务全量重新提交，避免模型费用和副作用重复。

### 6. 堆增长、RSS 增长与 JVM 诊断

看 GC 后存活量、分配速率、GC 次数/暂停、堆使用、线程数与 RSS 的时间趋势。堆在 GC 后持续上升才更支持保留问题；短时间峰值也可能是合理分配或队列积压。GC 后堆基本稳定、RSS 持续涨，需要检查线程栈、DirectByteBuffer、Metaspace、代码缓存及 native 库，RSS 并不等于 Xmx。

建议由授权运维在故障实例使用 `jcmd <pid> Thread.print -l` 看线程，`jstat -gcutil <pid> 1000 5` 看 GC 趋势，`jcmd <pid> GC.heap_info` 看堆；JFR 可用有时长和体积上限的采集定位热点与分配。`VM.native_memory summary` 依赖启动时开启 NMT，不能假设现在随时可用。heap dump 可能暂停、占大量磁盘并包含个人数据，只在必要且授权后采集，检查 dominator/retained size/GC root 保留链；大 shallow size 不直接等于泄漏根因。

针对本链优先检查 Runnable 捕获、Chunk/Embedding 批次保留、队列和缓存、ThreadLocal 的 value、未关闭资源。先控任务量再调 GC/Xmx，防止加堆掩盖保留链或导致更长暂停。本轮提供诊断方案，没有执行 heap dump、JFR 或制造 OOM，不能将它们写成已完成线上治理。

## 五、上游概念到本地代码与入口

| 上游学习概念 | 本地资产或实现 | 必要差异 |
| --- | --- | --- |
| Java Skill 的经历、机制、边界与故障 | 本文三档答案；专项题库三维训练 | 独立编写题面/答案，不复制 AGPL Prompt |
| java.md 的线程池与拒绝 | KnowledgeDocumentTaskExecutorConfiguration、KnowledgeDocumentTaskService | 本地只按资料任务设 2/2/8，不动态调参，不照搬上游技术栈 |
| ThreadLocal 与异步编排 | 本文上下文和同池等待章节 | 本地未配 TaskDecorator，不声称跨线程上下文已实现 |
| JVM/OOM 与故障追问 | 本文线程快照、GC 后堆与 RSS 诊断 | 是方法训练，没有生产事故数据 |
| 8.3 Skill/Rubric 与证据训练 | QuestionBankService → STANDARD 题库专项会话 → InterviewReviewTaskService | 本轮使用三项分类和同版本证据；保留本地证据校验与复习历史 |
| 9.1 任务恢复与取消 | KnowledgeDocumentTaskService → fileId/attempt 条件写；MarkdownContentImporter → 索引发布 | 本地 Dense/Sparse 与 generation 防旧发布；资料任务当前只重启接管一次，没有存活心跳扫描 |
| 阅读、提问、本人练习 | `/` 资料目录 → `/answer` → `/question-bank` → `/interview` 与 `/review` | 代理示范会话与正式后端 8 题完全分开，示范分数不是本人能力 |

API 语义校准：Java 17 ThreadPoolExecutor、Future、ThreadLocal 官方文档：
https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/ThreadPoolExecutor.html
https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/concurrent/Future.html
https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/lang/ThreadLocal.html

## 六、固定训练题、答案要点与追问

主问题 A（原理与实现）：结合资料池 2/2/8 解释任务接收顺序、队列容量和拒绝策略；为什么 max 不一定能救积压，CallerRunsPolicy 不一定适合长任务？

参考答案：先 core、再队列、队列满再扩 max、最后拒绝；本池 core=max=2，稳态两工作加八排队；无界队列使 max 通常难生效。有界任务数仍需限制单任务载荷和下游并发。当前明确失败并留历史，CallerRuns 会占提交者线程，不保证持久可靠。

追问：把 queue 改成无界、max 改成 100，是否一定能避免 OOM 和长等待？检查点是排队顺序、单任务载荷、下游约束和拒绝可观察性。

主问题 B（边界与故障）：超时写 FAILED 后为什么线程仍可能被占用？如何传递与清理上下文，并防止超时/重启的旧 worker 发布结果？

参考答案：cancel(true) 是中断请求，不保证阻塞 I/O 停止；预算包含排队，外部调用有自己的超时。日志上下文提交快照、执行设置、finally 恢复原值，CallerRuns 时保护调用者上下文；不能透传事务。completion guard 只管当前进程，DB attempt 条件和索引 generation 防迟到写回，重启 fileId 接管仅一次，外部副作用仍需幂等。

追问：两个 worker 都 submit 子任务到同池再 get，为何 CPU 很低仍没有进展？检查点是同池依赖等待、真实 active 与业务计数区别，以及中断和旧结果防护。

主问题 C（表达与追问）：资料任务卡住且进程内存持续增长，请按证据、定位、减载、恢复说明步骤；如何区分堆保留和堆外/RSS 增长？

参考答案：对齐 active/queue/等待/拒绝/下游延迟，连续线程快照看等待资源，不只看线程状态；观察 GC 后存活堆与 RSS，堆涨查队列闭包、缓存、ThreadLocal，堆稳 RSS 涨查线程栈/直接内存/元空间。先控入口和下游再恢复一次，取 dump/JFR 要有必要性和授权，NMT 需预先开启，不能只加线程或 Xmx。

追问：业务 running 为零且 GC 后堆稳定，为什么还可能没有可用线程且 RSS 上涨？检查点是真实线程占用、取消非强制、堆外分类和有界恢复。

## 七、脱离材料后的掌握标准

- 30 秒讲清隔离、2/2/8、明确拒绝和超时非强杀。
- 90 秒讲对线程池接收顺序，解释 CallerRuns 的 HTTP/调度代价与上下文恢复。
- 3 分钟沿真实类讲到 fileId、completion guard、attempt、generation 的职责，并给出先证据后减载恢复的排障链。
- 不说“弱引用所以没有泄漏”“RUNNABLE 一定用 CPU”“FAILED 所以线程已停”“加 max/Xmx 就解决”。
- 关闭本文独立回答三个问题，回看示范遗漏，再以自己的话再练；不能把代理分数或诊断方案当作本人实战结果。
