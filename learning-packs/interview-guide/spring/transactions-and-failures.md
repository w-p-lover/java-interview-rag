---
id: interview-guide-spring-transactions-and-failures-v1
title: Spring 事务边界与失效场景
source: interview-guide/spring/transactions-and-failures.md
upstream_repository: https://github.com/Snailclimb/interview-guide
upstream_commit: 969e2af8550f680688f5b50f0d895186a661a319
upstream_reference: app/src/main/resources/skills/_shared/references/spring.md
upstream_skill: app/src/main/resources/skills/java-backend/SKILL.md
official_references:
  - https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html
  - https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/tx-propagation.html
license: AGPL-3.0
transformation: 基于上游事务知识点和追问顺序独立重写，用 Spring Framework 官方语义校准边界，并结合本项目真实代码形成学习案例；未复制上游原文、Prompt 或源码
usage: 知识库阅读、RAG 问答、SPRING 题库、专项面试与复习
---

# Spring 事务边界与失效场景

## 学习目标

学完后应能完成四件事：说清声明式事务如何通过代理建立边界；区分 `REQUIRED`、`REQUIRES_NEW` 和 `NESTED` 的物理事务语义；定位自调用、异常处理、方法可见性和异步边界造成的失效；用本项目题库生成链路说明为什么模型调用不应包在长数据库事务中。

面试回答不应只背七种传播行为。应先画出调用是否经过代理、当前是否已有事务、使用哪个事务管理器，再判断异常是否能到达拦截器以及最终提交或回滚哪个物理事务。

## 一、声明式事务实际做了什么

`@Transactional` 本身不会让方法具有事务。Spring 在初始化 Bean 时根据事务元数据创建代理；外部调用进入代理后，`TransactionInterceptor` 查找对应的 `PlatformTransactionManager`，按传播行为开启、加入或暂停事务，再调用目标方法，最后根据返回值和异常规则提交或回滚。

对 JDBC 事务，Spring 通常把连接等资源绑定到当前线程。这解释了两个重要边界：未经过代理的调用不会执行事务拦截器；新线程不会自动继承调用线程的事务上下文。

## 二、代理边界和自调用

同一个对象中的 `this.inner()` 是直接 Java 方法调用，不会重新经过 Spring 代理。因此，只在 `inner()` 上声明的新传播行为、隔离级别或回滚规则不会因自调用而生效。

稳妥的解法是把需要独立事务语义的方法移到另一个职责清晰的 Bean，由外部调用经过代理。自注入代理或 `AopContext.currentProxy()` 可以绕过问题，但会增加类对 Spring AOP 的耦合，通常不是首选。

### 方法可见性的版本差异

“`@Transactional` 标在非 `public` 方法上一定失效”过于绝对。基于接口的 JDK 动态代理只能拦截通过接口暴露的 `public` 方法；Spring 6 的类代理在受支持的场景下可以拦截受保护或包可见方法。但 `private` 方法不能被子类代理覆写，`final` 类或方法也会限制基于类的代理。

工程上仍建议把声明式事务放在清晰的 `public` 业务边界，这样既避免代理类型差异，也便于测试和代码审查。

## 三、回滚取决于逃出代理的异常

默认规则是对 `RuntimeException` 和 `Error` 回滚，对受检异常提交。需要对受检异常回滚时，应显式配置 `rollbackFor`；反向例外用 `noRollbackFor` 表达。不要为了“保险”统一写 `rollbackFor = Exception.class`，而应先确定业务异常是否代表整个事务必须撤销。

如果事务方法在内部 `catch` 住异常并正常返回，代理看不到失败，通常会尝试提交。正确方式是让需要回滚的异常继续抛出，或在确实必须转换为正常返回时显式标记当前事务 `rollbackOnly`。但标记回滚后再向外伪装成成功会造成语义冲突，应尽量返回真实失败。

### UnexpectedRollbackException

内层 `REQUIRED` 与外层共用同一物理事务。内层失败把事务标记为 rollback-only 后，即使外层捕获了异常并继续返回，最外层提交时仍会发现不能提交，Spring 会抛出 `UnexpectedRollbackException`。它不是 Spring 无故回滚，而是防止调用方误以为事务已成功。

## 四、七种传播行为应该按“有无现有事务”理解

