---
id: interview-guide-java-algorithms-data-structures-v1
title: Java 算法、数据结构与复杂度表达
source: interview-guide/java/algorithms-and-data-structures.md
upstream_repository: https://github.com/Snailclimb/interview-guide
upstream_commit: 969e2af8550f680688f5b50f0d895186a661a319
upstream_references: [app/src/main/resources/skills/_shared/references/algorithm-data-structures.md]
upstream_skill: app/src/main/resources/skills/algorithm/SKILL.md
license: AGPL-3.0
transformation: 基于上游算法与数据结构知识点独立扩展为 Java 解题、复杂度和工程算法课程，并结合本项目切分、证据排序与 RRF；未复制上游原文、Prompt 或源码
skill: JAVA_CORE
usage: 知识库阅读、RAG 问答、JAVA_CORE 题库、专项面试与复习
---

# Java 算法、数据结构与复杂度表达

## 一、先定义输入、输出与不变量

算法面试先澄清空输入、重复值、数据范围、是否允许修改输入和空间限制。写代码前说出循环或递归不变量，能显著降低边界错误。复杂度应说明 n 代表什么、平均/最坏/摊销口径，递归还要计算调用栈。

## 二、常用结构与模板

数组适合随机访问；链表适合已知节点位置的连接修改。栈用于括号、单调栈和 DFS，队列用于 BFS，堆维护 TopK，哈希表换取近似 O(1) 查找，树表达层级和有序关系，图需要显式处理 visited 防止环。

双指针适合有序区间和滑动窗口；二分查找关键是区间定义与单调性；回溯维护选择、递归、撤销；动态规划先定义状态、转移、初值和遍历顺序。不要背模板后忽略适用条件。

## 三、排序、TopK 与稳定性

比较排序一般下界 O(n log n)。快速排序平均 O(n log n)、最坏 O(n²)，归并排序稳定但需额外空间，堆排序原地但缓存局部性和稳定性不同。TopK 在 k 远小于 n 时可用大小为 k 的堆达到 O(n log k)，全排序并非总是错误，但成本更高。

## 四、工程算法也要证明

Markdown Chunker 按章节和段落保持顺序，在 token 超限时二分寻找可接受前缀；证据选择对每个 Chunk 计算词项相关度后排序；Hybrid 用 RRF 按排名融合，并按 Chunk 身份去重。算法正确性不仅是返回值，还包括顺序稳定、重复身份、边界和复杂度。

大语料下为每题扫描全部 Chunk 是 O(n)，目前题库人工建题规模可接受；若规模扩大，应先用倒排召回候选再排序，而不是无证据提前优化。

## 五、练习题

### 主问题 1｜原理与实现

如何设计一个返回数组 TopK 最大值的算法，并比较全排序与最小堆？

参考回答：全排序 O(n log n)，实现简单；维护大小 k 的最小堆，前 k 个入堆，之后大于堆顶才替换，时间 O(n log k)、空间 O(k)。若需要最终有序，再对堆中 k 个元素排序。还要定义重复值和 k 越界行为。

回答检查点：输入边界、堆不变量、时间空间、结果顺序和 k 与 n 关系。

追问：k 接近 n 时为什么全排序可能更合适？

追问参考：两者渐进成本接近，排序库常数、缓存局部性和代码简单性可能更优。

### 主问题 2｜并发与边界

对所有知识 Chunk 计算相关度再排序的复杂度是什么，规模增大后如何优化且不破坏证据正确性？

参考回答：若 n 个 Chunk、每个比较或打分成本为 t，打分 O(nt)，全排序 O(n log n)。可先用倒排/Dense 召回小候选集，再在候选内精排；保留 source/version/heading 身份和稳定 tie-break，并用固定题集验证 Recall@K，不能只追求延迟。

回答检查点：扫描与排序复杂度、候选召回、精排、身份元数据、Recall 和稳定性。

追问：为什么直接截断前 100 个文档再打分可能漏掉正确证据？

追问参考：截断若与查询无关，会系统性丢失后部相关项；候选选择必须有可验证召回策略。

### 主问题 3｜结论与推理表达

二分查找经常出现死循环或漏解，你会如何用区间不变量写清楚？

参考回答：先选择闭区间 `[l,r]` 或左闭右开 `[l,r)` 并保持一致。闭区间循环 `l<=r`，排除 mid 后用 `mid+1/mid-1`；左闭右开循环 `l<r`，右边界设 mid。计算 mid 用 `l+(r-l)/2`。返回位置或插入点要由最终区间语义推出。

回答检查点：区间定义、循环条件、边界推进、溢出和返回语义。

追问：查找第一个大于等于 target 时，什么是循环结束后的不变量？

追问参考：常用左闭右开写法结束时 l==r，左侧都小于 target，l 是第一个可能满足条件的位置。
