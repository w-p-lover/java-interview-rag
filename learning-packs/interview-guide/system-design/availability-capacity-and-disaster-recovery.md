---
id: interview-guide-ha-capacity-disaster-recovery-v1
title: 高可用、容量治理、熔断降级与容灾
source: interview-guide/system-design/availability-capacity-and-disaster-recovery.md
upstream_repository: https://github.com/Snailclimb/interview-guide
upstream_commit: 969e2af8550f680688f5b50f0d895186a661a319
upstream_references: [app/src/main/resources/skills/_shared/references/high-availability.md, app/src/main/resources/skills/_shared/references/system-design-scenarios.md]
upstream_skill: app/src/main/resources/skills/system-design/SKILL.md
license: AGPL-3.0
transformation: 基于上游高可用和系统设计场景独立扩展为容量、故障与恢复课程，并结合本项目检索降级、线程池舱壁、任务恢复和多存储巡检；未复制上游原文、Prompt 或源码
skill: SYSTEM_DESIGN
usage: 知识库阅读、RAG 问答、SYSTEM_DESIGN 题库、专项面试与复习
---

# 高可用、容量治理、熔断降级与容灾

## 一、从 SLI、SLO、错误预算开始

SLI 是延迟、成功率、正确率等实际指标，SLO 是目标，SLA 是对外承诺。99.9% 月可用性仍允许约 43 分钟不可用；目标必须定义统计窗口、请求口径和排除项。错误预算决定发布速度和风险，而不是只做漂亮看板。

## 二、容量估算与过载保护

用峰值 QPS、平均/尾部耗时估算并发，Little's Law 给出 `L = λW` 的基本关系。还要核对线程、队列、连接池、CPU、内存、带宽和下游配额。平均值会掩盖尾延迟，容量至少观察 P95/P99 和突发系数。

限流控制进入量，舱壁隔离资源，超时释放等待，熔断在持续故障时快速失败，降级提供可接受结果。顺序错误会让保护失效：例如没有总 deadline 的重试先把线程池占满，再熔断已太迟。

## 三、熔断与恢复状态

熔断器通常 Closed、Open、Half-Open 转换。触发条件可基于失败率或慢调用率，但样本窗口过小会抖动。Half-Open 只放少量探测，请求成功达到门槛再关闭；恢复不能仅靠人工猜测，也不能一次成功就放开全部流量。

## 四、RTO、RPO 与灾备

RTO 是恢复服务时间，RPO 是可接受的数据丢失窗口。备份存在不等于可恢复，必须定期还原演练并校验应用版本、密钥、对象和数据库一致性。主备切换还要防脑裂，使用仲裁、租约和 fencing 限制双主写。

## 五、本项目可用性映射

Hybrid 检索将 Dense/Sparse 放入独立舱壁，单路失败降级、双路失败明确不可用；回答链有并发许可、队列和总超时。资料与题库任务都有失败终态，题库还通过心跳、attempt 和最大恢复次数处理失联。MySQL、Qdrant、Lucene 通过一致性巡检暴露部分失败，并从快照人工重建。

边界必须保留：本地实例没有跨机房 SLA，也没有证明多副本容灾；Redis、Qdrant 或 MySQL 的高可用需要部署层和演练支持，代码中的 fallback 不能替代灾备。

## 六、练习题

### 主问题 1｜方案可行性

如何为 RAG 问答定义可执行的 SLO 和容量模型？

参考回答：分别定义检索成功率、grounded 回答率、端到端 P95/P99、拒答正确性和来源可用性；按峰值 QPS 与尾延迟估算并发，再核对模型配额、线程池、连接池和缓存命中。将降级回答与完整回答分开统计，不能用 HTTP 200 掩盖质量失败。

回答检查点：质量与可用性 SLI、统计窗口、峰值并发、下游配额、降级口径和错误预算。

追问：为什么只统计接口 200 率会高估 RAG 可用性？

追问参考：无证据、错误引用、检索降级或固定兜底都可能返回 200，但用户没有得到可信答案。

### 主问题 2｜容量、一致性与恢复

模型调用变慢导致线程池排队时，限流、舱壁、超时、熔断和降级应如何配合？

参考回答：入口限流控制到达量，模型独立舱壁限制运行与排队，总 deadline 到期释放资源；慢调用率达到窗口门槛后熔断，Open 快速失败，Half-Open 小流量探测；可返回检索证据、明确稍后重试或拒答。指标覆盖队列、拒绝、超时、熔断状态和下游耗时。

回答检查点：保护顺序、有界队列、慢调用熔断、半开探测、业务降级和指标。

追问：为什么无限队列会让熔断恢复更困难？

追问参考：旧请求持续堆积并消耗恢复后的容量，用户在排队中早已超时，系统无法快速回到新鲜流量。

### 主问题 3｜取舍表达

如何为 MySQL、Qdrant、Lucene 和资料快照设计 RTO/RPO 与恢复演练？

参考回答：先确定 MySQL 版本/正文是权威，索引是可重建派生数据；MySQL 备份和日志决定 RPO，应用与数据库恢复决定基础 RTO，Qdrant/Lucene 可从快照重建但时间计入 RTO。演练应恢复到隔离环境，核对版本、Chunk 数、检索样本和切换回滚，并记录实际时间。

回答检查点：事实源与派生数据、备份日志、重建时间、隔离演练、校验和回滚。

追问：为什么“每天有备份”不能证明 RTO 达标？

追问参考：恢复脚本、依赖、权限或备份本身可能失效，且恢复耗时未知；必须实际还原和计时。