- `REQUIRED`：有事务就加入，没有就新建。它是默认选择，多层逻辑通常共享一个物理事务。
- `REQUIRES_NEW`：总是使用独立物理事务；存在外层事务时先暂停它。内层可以独立提交或回滚，但通常还要占用另一个数据库连接。
- `NESTED`：在同一物理事务中建立保存点。内层可回滚到保存点，外层仍可继续；但最终是否提交仍由外层物理事务决定。它依赖事务管理器和资源的保存点支持，不是一个通用的“小事务”。
- `SUPPORTS`：有就加入，没有就以非事务方式运行。
- `MANDATORY`：必须已经存在事务，否则直接失败。
- `NOT_SUPPORTED`：存在事务时暂停，当前方法以非事务方式运行。
- `NEVER`：必须在没有事务的情况下运行，存在事务则失败。

### REQUIRES_NEW 与 NESTED 的关键差异

`REQUIRES_NEW` 是两个彼此独立的物理事务：内层提交后，外层回滚不会撤销已提交的内层结果。`NESTED` 是一个物理事务内的保存点：外层最终回滚时，内层所有结果仍会撤销。

`REQUIRES_NEW` 还有容量风险。外层持有一个连接时，每个并发线程进入内层新事务可能再等待一个连接。如果连接池只刚好等于外层并发数，所有线程都可能持有外层连接并等待新连接，形成容量僵局。使用时必须将额外连接占用纳入估算。

## 五、隔离级别不是越高越好

`isolation = DEFAULT` 表示使用底层数据库默认隔离级别。提高隔离级别可以减少某些并发异常，但可能增加锁等待、冲突和重试。真正的选择要结合数据库实现、SQL 形状、索引、锁范围和业务一致性。

乐观锁版本号、唯一索引和数据库条件更新不能被一个“更高隔离级别”简单取代。例如请求幂等和题库活动任务互斥依赖唯一索引与冲突恢复，而不是依赖长时间串行化整个业务流程。

## 六、异步、外部调用与事务不能混为一个原子操作

`@Async` 或线程池会切换线程，调用方的线程绑定事务不会自动传递。异步方法如果需要事务，应在异步线程中通过代理重新建立自己的边界，并单独处理调用方已经提交、回滚或取消的竞态。

数据库事务只能原子控制它所管理的数据库资源。它不能回滚已发送的 HTTP 请求、已消耗的 Chat/Embedding 调用、已写入的 Qdrant 向量或 Lucene 索引。这些跨资源流程需要状态机、幂等键、attempt fencing、Outbox 或明确补偿，不能只加一个 `@Transactional` 就宣称原子。

### 为什么不把慢调用放进长事务

模型、HTTP 或大文件解析可能持续数十秒。如果整段时间都持有数据库连接和锁，会放大连接池压力、锁等待、死锁概率和失败后的重试成本。更稳妥的结构是：短事务写入初始状态 -> 事务外调用慢服务 -> 短事务校验 attempt/版本并写入结果。

## 七、本项目代码追踪

### 题库生成：慢模型调用在事务外

`BankGenerationService` 先领取任务并更新 `PROCESSING` 状态，再在事务外检索资料和调用模型。模型返回每道候选题后，`MySqlQuestionBankStore.saveGenerated` 才用 `TransactionTemplate` 开启短事务：以版本条件更新任务成功数，再插入题目；重复题或失效 attempt 会回滚这次进度增量。

这个边界避免在付费模型调用期间占用数据库连接，同时保证“题目已落库”和“成功数加一”不会部分成功。它不保证模型调用与数据库原子；重复费用由 `requestId`、活动任务唯一键、attempt 和恢复次数上限控制。

### 简历版本：悲观锁、乐观锁和唯一键各司其职

`MySqlResumeCenterStore.saveVersion` 在事务内锁定简历记录，插入不可变版本，再用记录版本号更新当前版本指针。文件内容 Hash 的唯一键处理并发重复；乐观锁防止用过期页面状态覆盖新指针；行锁保护版本号计算。三者解决的不是同一个问题。

### 会话创建：声明式事务的清晰入口

