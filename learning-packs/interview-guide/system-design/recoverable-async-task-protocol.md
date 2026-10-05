---
id: interview-guide-recoverable-async-task-protocol-v1
title: 可恢复异步任务：心跳、租约、attempt fencing 与迟到结果隔离
source: interview-guide/system-design/recoverable-async-task-protocol.md
upstream_repository: https://github.com/Snailclimb/interview-guide
upstream_commit: 969e2af8550f680688f5b50f0d895186a661a319
upstream_references:
  - app/src/main/java/interview/guide/common/async/AsyncTaskRecoveryProperties.java
  - app/src/main/java/interview/guide/modules/knowledgebase/listener/VectorizeRecoveryScheduler.java
  - app/src/main/java/interview/guide/modules/knowledgebase/listener/QuestionGenerationRecoveryScheduler.java
  - app/src/main/resources/db/migration/V20260905__task_recovery_fields.sql
  - app/src/main/resources/db/migration/V20260906__task_attempt_fencing.sql
upstream_skill: app/src/main/resources/skills/system-design/SKILL.md
license: AGPL-3.0
transformation: 基于上游任务失联扫描、心跳、恢复次数与 attempt fencing 语义独立重写，结合本项目题库生成、资料索引和索引代际发布实现形成可追踪案例；未复制上游原文、Prompt、表结构或源码
skill: SYSTEM_DESIGN
usage: 知识库阅读、RAG 问答、SYSTEM_DESIGN 题库、专项面试与复习
---

# 可恢复异步任务：心跳、租约、attempt fencing 与迟到结果隔离

## 学习目标

学完后应能完成四件事：设计一条可查询、可恢复且不会被旧 Worker 覆盖的异步任务状态机；解释心跳、失联阈值和恢复次数各自解决什么问题；用数据库条件更新实现 attempt fencing；区分任务终态、业务副作用和多存储发布三个不同的一致性边界。

面试回答不要只说“加 MQ、加重试”。可靠性来自权威状态、领取协议、幂等副作用、恢复条件和迟到写隔离。消息中间件只改变投递与消费方式，不会自动提供这些业务语义。

## 一、先分清三个标识

一个可恢复任务通常至少需要三个不同标识：

- `requestId` 或业务幂等键代表“用户想完成哪一件事”。相同键和相同输入应复用同一个任务或结果；相同键却对应不同输入应拒绝。
- `taskId` 代表这条持久任务记录。重启、重新投递或 Worker 接管后仍保持不变，便于用户查询同一业务过程。
- `attemptId` 代表一次执行所有权。每次重新领取或恢复都生成新值，旧 attempt 后续提交的心跳、成功和失败必须被拒绝。

只使用 `taskId` 无法区分新旧 Worker。只使用随机消息 ID 又无法把多次投递收敛到同一个业务任务。三者混用会造成重复任务、状态倒退或旧结果覆盖新结果。

## 二、状态机要同时保护领取和终态

最小状态可以是 `QUEUED -> RUNNING -> SUCCEEDED/FAILED/CANCELLED`。关键不在枚举数量，而在每次迁移都要带前置条件：

```sql
UPDATE task
SET status = 'RUNNING', attempt_id = :newAttempt, worker_id = :worker
WHERE task_id = :taskId AND status = 'QUEUED';

UPDATE task
SET status = 'SUCCEEDED', result_ref = :result
WHERE task_id = :taskId
  AND status = 'RUNNING'
  AND attempt_id = :attempt;
```

第一条更新影响一行才算领取成功。第二条同时校验状态和 attempt，才能阻止被回收的旧 Worker 写入成功。失败、进度和心跳也应遵守相同所有权条件，不能只在成功路径做 fencing。

数据库事务只能保护本库中的状态与业务事实。若 Worker 已经调用外部模型、发送邮件或写入对象存储，状态更新失败并不会撤销这些副作用；外部动作还要有幂等键、可核对结果或补偿策略。

## 三、心跳是故障怀疑，不是锁

Worker 领取任务后周期性更新 `lastHeartbeatAt`。扫描器把“状态仍为 RUNNING 且心跳早于阈值”的记录视为疑似失联，再通过条件更新接管。心跳的作用是缩短恢复时间并提供活性信号，它不是互斥锁，也不能证明旧 Worker 已经停止。

阈值必须大于正常的排队等待、单次外部调用和心跳抖动。阈值太短会在 Full GC、网络抖动或慢模型调用时误接管；太长会放大 RTO。合理关系通常是：

1. Provider 连接、读取或整体超时小于失联阈值。
2. 心跳周期显著小于失联阈值，允许丢失少量心跳。
3. 扫描批量和频率受限，避免恢复风暴压垮下游。
4. 恢复次数有上限，耗尽后进入明确失败态并等待人工处理。

