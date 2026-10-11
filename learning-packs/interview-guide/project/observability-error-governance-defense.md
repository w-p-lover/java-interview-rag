---
id: interview-guide-observability-error-governance-defense-v1
title: Spring Boot 接口监控与异常治理答辩
source: interview-guide/project/observability-error-governance-defense.md
upstream_repository: https://github.com/Snailclimb/interview-guide
upstream_commit: ebe2697aeaa1c35fc780238f626165cf3e807782
upstream_reference: app/src/main/resources/skills/_shared/references/high-availability.md
upstream_skill: app/src/main/resources/skills/java-backend/SKILL.md
license: AGPL-3.0（上游材料许可）
transformation: 只学习公开概念与经验、机制、边界、故障的提问顺序，正文、例子、题面与回答独立编写；未复制上游原文、Prompt 或源码
usage: 正式后端 Q8 准备、知识检索、SYSTEM_DESIGN 证据题库与独立示范再练
local_baseline: java-interview-rag@3ee3cff
---

# Spring Boot 接口监控与异常治理答辩

## 一、这门课解决什么缺口

正式后端 Q8 要求把请求量、耗时、失败、日志与降级串成一套可长期运维的方案。回答“用了 Actuator、Micrometer、日志”还不够：必须解释统计对象、失败含义、时间窗口、排查证据和恢复条件。这里以本项目真实代码为例，不把未来告警平台、链路追踪或生产运维经验写成已实现能力。

先打开前端 `/observability` 看运行观测，再到首页 `/` 的资料目录找本课程；首页检索区域可问“HTTP 200 为什么不能证明回答可信”。练习入口为 `/question-bank` 的本课程 SYSTEM_DESIGN 题；创建自己的专项会话，首答后按遗漏再次作答。自动运行的示范会话只用于证明路径，正式本人会话 `4a0bfddf-7842-4611-bd8f-486181c3ffe5` 不由代理作答。

## 二、30 秒回答：先定义成功

我先区分 HTTP 请求成功、异步业务成功和回答证据成立。请求层按路由模板统计数量、4xx、5xx和耗时；任务层看排队、执行、拒绝、超时与最终结果；知识回答层看召回、TIMEOUT、引用校验和 grounded。Counter 与 Timer 用低基数标签，requestId 放日志用于定位，不放指标标签。故障处理先分类，再用同一时间窗口的失败率、长尾延迟、队列及下游信号决定减载或人工处理。当前项目有指标入口与业务日志，没有已验证的生产 SLO、自动告警或完整分布式追踪。

## 三、90 秒回答：从入口到终态

`BusinessRequestMetricsFilter` 只处理 `/api/`，在入口创建 `X-Request-Id`、设置 MDC并启动 Timer.Sample。同步请求在过滤链结束记录一次；异步请求通过 AsyncListener 在完成、超时或错误时记录，AtomicBoolean 防重复终结。operation 优先采用 Spring MVC 的路由模板，例如 `GET /api/interviews/sessions/{sessionId}`，匹配不到使用固定 unmatched 标签；不按真实 ID 建立新指标。

Counter 按 operation、outcome 记录请求数，Timer 按 operation 记录耗时。`BusinessObservabilityService` 汇总本进程数量、失败比例、平均耗时和 Timer 的窗口最大值，并组合资料任务及 Dense/Sparse 容量。它没有返回 P95/P99，也没有跨实例、跨重启累计；必须通过外部按时间窗口聚合的指标体系再定义正式 SLI。HTTP 200 的 TIMEOUT 兜底仍可能被请求层计为 SUCCESS，应结合 `RagRetrievalLog` 的状态、grounded 和耗时解释业务结果。

## 四、三分钟展开：数量、耗时与标签

Counter 单调累计，观察流量要看窗口内增量除以窗口秒数，而不是把累计总量称为 QPS。Timer 本身已有调用次数和总耗时，额外的 Counter 在本项目用于区分 outcome；不要把两者相加成为“双倍请求量”。同一观察窗口的失败率是失败数除以总数，零请求时应说明没有样本，不能据此宣布系统健康。