`QuestionBankService.startSession` 是由 Controller 调用的 `public` 方法，使用 `@Transactional` 包含会话创建与题目使用记录。这是典型的外部代理边界：进入时已经完成题目选择所需的本地资料检查，没有在事务中调用 Chat 或 Embedding。

## 八、常见失效场景速查

| 现象 | 直接原因 | 检查方式 | 修复方向 |
| --- | --- | --- | --- |
| 同类内部调用没有新事务 | 调用未经过代理 | 核对调用对象和代理类型 | 拆到另一个 Bean 的公开业务边界 |
| 抛了受检异常仍提交 | 不在默认回滚规则中 | 核对异常类型与 `rollbackFor` | 按业务语义显式配置 |
| 方法内日志了异常却没回滚 | 异常被捕获后正常返回 | 查看异常是否逃出代理 | 重抛或在必要时标记 rollback-only |
| 异步任务不在原事务中 | 线程切换，上下文不自动传递 | 记录线程名和事务激活状态 | 异步线程内建立新事务或使用 Outbox/状态机 |
| 外层捕获异常后仍出现 `UnexpectedRollbackException` | 共享物理事务已被标记 rollback-only | 追踪内层 `REQUIRED` 异常 | 不伪装成功，重新设计异常或传播边界 |
| `REQUIRES_NEW` 高并发时卡住 | 外层持有连接，内层等待额外连接 | 观察连接池 active/pending/timeout | 估算额外连接、缩小并发或取消不必要的新事务 |

## 九、面试回答模板

可以按下面顺序组织两分钟回答：

1. **边界**：哪个 `public` 业务方法是事务入口，调用是否经过代理。
2. **资源**：当前事务管理的是哪个数据库，哪些 HTTP、模型或向量写入不在这个原子边界内。
3. **传播**：是加入现有物理事务、开新事务，还是使用保存点。
4. **回滚**：什么异常会逃出代理，是否有受检异常或被捕获的异常。
5. **并发**：唯一键、行锁、乐观锁和隔离级别分别保护什么。
6. **失败恢复**：对跨资源失败，使用状态、幂等、fencing 或补偿，而不是扩大数据库事务。

## 十、练习题

### 主问题 1

`@Transactional` 标在内部方法上，为什么同类自调用可能失效？如果事务方法捕获异常后正常返回，回滚又会发生什么变化？

回答检查点：外部代理调用、`this` 自调用绕过拦截器、默认回滚规则、异常被吞后代理看到正常返回、重抛或 rollback-only 的语义。

追问：为什么不建议为解决自调用而到处使用 `AopContext.currentProxy()`？

### 主问题 2

`REQUIRES_NEW` 和 `NESTED` 的提交、回滚和资源占用有什么差异？请说明外层最终回滚时两者的结果。

回答检查点：独立物理事务与保存点、外层暂停、内层独立提交、外层回滚对嵌套结果的影响、事务管理器支持、`REQUIRES_NEW` 额外连接占用。

追问：连接池大小等于外层并发数时，为什么所有线程都可能卡在内层新事务？

### 主问题 3

题库生成需要调用慢模型，同时更新任务进度并插入题目。为什么不应把整个流程放进一个 `@Transactional` 方法？本项目如何划分边界？

回答检查点：慢调用占用连接和锁、模型调用无法被数据库回滚、事务外调用模型、短事务原子保存题目和进度、`requestId`/attempt/唯一键控制重复与迟到结果。

追问：如果模型调用成功但题目落库失败，为什么不能声称费用也已回滚？

## 十一、掌握标准

- 能画出 Controller -> 事务代理 -> Service -> Mapper -> 提交/回滚的调用链。
- 能结合代理类型和 Spring 版本说明方法可见性边界，不背绝对化结论。
- 能用“独立物理事务”和“同一物理事务的保存点”准确区分 `REQUIRES_NEW` 与 `NESTED`。
- 能解释异常被捕获、受检异常、rollback-only 和 `UnexpectedRollbackException` 的关系。
- 能说明数据库事务不能覆盖 Chat/Embedding、HTTP、Qdrant 和 Lucene，并给出状态机、幂等、fencing 或补偿方案。
- 能在本项目定位 `QuestionBankService.startSession`、`BankGenerationService`、`MySqlQuestionBankStore.saveGenerated` 和 `MySqlResumeCenterStore.saveVersion` 的事务边界。
