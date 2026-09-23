---
id: interview-guide-java-collections-hash-tables-v1
title: Java 集合、HashMap 与并发容器
source: interview-guide/java/collections-and-hash-tables.md
upstream_repository: https://github.com/Snailclimb/interview-guide
upstream_commit: 969e2af8550f680688f5b50f0d895186a661a319
upstream_references: [app/src/main/resources/skills/_shared/references/java.md]
upstream_skill: app/src/main/resources/skills/java-backend/SKILL.md
license: AGPL-3.0
transformation: 基于上游集合知识点独立扩展为选型、复杂度和并发正确性课程，并结合本项目证据排序、任务映射与去重代码；未复制上游原文、Prompt 或源码
skill: JAVA_CORE
usage: 知识库阅读、RAG 问答、JAVA_CORE 题库、专项面试与复习
---

# Java 集合、HashMap 与并发容器

## 一、集合选型先看访问模式

`ArrayList` 提供连续索引和摊销 O(1) 尾部追加，中间插入需要移动元素；`LinkedList` 节点开销和缓存局部性较差，只有已经持有节点位置时插删才体现优势。`HashSet` 基于哈希去重，`TreeSet` 提供排序和范围能力，`LinkedHashMap` 维护确定性迭代顺序，可实现访问顺序 LRU，但仍不是线程安全缓存。

复杂度是平均或摊销边界，不代表每次成本。扩容、哈希冲突、比较器成本和内存局部性会改变真实表现；小数据量时清晰性通常比理论常数更重要。

## 二、HashMap 的定位、扩容与树化

Java 8 的 HashMap 用数组加链表/红黑树。哈希扰动后通过 `(n - 1) & hash` 定位桶，因此容量使用 2 的幂；负载因子平衡空间和冲突。达到阈值后扩容，节点按旧容量对应位拆到原索引或 `原索引 + oldCap`。

桶元素达到树化阈值不一定立刻树化：容量过小时优先扩容，避免只是表太小导致冲突。正确 Key 必须满足 `equals` 相等则 `hashCode` 相等；可变字段参与二者会让对象放入后无法按新状态找到。

## 三、迭代、视图与不可变性

集合的 `subList`、`keySet`、`entrySet` 常是原集合视图，修改会互相影响。fail-fast 通过修改计数尽早暴露结构性并发修改，但不是线程安全保证，也不能依赖它必然抛异常。`List.of` 等不可变工厂拒绝修改，也不接受某些空值；不可变集合能缩小共享状态，但元素对象本身仍可能可变。

## 四、并发容器不是统一加锁

`ConcurrentHashMap` 的读大多无锁，更新通过 CAS 与桶级同步协调，不允许 null，以避免并发场景中“无映射”和“映射为 null”的歧义。复合操作应使用 `putIfAbsent`、`compute`、`merge` 等原子 API，不能把 `get` 和 `put` 拼成线程安全操作。

`CopyOnWriteArrayList` 写时复制，适合读多写极少且快照迭代的场景；大列表或高频写会产生复制和 GC 压力。高并发计数可考虑 `LongAdder`，但读取不是全局线性化快照，不适合需要严格序列的余额。

## 五、本项目代码映射

`BankSources` 使用 `HashSet` 保存规范化检索词，避免重复词重复计分；题库生成使用 `LinkedHashMap` 保持 Rubric 配额顺序，便于确定性生成和测试。`BankGenerationService` 的调度、排队工作和心跳句柄使用 `ConcurrentHashMap`，但跨多个 Map 的取消流程仍用同步块保护复合不变量，说明“用了并发容器”不等于复合业务动作天然原子。

RAG 评估的 RRF 融合用 `LinkedHashMap` 聚合同一 Chunk，保持稳定结果顺序；相关身份需要去重，否则 nDCG 等指标可能被重复来源放大。集合选择在这里同时影响正确性、确定性和复杂度。

## 六、练习题

### 主问题 1｜原理与实现

解释 HashMap 从 hash 定位、冲突处理到扩容的完整路径，为什么容量通常是 2 的幂？

参考回答：先扰动 Key 的 hash，再用 `(n-1)&hash` 定位桶；桶内按 hash 与 equals 查找，冲突使用链表，满足容量和阈值条件才树化。扩容到两倍后，节点根据 hash 的 oldCap 位分到原位置或原位置加 oldCap，无需重新取模。2 的幂让掩码覆盖低位并简化扩容拆分。

回答检查点：扰动、掩码、equals/hashCode、链表树化、扩容阈值和高位拆分。

追问：Key 的可变字段参与 hashCode 会发生什么？

追问参考：放入后字段变化会改变定位桶，逻辑上同一对象可能无法被 get/remove 找到，并破坏 Map 不变量。

### 主问题 2｜并发与边界

ConcurrentHashMap 已经线程安全，为什么取消题库任务时仍可能需要同步复合操作？

参考回答：ConcurrentHashMap 只保证单个方法或指定原子 API 的并发语义。若业务要同时检查任务状态、移除 queuedWork、取消 Future 并更新 scheduled，多步之间仍可能穿插。应把复合不变量放进同一锁、单个 compute 或数据库条件状态转换，并明确锁范围。

回答检查点：单操作安全、复合竞态、compute/锁/状态机、跨 Map 一致性、数据库最终条件。

追问：`get` 后判断为空再 `put` 为什么仍会重复创建？

追问参考：两个线程可同时看到空并分别创建，应使用 `putIfAbsent`、`computeIfAbsent`，副作用场景还需持久唯一约束。

### 主问题 3｜结论与推理表达

RAG 结果融合为什么既需要 HashMap 去重又需要稳定迭代顺序？

参考回答：融合要按 Chunk 身份聚合 Dense/Sparse 分数，Map 提供近似 O(1) 查找；若同一相关结果重复计数会扭曲排序和评估。使用 LinkedHashMap 或显式稳定排序可让相同输入输出顺序可复现，便于测试和比较。最终仍需按 RRF 分数、稳定 tie-break 排序，而不能依赖普通 HashMap 顺序。

回答检查点：身份去重、聚合复杂度、确定性、稳定 tie-break、指标正确性。

追问：为什么 HashSet 去重后仍不能直接代表最终排序？

追问参考：Set 只表达唯一性，不保存融合分数和业务排序规则；还需要分数聚合及确定性比较器。
