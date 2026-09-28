---
id: interview-guide-java-async-composition-context-virtual-threads-v1
title: CompletableFuture、异步上下文与虚拟线程
source: interview-guide/java/async-composition-context-virtual-threads.md
upstream_repository: https://github.com/Snailclimb/interview-guide
upstream_commit: 969e2af8550f680688f5b50f0d895186a661a319
upstream_reference: app/src/main/resources/skills/_shared/references/java.md
upstream_skill: app/src/main/resources/skills/java-backend/SKILL.md
license: AGPL-3.0
transformation: 基于上游 CompletableFuture、ThreadLocal 和虚拟线程知识点独立重写，结合本项目并行出题、RAG 超时、简历审计与请求日志形成案例；未复制上游原文、Prompt 或源码
usage: 知识库阅读、RAG 问答、JAVA_CORE 题库、专项面试与复习
---

# CompletableFuture、异步上下文与虚拟线程

## 学习目标

学完后应能完成四件事：解释 `CompletableFuture` 的执行线程与组合语义；区分异常转换、超时、取消和底层副作用；说明 MDC/ThreadLocal 为什么不会自然跨线程；判断 Java 21 虚拟线程能解决什么、不能解决什么。

面试回答不要停在 API 名称。先说明任务由谁执行、结果如何组合、失败如何传播、上下文如何传递，再说明容量和外部副作用边界。

## 一、CompletionStage 描述的是结果依赖图

`CompletableFuture` 同时实现 `Future` 和 `CompletionStage`。它既能代表一个尚未完成的结果，也能把后续计算组织成依赖图。

- `thenApply` 把一个正常结果同步映射为另一个值。
- `thenCompose` 把“返回 Future 的异步函数”摊平成一层，避免 `CompletableFuture<CompletableFuture<T>>`。
- `thenCombine` 等待两个独立结果后合并，适合两条都必须成功的分支。
- `allOf` 只表示所有阶段都完成，不直接收集结果；仍要从原 Future 读取各自结果。
- `anyOf` 在任一阶段完成时结束，但不会自动取消其他阶段。

没有 `Async` 后缀的回调通常由完成前一阶段的线程执行；带 `Async` 且未指定执行器时通常进入 `ForkJoinPool.commonPool()`。因此链式写法不等于固定线程模型。阻塞 HTTP、数据库或模型调用不应无意识占用公共池，生产代码应显式选择有界执行器，并把线程数、队列、拒绝和关闭纳入容量设计。

## 二、异常处理先区分“恢复”与“观察”

`join()` 在失败时抛出未检查的 `CompletionException`，`get()` 抛出受检查的 `ExecutionException`，两者都要继续查看 `cause` 才能得到真实业务异常。

- `exceptionally` 只处理异常，并把备用值变成正常结果。
- `handle` 同时接收正常结果或异常，可做统一转换。
- `whenComplete` 适合记录、释放资源等旁路动作；它通常不应把失败伪装成成功。

恢复策略必须保持业务语义。某个来源生成失败时，可以明确降级为另一个来源；知识检索或模型评分失败时，不能随意返回空列表或高分结果。`allOf` 中一个阶段失败会让组合结果失败，但其他已提交任务仍可能继续执行；组合 Future 的失败不是事务回滚。

## 三、超时和取消不等于底层工作停止

`orTimeout` 到期后以异常完成当前 Future；`completeOnTimeout` 到期后用备用值正常完成。两者改变的是等待者观察到的完成状态，不保证阻塞中的 HTTP、数据库、文件或模型调用立即终止。

`cancel(true)` 也只是尝试中断执行线程。任务必须检查中断或调用可中断 API，第三方客户端还要有自己的连接、读取和整体截止时间。已经发生的数据库写入、对象上传或 Provider 计费不会因为 Future 取消自动回滚。

可靠异步任务还需要领域状态：任务 ID、attempt ID、开始和心跳时间、截止时间、完成条件与幂等键。超时 Worker 的迟到结果必须被状态条件或 fencing 拒绝，不能只依赖内存 Future。

## 四、ThreadLocal 与 MDC 是线程绑定状态

`ThreadLocal` 的值保存在当前线程关联的 ThreadLocalMap 中。在线程池中，线程会被不同请求复用：如果设置后不在 `finally` 中清理，后续任务可能读到前一个请求的数据；Key 使用弱引用也不能消除 Value 长期滞留风险。

普通 `ThreadLocal` 不会自动传到异步线程。`InheritableThreadLocal` 只在线程创建时复制，无法正确覆盖线程池复用场景。MDC 通常也是 ThreadLocal 方案，因此 Web 请求线程中的 requestId 不会自然出现在 `CompletableFuture` 或执行器 Worker 日志里。

可选方案是把必要上下文作为方法参数显式传递，或在提交任务时捕获允许传播的最小快照，在 Worker 中设置并在 `finally` 清理。不要把安全身份、完整简历或大对象不加筛选地塞进 ThreadLocal；跨进程任务必须把必要事实显式持久化，ThreadLocal 无法承担恢复协议。

## 五、虚拟线程降低等待成本，不提高下游容量

Java 21 虚拟线程由 JVM 调度到少量 carrier 平台线程上。它适合大量“每请求一线程”的阻塞式 I/O 任务，可以降低平台线程栈和上下文切换成本，让同步代码保持直观。

它不让 CPU 计算变快，也不会增加数据库连接、Qdrant、Redis、HTTP Provider 或模型配额。即使使用虚拟线程，仍需连接池、Semaphore、限流、超时和有界业务并发。把大量虚拟线程同时放向只能接收 20 个请求的下游，只会更快形成等待和失败。