平均延迟会掩盖长尾。当前概览的 average 来自 Timer 累计耗时和请求 Counter；max 是 Timer 的时间窗口最大值，空闲一段时间可能归零，并非进程历史最慢请求。不要用当前 max 推断 P99。若需要 P95/P99，应先配置并验证直方图、抓取与查询窗口，同时区分完成耗时和仍未完成的在途请求；本轮没有新增该能力。

低基数标签用路由模板、有限 outcome 和业务类型。用户 ID、requestId、原始 URL、异常完整文本、问题正文不能作为指标标签；否则实例数、内存与查询成本会失控。用户输入与密钥也不能未经脱敏进入日志。资料任务 `running` 是逻辑状态计数，不等于执行器真实 active；超时标记失败后线程仍可能占用，须查看容量观测或线程证据。

## 五、异常分类与失败分母

参数非法、版本冲突、容量拒绝、检索超时、依赖不可用、程序缺陷应分开。现有题库 Controller 对非法参数返回400、缺失返回404、版本冲突返回409、不可用返回503；资料排队满返回429，检索 Advice 将截止时间和不可用分别映射504与503。它们是局部处理器，不是全项目已经实现统一错误码、统一响应体与统一关联 ID的证明。

请求过滤器把4xx归为 CLIENT_ERROR、5xx归为 SERVER_ERROR，概览把两类都计入 failureCount。业务可用性 SLI通常要排除正常的参数错误，并按策略解释容量拒绝，但当前概览没有做到这一细分。未被处理的异常经过过滤器时会计为500；业务服务捕获异常并返回兜底时，HTTP 可能是200，不能只靠接口状态码报警。应把业务成功定义和统计分母写清楚，再决定是否改变实现。

## 六、HTTP 200 与 grounded 的真实案例

2026-10-10 的执行器课程检索命中5条来源，知识回答约61秒后返回 TIMEOUT、grounded=false；HTTP 请求完成不等于生成成功。进程编排流式请求约29.1秒结束，8条模型陈述中5条有效、3条证据不足被过滤，业务日志为 PARTIAL_ANSWER。部分回答可帮助阅读，但不等于完整回答或每个原始陈述都可信。

`RagAnswerService` 持久记录状态、来源、Prompt版本、grounded、总耗时与错误类型，适合回答“失败落在哪一层”。`RagRetrievalLog` 当前没有 requestId 字段，也没有全链路分段时长；不能声称它已与 HTTP requestId 自动一对一关联。检索计数、生成延迟、引用有效性和用户学习效果是不同指标，命中率高不能替代后三者。

## 七、日志、MDC 与异步边界

日志用于解释一次失败的上下文：请求或任务标识、operation、错误分类、耗时、资料版本、attempt与终态，而不是批量输出原文。MDC 是线程上下文；入口写入不代表线程池或 Reactor 自动获得相同值。工作线程需要提交时快照、执行时设置、finally恢复原值，复用线程和 CallerRuns 情况尤其需要检查。当前过滤器没有实现这一套跨线程传播。

过滤器同步 finally 清除MDC，异步完成回调也清除，但异步入口线程与回调线程可能不同；因此现有源码不足以证明所有异步线程的关联与清理完整。没有新增修复或真实并发验证前，将此作为已知边界。`X-Request-Id` 是当前请求的关联线索，不是 trace/span，也不是防重复执行的 requestId；不能把诊断标识当业务幂等键。

## 八、从告警信号到减载和恢复

设计上先约定用户目标，再选 SLI和告警窗口。例如给知识回答定义“预算内且证据成立的比例”，将无资料的正确拒答单独计量，不能套用HTTP成功率。SLO 是服务目标，SLA 是对外约定；本项目没有生产样本来承诺99.9%。错误预算是该目标允许的失败量，不应拿一次代理评分或短时间开发验证来计算生产预算。

处理步骤是：对齐流量、失败分类、长尾延迟、队列/active和下游延迟；确认是依赖慢、容量不足、证据错误还是程序缺陷；先限制新工作或降低昂贵功能，保留资料阅读；依赖恢复后用有界探测和一次受控恢复验证，再逐步放量。不要无边界重试模型调用，也不要把扩大线程/timeout当通用恢复。告警应按影响、持续时间和样本量分级、聚合并有明确处理人；本地尚无自动告警、熔断状态机或半开探测实现。

