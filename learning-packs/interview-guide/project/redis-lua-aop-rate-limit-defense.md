---
id: interview-guide-project-redis-lua-aop-rate-limit-defense-v1
title: Redis Lua 与 AOP 滑动窗口限流项目答辩
source: interview-guide/project/redis-lua-aop-rate-limit-defense.md
upstream_repository: https://github.com/Snailclimb/interview-guide
upstream_commit: 969e2af8550f680688f5b50f0d895186a661a319
upstream_references:
  - app/src/main/resources/skills/_shared/references/redis.md
  - app/src/main/resources/skills/java-backend/SKILL.md
license: AGPL-3.0
transformation: 基于上游 Redis Lua 原子性与 Java 后端项目追问顺序独立组织，结合只读参考项目 blog-springboot@46bfc235 的真实 RateLimit、RateLimitAspect 和 rate_limit.lua 实现形成答辩材料；未复制上游原文、Prompt 或源码
skill: SYSTEM_DESIGN
usage: 知识库阅读、RAG 问答、项目专项面试与复习
---

# Redis Lua 与 AOP 滑动窗口限流项目答辩

## 一、先明确这段经历能证明什么

这段实现位于只读参考项目 `blog-springboot@46bfc235`，真实调用链是：

```text
业务方法上的 @RateLimit
-> RateLimitAspect 读取 key、limit、period
-> SpEL 将方法参数写入限流 key
-> RedisTemplate 执行 rate_limit.lua
-> Lua 清理窗口外记录、计数并决定放行或拒绝
-> 放行后才执行原业务方法
```

当前有两个实际入口：

- 文章详情使用 `api:article:view:{articleId}`，60 秒最多 10 次。
- AI 写作使用 `api:ai:write:{action}`，3600 秒最多 20 次。

它能证明注解抽象、AOP 横切治理、SpEL 动态 key、Redis ZSet 和 Lua 原子判断已经落地。它不能证明已经完成按用户/IP 的公平限流、Redis 故障分级、集群压测、限流指标告警或生产容量验证。

## 二、30 秒结论

> 我把限流做成方法级注解，注解保存 key 模板、窗口和阈值，AOP 在业务执行前用方法参数解析 SpEL key，再调用 Redis Lua 脚本。脚本在一次原子执行中删除窗口外时间点、统计当前数量并决定是否写入本次请求，所以不会出现应用侧“先查数量再写入”的竞态。当前文章和 AI 入口按资源维度限流，还是全局 key，不是按用户或 IP 隔离；Redis 异常策略和高并发下的唯一 member 也需要继续完善。

## 三、90 秒标准回答

> 我先把可变规则放进 `@RateLimit`：`key` 支持 SpEL，`limit` 表示窗口内最大次数，`period` 表示秒级窗口。`RateLimitAspect` 使用 `@Around` 拦截注解方法，从 `MethodSignature` 取得参数名和值，用模板表达式把 `articleId` 或 `action` 拼进 key，然后通过 `RedisTemplate.execute` 调用类路径下的 Lua 脚本。
>
> Lua 使用 ZSet。score 是请求时间戳，执行时先用 `ZREMRANGEBYSCORE` 删除 `now - period` 之前的成员，再用 `ZCARD` 得到窗口内数量。数量小于阈值时执行 `ZADD` 并设置过期时间，返回 1；否则返回 0。清理、计数和写入都在一个脚本中执行，Redis 对脚本按命令执行边界保持原子性，因此并发请求不能在计数和写入之间插入另一套判断。
>
> 当前 key 的语义必须如实说明：`api:article:view:{articleId}` 是所有访问者共享的单篇文章额度，`api:ai:write:{action}` 是同一写作动作的共享额度。如果目标是限制单个用户，应把可信的 `userId` 加入 key；匿名接口可以使用经过代理信任边界处理后的客户端标识。不能直接声称当前已经按用户或 IP 限流。
>
> 当前实现还有两个明确边界。第一，ZSet member 直接使用毫秒时间戳，同一 key 在同一毫秒的多个请求会覆盖，可能少计数；生产改进应使用 `timestamp:requestId` 作为唯一 member，score 仍使用时间戳。第二，Redis 调用异常没有显式捕获，会中断请求，而返回 `null` 又会继续执行业务，故障语义不统一。应按接口成本和风险明确 fail-open 或 fail-closed，并补充 BYPASS、REJECT、ERROR、耗时和 key 维度基数指标。