若业务阶段能够报告真实进度，心跳可以由“成功推进一个阶段”顺带更新；若模型调用会长时间阻塞，则需要独立调度心跳。但独立心跳也可能在业务线程已卡死时继续存活，因此还要有任务总截止时间和下游超时，不能只靠心跳判断健康。

## 四、恢复协议同时需要 safety 与 liveness

`liveness` 关注任务最终能继续前进：丢失投递要补投、进程重启要重新排队、失联 Worker 要被接管。`safety` 关注任何时刻都不能产生错误结果：重复消息不重复生成业务事实、旧 Worker 不能覆盖新 Worker、资料版本变化后不能发布旧内容。

只做自动重试解决不了 safety；只做 fencing 又可能让任务永远停在 RUNNING。完整协议通常包含：

- 持久化输入引用与运行契约，使新 Worker 能重建执行上下文。
- 原子领取，确保同一时刻只有一个当前 attempt。
- 心跳、截止时间、失联扫描与有上限恢复，保证活性。
- 业务唯一键、逐项幂等保存或可重放步骤，控制重复副作用。
- 所有状态写和发布动作校验当前 attempt，保证安全。
- 失败分类区分可恢复、契约变化、输入缺失和恢复耗尽。

这比承诺“exactly once”更诚实。常见系统只能提供至少一次投递，再依靠幂等与 fencing 得到“业务结果至多生效一次”的效果。

## 五、任务成功不等于多存储已经发布

资料索引任务会写 MySQL 元数据、Qdrant Dense 和 Lucene Sparse。即使任务表成功写为 `SUCCEEDED`，若两路索引不是同一个完整快照，检索仍可能读到混合版本。因此任务执行所有权和索引发布所有权要分层：

1. `taskId + attemptId` 决定哪个 Worker 可以推进任务。
2. `generationId` 标识本次构建出的 Dense/Sparse 快照。
3. 两路分别写临时代并核对 Chunk 数与版本。
4. 发布指针通过 `buildingGeneration + attemptId` 条件更新切换。
5. 任一路失败保持旧 `publishedGeneration` 可读，并清理或巡检临时代。

attempt fencing 防止旧 Worker 发布，generation 隔离防止半成品被查询。两者作用不同，不能用一个字段模糊代替。

## 六、本项目代码追踪

### 题库生成：完整的心跳与失联接管

`BankGenerationService` 为 `PROCESSING` attempt 启动周期心跳。`scanStaleTasks` 扫描未完成任务：`QUEUED` 任务重新调度，心跳过期的 `PROCESSING` 任务按恢复上限生成新 attempt 或写入 `RECOVERY_EXHAUSTED`。`QuestionBankStore.recoverStale` 同时校验任务版本、旧 attempt 和失联时间，避免两个扫描器重复接管。

生成过程中每保存一道题前都重新读取当前任务并比较 attempt；终态更新也携带 attempt。即使旧模型调用迟到返回，它最多浪费一次外部调用，不能继续把旧结果写成当前任务结果。

### 资料任务：重启恢复与本地截止时间

`KnowledgeDocumentTaskService` 把 `fileId` 持久化为可恢复输入。应用重启时，遗留 `RUNNING` 任务通过 `requeueAfterRestart` 把旧 attempt 条件替换为新 attempt，恢复次数上限当前为一次；没有文件引用的旧任务明确失败，不伪造可恢复。

单实例运行期还用有界线程池、`FutureTask`、截止调度和 completion guard 处理队列拒绝、超时与取消。completion guard 防止本进程中的正常完成、异常和超时重复计数；真正跨重启的迟到结果隔离仍由任务表的 attempt 条件更新承担。当前资料任务没有周期心跳和运行中自动失联扫描，这与题库任务的恢复能力不同。

### 索引构建：attempt 与 generation 双重 fencing

`MarkdownContentImporter` 把任务 attempt 传给索引状态；`KnowledgeIndexingStateMapper` 发布时要求当前 `building_generation` 与 `attempt_id` 同时匹配。Dense 与 Sparse 先写同一 generation，完成核对后才切换 `published_generation`。因此旧 Worker 即使继续完成临时写入，也不能把过时 generation 设为检索入口。

### 评估运行：逐样本恢复而不是整批重跑

检索、回答和评分稳定性实验都持久化运行契约与逐样本结果。重启后只补未成功样本，同契约重复启动复用已有运行；契约变化则不能把旧结果混进新运行。这是相同恢复原则在批处理上的另一种实现：恢复粒度是样本，fencing 依靠运行契约和状态条件，而不是假设整批调用具有事务性。

## 七、上游与本地的必要差异