## 九、代码与上游概念对应

| 上游学习点 | 本地入口与代码 | 当前差异与讲解边界 |
| --- | --- | --- |
| 可观测性和服务指标 | `/api/observability/overview`；`observability/BusinessRequestMetricsFilter.java`、`BusinessObservabilityService.java` | 当前实例摘要，HTTP与业务失败分开解释；无P95/P99与生产SLO |
| 限流、隔离与失败恢复 | `knowledge/task/KnowledgeDocumentTaskMetrics.java`；`knowledge/vector/KnowledgeRetrievalDeadline.java` | 有界执行与截止时间；逻辑running不等于物理active |
| 日志与故障追踪 | 入口MDC；`knowledge/rag/RagAnswerService.java`、`RagRetrievalLog.java` | 没有完整跨线程MDC传播、trace/span或requestId日志表关联 |
| 异常分类与降级 | `knowledge/web/KnowledgeRetrievalExceptionHandler.java`；题库及资料Controller | 局部错误映射、来源继续阅读；不是统一全局协议或自动熔断 |
| 告警与容量规划 | `/observability`；`application.yml` 的health/info/metrics | 当前没有Prometheus导出配置、告警规则、生产压测或自动扩缩容 |

上述Java路径以 `src/main/java/com/javainterview/rag/` 为根。Actuator目前只暴露 health、info、metrics；不能把依赖Actuator写成已经部署Prometheus、Grafana或分布式追踪。8.3借用三维Rubric组织训练，9.1的attempt与恢复状态用于解释异步业务终态；本课复用两条已有流程，不新增平台。

## 十、三道主问题、参考回答和追问

第一题（方案可行性）：如何把Spring Boot接口请求量、耗时、失败分类与日志串成可定位的方案？参考回答：入口按路由模板记Counter和Timer，同步/异步只终结一次，区分4xx和5xx；requestId留在日志并明确MDC跨线程边界。再结合任务终态和回答状态判断业务成功，HTTP 200不保证grounded；概览只提供本进程累计与窗口max，没有生产SLO或分位数。追问：异步请求返回200且生成超时，哪些证据能证明故障位置？

第二题（容量、一致性与恢复）：接口平均耗时稳定但用户持续报慢且任务running降为零，应怎样决定减载和恢复？参考回答：查同窗口流量、完成样本与未完成请求、P95/P99（需另有真实采集）、逻辑running与真实active、队列和拒绝、下游与业务状态；max归零不说明长期没有慢请求。先控新任务、保留阅读，再受控恢复并验证attempt防旧写回；不靠扩大线程或无限重试。追问：超时已标记FAILED，为什么还需检查active和下游连接？

第三题（取舍表达）：能否直接用当前failureRate制定99.9%可用性并把requestId作为指标标签？参考回答：不能。该比例累计且混入4xx，不是用户业务成功SLI，也没有窗口与足够样本；requestId是高基数关联标识，放日志而非标签。要先定义成功、分母、延迟阈值和窗口，再验证采集和告警；生产SLO、全链路追踪与自动熔断是后续方案。追问：哪些数据能支持开启自动熔断，哪些错误不应触发下游熔断？

## 十一、本人练习顺序

阅读后关掉材料，用30秒讲清三种成功，再用90秒说明本地入口与指标口径，最后用三分钟回答“200但TIMEOUT”的排障和恢复。首答记录具体遗漏，再沿题库证据进行二次作答；分数提高只表示这次表达改善，不证明生产系统可用性或用户已经掌握。尤其不要复述不存在的P99、告警阈值、生产事故、自动熔断和traceId关联。

资料检索只确认能找到相应机制正文，不以命中练习题面证明陌生问题的泛化召回质量。独立首答刻意只讲工具名，二答补齐指标语义、异步边界与故障步骤；两次代理评分用于验收证据绑定和再练路径，正式本人题目保持原难度。