## 四、3 分钟完整回答

### 1. 限流对象与 key 设计

设计 key 前先回答“保护谁、限制谁、按什么资源隔离”：

```text
rate-limit:{environment}:{route}:{resource}:{subject}
```

- `route/resource` 防止不同业务共享额度，例如文章详情按 `articleId`，AI 写作按 `action`。
- `subject` 决定公平性，可以是登录用户、租户、API 客户端或可信匿名标识。
- 环境或应用前缀避免测试与正式数据、不同服务之间相互污染。
- subject 必须有稳定来源和基数上限，不能把任意正文直接拼进 key。

真实代码只包含 route 与 resource，没有 subject；因此它保护的是单个资源的整体下游容量，而不是限制某个用户。面试时应先说真实语义，再说明如果业务目标改变会怎样扩展。

### 2. 为什么用 ZSet 和 Lua

固定窗口计数器实现简单，但窗口边界可能在很短时间内通过接近两倍额度。ZSet 滑动窗口保存最近一个周期内的请求时间点，能在任意时刻统计连续窗口。

单次脚本依次完成：

1. `ZREMRANGEBYSCORE key 0 windowStart` 清理窗口外成员。
2. `ZCARD key` 统计仍在窗口中的请求。
3. 未满时 `ZADD key now member` 记录本次请求。
4. `EXPIRE key period` 回收不再访问的 key。

Lua 解决的是同一 Redis 节点上这组命令的原子执行，不是跨 Redis、数据库和业务方法的事务。脚本返回允许后，业务方法仍可能失败；因此限流记录代表“获得执行资格”，不代表业务成功。

### 3. 当前实现的正确性边界

#### 同毫秒 member 覆盖

当前脚本的 score 和 member 都是 `now`。ZSet member 唯一，所以同一个 key 在同一毫秒到达的请求会更新同一 member，而不是新增多条记录。改进方式是由应用传入一次请求唯一标识，让 member 使用 `now:requestId`，score 继续使用 `now`。若只随机追加后缀，也要保证碰撞概率和长度可控。

#### 应用时钟

当前时间来自 `System.currentTimeMillis()`。多实例时钟偏差会改变清理边界和 score 顺序。第一版可以依赖主机时钟同步并监控偏差；更严格时要统一时间来源并验证 Redis 版本、复制和脚本约束，不能只说“换 Redis TIME”就结束设计。

#### SpEL 参数与 key 基数

SpEL 解析依赖运行时能取得方法参数名。构建配置、代理方法和参数名元数据变化都可能让变量解析失败，应有启动期或测试验证。表达式来自代码注解而非用户输入，降低了表达式注入风险，但解析出的值仍需要长度、字符和基数约束。

#### 过期与成本

成功写入时刷新 TTL，停止访问后 key 会被回收。每次请求仍需要清理和维护 ZSet，时间复杂度大致包含 `ZREMRANGEBYSCORE O(log N + M)`、`ZCARD O(1)`、`ZADD O(log N)`，空间与窗口内已记录请求数相关。高 QPS、长窗口或高基数 subject 会放大内存和脚本执行时间；此时令牌桶、分桶计数、网关限流或本地加全局两级方案可能更合适。

### 4. Redis 故障时怎么选

故障策略不能用一个答案覆盖所有接口：

| 场景 | 推荐倾向 | 原因 |
| --- | --- | --- |
| 公开文章读取 | 有界 fail-open 或本地降级 | Redis 故障时仍希望保留基本可用性，但必须有下游容量保护 |
| 付费 AI 写作 | fail-closed 或极小本地额度 | 放开可能造成重复费用和 Provider 过载 |
| 登录、权限、资金等安全入口 | fail-closed | 不能因限流组件故障扩大安全风险 |

当前 `RateLimitAspect` 没有捕获 Redis 异常，执行异常会阻断业务；但 `execute` 返回 `null` 时又会放行。面试中应该把它表述为“第一版尚未统一的边界”，而不是声称已经实现完整降级。

### 5. 怎样观测和恢复

至少需要以下低基数指标：

- `allowed / rejected / bypass / error` 次数，按接口和策略分类。
- Lua 执行耗时、Redis 超时和连接错误。
- 当前窗口计数与阈值比例，但避免把原始用户 ID 作为指标标签。
- 被保护下游的并发、失败、P95/P99 和费用，判断限流是否真正保护了系统。