上游固定 commit 使用 Redis Stream 承担持久投递和消费者组，分别扫描长时间 `PENDING` 与无进展 `PROCESSING`，通过恢复计数控制自动补投，并为向量化与简历分析补充 attempt 字段。这个设计适合消息已经成为执行通道、需要处理投递丢失和消费者死亡的场景。

本地当前仍是单 storage 实例的有界执行器，MySQL 是任务事实源；题库任务实现心跳扫描，资料任务只在应用重启时接管，尚无跨进程 Worker，因此没有为了形式一致引入 Redis Stream。若未来出现独立 Worker、多个实例竞争或必须保留 Pending 消息，才需要把执行通道升级为 Stream/MQ，同时保留现有 task/attempt/幂等协议，而不是用消息系统替换它们。

## 八、面试回答模板

可以按下面顺序组织三分钟回答：

1. **语义**：先定义 requestId、taskId、attemptId，说明系统提供至少一次执行还是业务结果一次生效。
2. **状态**：给出状态机与原子领取条件，所有心跳、进度和终态更新都校验 attempt。
3. **恢复**：说明心跳周期、失联阈值、总截止时间、扫描批量和恢复上限。
4. **副作用**：区分任务状态与数据库、模型、对象存储等外部副作用，使用幂等键、唯一约束和补偿。
5. **发布**：多存储构建用 generation 隔离半成品，只有完整校验后切换发布指针。
6. **取舍**：当前单实例执行器足够时不提前引入 MQ；跨进程执行成立后再升级通道，业务 fencing 协议保持不变。

## 九、练习题

### 主问题 1：方案可行性

请设计一套支持应用重启和 Worker 接管的异步任务协议，并说明为什么 `taskId` 与 `attemptId` 不能合并。

参考回答：业务请求先用 requestId 幂等创建稳定 taskId；每次执行或恢复生成新的 attemptId。Worker 通过 `taskId + QUEUED` 条件原子领取，心跳、进度、成功和失败都要求数据库当前 attempt 相等。重启或失联扫描只在旧 attempt 与阈值仍匹配时换新 attempt 并重新排队。taskId 让用户持续查询同一任务，attemptId 隔离不同执行所有者；合并后无法在保持任务身份不变的同时拒绝旧 Worker 迟到写。

回答检查点：requestId、稳定 taskId、执行 attempt、原子领取、条件终态、持久输入、恢复上限、迟到隔离。

追问：如果消息被重复投递两次，为什么原子领取成功一次仍不足以保证外部副作用只发生一次？

### 主问题 2：容量、一致性与恢复

如何设置心跳、失联阈值和任务超时，避免慢调用被误判，同时让故障能及时恢复？

参考回答：先取得真实耗时分布和 Provider 最坏超时；心跳周期应明显小于失联阈值，失联阈值要覆盖正常排队、单次调用和短暂抖动，Provider 整体截止时间应早于失联阈值。扫描器限制周期和批量，恢复使用退避和次数上限，避免故障时集中补投。独立心跳不能代替业务截止时间，因为业务线程卡死时心跳线程仍可能存活；接管后还必须换 attempt，并用条件写拒绝旧 Worker。

回答检查点：耗时证据、心跳周期、失联阈值、Provider deadline、扫描批量、恢复上限、恢复风暴、attempt fencing。

追问：进程发生长时间 Stop-The-World 后恢复，旧 Worker 和新 Worker 同时运行时，哪一层必须拒绝旧结果？

### 主问题 3：取舍表达

为什么本项目现在不把所有异步任务改成 Redis Stream？什么时候才值得升级？

参考回答：当前只有单 storage 实例，有界执行器加 MySQL 任务事实源已经覆盖查询、重启恢复、失败终态和 attempt 条件写；改成 Stream 会增加消费者组、Pending、claim、监控和运维成本，但不会自动解决幂等、外部副作用和旧 Worker 覆盖。出现跨进程 Worker、多实例竞争、进程退出后仍需持久 Pending、吞吐要求超过单实例队列或需要独立扩缩容时再升级执行通道。升级后仍保留 taskId、attemptId、业务唯一键、恢复上限和 generation 发布协议。

回答检查点：当前约束、已有能力、Stream 收益、运维代价、明确进入条件、协议不变部分、没有伪造现状。

追问：Redis Stream 的 Pending Entries List 能否替代数据库中的业务任务状态？为什么？

## 十、掌握标准

- 能画出 `QUEUED -> RUNNING -> 终态` 与失联接管产生新 attempt 的时序图。
- 能写出领取、心跳、完成和恢复的关键条件更新，而不只说“加分布式锁”。
- 能解释心跳、超时、中断、恢复和 fencing 的职责差异。
- 能沿本地题库任务、资料任务和索引 generation 指出各自已经实现与尚未实现的边界。
- 能说明何时使用 Redis Stream/MQ，并明确消息通道不能替代业务幂等与发布协议。