长时间持有 `synchronized` 监视器或执行 native 调用时，虚拟线程可能固定 carrier，降低伸缩性；应通过 JFR 和运行指标确认，而不是看到 `synchronized` 就机械重写。大量 ThreadLocal 也会按虚拟线程复制状态，增加内存和上下文治理成本。

本项目源码目标仍是 Java 17，因此当前不引入虚拟线程。面试中应把“理解 Java 21 能力”与“当前项目已采用”分开表述；只有升级运行与编译基线、完成容量对比且真实线程瓶颈存在时才考虑迁移。

## 六、本项目代码追踪

### 多来源并行出题

`InterviewQuestionGenerationService` 用两个 `CompletableFuture.supplyAsync` 并行生成简历题和方向题。它没有指定执行器，所以使用公共池；一条分支失败时会取消另一条并按来源降级。这里的 `cancel(true)` 不能证明模型请求已经停止，降级结果也不等于另一分支没有产生外部成本。

### RAG 回答截止时间

`RagAnswerService.callWithTimeout` 使用专用 `answerExecutor`，先通过 Semaphore 控制业务并发，再用 `Future.get(timeout)` 设置等待边界。`whenComplete` 负责释放许可，拒绝提交时也显式释放；超时后发送取消信号并返回稳定业务错误。专用执行器、舱壁和 Provider HTTP 超时必须共同存在。

### 简历证据审计

`ResumeEvidenceAuditService` 把模型调用放入专用 `auditExecutor`，等待超时后取消 Future，并把 `ExecutionException` 的 cause 恢复为真实运行异常。请求日志在成功和失败两条路径记录，但模型客户端仍需要自己的读取超时，不能只依靠等待方取消。

### 请求上下文

`BusinessRequestMetricsFilter` 在请求线程写入 MDC requestId，并在同步完成或异步监听回调中清理。项目没有把 MDC 自动复制到上述 `CompletableFuture` Worker，而是对需要持久关联的任务显式保存 requestId、taskId 和 attemptId。这比假设 ThreadLocal 能跨线程、跨重启更可靠。

## 七、面试回答模板

可以按下面顺序组织两分钟回答：

1. **线程**：每个阶段由当前线程、公共池还是专用执行器运行。
2. **组合**：顺序依赖用 `thenCompose`，独立分支用 `thenCombine/allOf`，并说明结果收集。
3. **失败**：区分恢复、观察和异常传播，不把部分失败伪装成全成功。
4. **截止**：等待超时和取消只是信号，外部调用还要自己的 deadline，迟到写入要 fencing。
5. **上下文**：ThreadLocal/MDC 不自动跨线程，优先显式传递最小事实并确保清理。
6. **虚拟线程**：它降低阻塞等待的线程成本，但不替代下游限流、连接池和幂等恢复。

## 八、练习题

### 主问题 1

`CompletableFuture.supplyAsync` 不传 Executor 有什么风险？`thenApply` 和 `thenApplyAsync` 的执行线程有什么差异？

参考回答：未指定执行器通常使用 commonPool，阻塞式模型或数据库调用会与其他公共任务争用；无 Async 的阶段通常由完成上一步的线程继续执行，Async 阶段会调度到默认或指定执行器。生产代码应按工作负载隔离有界执行器，并定义拒绝、超时、关闭和指标。

回答检查点：commonPool、阻塞污染、完成线程、Async 调度、专用有界执行器、拒绝和观测。

追问：`allOf` 中一个 Future 失败后，为什么不能认为另外两个任务已经停止或回滚？

### 主问题 2

RAG 模型调用等待 30 秒后执行 `future.cancel(true)`，为什么 Provider 仍可能继续处理并计费？怎样建立可靠截止边界？

参考回答：取消只尝试中断本地执行线程，SDK 或 socket 未必响应，远端请求已经离开进程；应同时设置客户端连接/读取/整体超时，限制并发和重试，给任务保存 attempt 与截止时间，并用条件更新拒绝迟到结果。是否计费由 Provider 接收和执行边界决定，不能由本地 Future 状态推断。

回答检查点：等待者超时、线程中断、远端副作用、HTTP deadline、并发上限、attempt fencing、迟到结果。

追问：`orTimeout` 与 `completeOnTimeout` 分别会让调用方观察到什么结果？

### 主问题 3

为什么请求线程 MDC 中的 requestId 在异步 Worker 里可能丢失或串请求？虚拟线程能否自动解决这个问题？

参考回答：MDC 通常基于 ThreadLocal，线程池 Worker 与请求线程不同且会复用；不清理会污染后续任务，InheritableThreadLocal 只在线程创建时复制。应显式传递 requestId，或捕获最小上下文后在 Worker 中设置并 finally 清理。虚拟线程仍有线程本地状态，不能自动跨异步边界或跨进程恢复，还可能放大 ThreadLocal 内存成本。

回答检查点：ThreadLocalMap、线程复用、finally 清理、显式上下文、虚拟线程、跨进程持久化。

追问：虚拟线程已经很轻量，为什么还需要数据库连接池和 Semaphore？

## 九、掌握标准

- 能画出两个异步分支从提交、组合、异常到降级的完整状态图。
- 能解释 `join/get`、`exceptionally/handle/whenComplete` 的不同职责。
- 能用本项目 RAG 回答链说明等待超时、HTTP 超时、取消和 attempt fencing 的边界。
- 能说明 MDC 在线程池中的丢失与污染风险，并给出最小上下文传播方案。
- 能准确表述虚拟线程适用场景，并明确本项目当前 Java 17 代码没有采用它。