Redis 恢复后不能只看连接成功，还要观察错误率、脚本耗时、拒绝比例和下游负载是否回落。当前参考项目没有专用限流指标和直接测试，这也是不能包装成生产级闭环的原因。

## 五、从问题追到真实代码

| 面试概念 | 只读参考项目入口 | 当前事实 |
| --- | --- | --- |
| 注解契约 | `com.ican.annotation.RateLimit` | 保存 key、limit、period 和拒绝提示 |
| AOP 编排 | `com.ican.aspect.RateLimitAspect` | 解析 SpEL、执行脚本、拒绝时抛业务异常 |
| 原子窗口 | `src/main/resources/lua/rate_limit.lua` | 清理、计数、写入和过期在一次脚本中执行 |
| 文章入口 | `ArticleServiceImpl#getArticleHomeById` | 按 articleId 全局限制 10 次/60 秒 |
| AI 入口 | `AiController#writeAssist` | 按 action 全局限制 20 次/3600 秒 |

不要把 `blog-springboot` 的代码说成当前 `java-interview-rag` 的运行实现。本学习包只把真实项目经历接入本项目的知识检索和训练链路。

## 六、正式题目回答骨架

题目：

> 请基于你使用 Redis Lua + AOP 实现滑动窗口限流的经历，说明限流对象和窗口键如何设计，Lua 脚本如何保证滑动窗口统计的原子性？当 Redis 不可用或脚本执行异常时，你会如何处理限流逻辑？

按下面六步回答：

1. 先说真实入口和业务目标：文章详情保护热点读取，AI 写作保护高成本调用。
2. 说明注解与 AOP：规则声明、SpEL key、业务执行前检查。
3. 说明 key 的真实维度：当前按资源全局限制，没有假装按用户/IP。
4. 说明 Lua 四步与原子性：清理、计数、写入、过期一次执行。
5. 主动指出两个现状边界：同毫秒 member 覆盖、异常与 null 的故障语义不统一。
6. 给出按业务分级的失败策略、唯一 member、指标和容量演进。

## 七、练习与评分标准

### 第一次作答：故意只讲实现

只回答“注解 + AOP + ZSet + Lua 原子执行”，不要看后面的检查点。这样可以暴露自己是否会主动说明限流对象、key 公平性、Redis 故障和现有缺口。

### 回答检查点

- 能说出文章与 AI 两个真实入口以及 key、阈值、窗口。
- 能区分“资源全局限流”和“按用户/IP 限流”。
- 能完整解释清理、计数、写入和过期为什么不能拆到应用侧。
- 能指出 Lua 原子性不覆盖后续业务方法。
- 能识别毫秒时间戳同时作为 member 的覆盖风险。
- 能按公开读取、付费 AI 和安全入口选择不同失败策略。
- 能提出 allowed/rejected/bypass/error、耗时和下游负载指标。

### 固定追问 1

同一毫秒内有 20 个请求，它们的 ZSet score 和 member 都使用毫秒时间戳，会发生什么？怎样修正而不改变按时间范围查询？

参考要点：member 唯一导致覆盖和少计数；score 保留时间戳，member 改为 `timestamp:requestId`；同时考虑时钟来源、长度和碰撞。

### 固定追问 2

为什么文章读取和 AI 写作不能使用完全相同的 Redis 故障策略？

参考要点：可用性、费用、安全和下游容量不同；文章可有界放行或本地降级，付费 AI 倾向拒绝或极小额度；任何 fail-open 都要有下游舱壁，任何 fail-closed 都要有明确错误和告警。

### 固定追问 3

如果加入 `userId` 后 key 数量快速增长，你如何控制 Redis 内存和指标基数？

参考要点：TTL、subject 合法性、窗口与额度、活跃 key 估算、避免高基数监控标签、采样与聚合、必要时换分桶/令牌桶/网关方案。

## 八、掌握标准

关闭材料后，应能在 90 秒内完成以下表达：

1. 画出注解、AOP、SpEL、Redis Lua 和业务方法的调用顺序。
2. 不看代码复述脚本四步，并解释原子性边界。
3. 明确当前 key 的真实限流对象，不虚构用户/IP 维度。
4. 主动说出同毫秒覆盖和 Redis 故障语义两个缺口。
5. 根据文章读取与付费 AI 的不同风险给出失败策略。
6. 说出至少四类限流指标和一个容量升级触发条件。
