---
id: interview-guide-java-jvm-memory-gc-diagnostics-v1
title: JVM 内存、类加载、GC 与故障诊断
source: interview-guide/java/jvm-memory-gc-diagnostics.md
upstream_repository: https://github.com/Snailclimb/interview-guide
upstream_commit: 969e2af8550f680688f5b50f0d895186a661a319
upstream_references: [app/src/main/resources/skills/_shared/references/java.md]
upstream_skill: app/src/main/resources/skills/java-backend/SKILL.md
license: AGPL-3.0
transformation: 基于上游 JVM 知识点独立扩展为从运行时结构到证据化排障的课程，并结合本项目有界线程池、PDF 解析和模型调用场景；未复制上游原文、Prompt 或源码
skill: JAVA_CORE
usage: 知识库阅读、RAG 问答、JAVA_CORE 题库、专项面试与复习
---

# JVM 内存、类加载、GC 与故障诊断

## 一、运行时内存按职责理解

堆保存对象和数组，是 GC 的主要区域；线程私有的虚拟机栈保存栈帧、局部变量和调用状态，本地方法栈服务 native 调用，程序计数器记录线程执行位置。元空间使用本地内存保存类元数据，但类对应的 `Class` 对象仍在堆中。直接内存不属于堆，却会受进程内存和 `MaxDirectMemorySize` 约束。

`StackOverflowError` 常由递归过深或栈帧过大触发；`OutOfMemoryError` 必须看具体区域：Java heap space、Metaspace、Direct buffer memory、unable to create native thread 的原因和证据不同。只说“内存不够，调大 Xmx”不是诊断。

## 二、对象分配、可达性与回收

对象通常先在 Eden 分配，大对象或特定收集器策略可能直接进入老年代。GC Roots 包括线程栈引用、静态字段、JNI 引用等；可达性分析决定对象是否仍被引用。强、软、弱、虚引用表达不同生命周期，但缓存不能依赖软引用获得稳定容量治理。

对象年龄、动态年龄判定和分配担保影响晋升。频繁 Young GC 可能来自分配速率高；老年代持续增长、Full GC 后回落很少才更像泄漏或长期存活对象过多。判断要结合 GC 日志、堆直方图和 dump 的引用链。

## 三、GC 选择与停顿边界

吞吐量是业务运行时间占比，延迟关注单次和尾部停顿，两者常需取舍。G1 将堆划分 Region，按收益选择回收集合，适合较大堆和可预测停顿目标；ZGC 更强调低停顿，但版本、资源成本和团队经验仍是选择条件。收集器名称不能替代目标：先给堆大小、分配速率、SLA、CPU 余量和可接受暂停。

调优顺序应是确认业务指标和 GC 证据，再减少不必要分配、限制缓存和批量大小，最后调整堆、Region、并发线程等参数。扩大堆可能降低频率，却增加故障转储和某些停顿成本。

## 四、类加载与双亲委派

类加载经过加载、验证、准备、解析和初始化。双亲委派优先让父加载器尝试，保护 Java 核心类唯一性并减少重复加载；SPI、容器隔离、插件和热部署可能使用线程上下文加载器或自定义加载器打破简单父优先。类卸载要求定义它的加载器、类对象和实例都不可达，热部署泄漏常是旧加载器被线程、ThreadLocal 或静态缓存引用。

## 五、本项目排障映射

本项目的 PDF 解析、Markdown 切分、Embedding 批次和模型上下文都会制造对象；回答、资料、题库使用有界线程池和队列，避免无界任务把堆与 native thread 一起耗尽。发生内存上涨时，应先按请求类型和任务状态定位，再看 `jcmd GC.heap_info`、类直方图、GC 日志和必要的 heap dump；模型超时、Redis/Qdrant 故障与 GC 暂停也要通过时间线区分。

不要在生产进程随意生成巨大 dump。先确认磁盘空间、停顿风险和脱敏要求；能用直方图、JFR 或小范围复现解决时，不扩大取证成本。

## 六、练习题

### 主问题 1｜原理与实现

Java 服务进程内存持续上涨，但堆使用率并不高，你会检查哪些 JVM 和操作系统内存区域？

参考回答：先区分 RSS、Java 堆、元空间、直接内存、线程栈和 native 库。检查 `-Xmx` 与实际堆、类加载器数量、DirectByteBuffer、线程数和每线程栈大小；结合 NMT、`jcmd`、线程 dump 和进程指标定位。堆不高不能排除直接内存泄漏或线程过多。

回答检查点：堆外内存、元空间、直接内存、线程栈、native thread、RSS 与证据工具。

追问：`unable to create native thread` 为什么不一定通过增大堆解决？

追问参考：它通常受操作系统线程/进程限制、地址空间和每线程栈占用影响；增大堆反而可能挤压 native 空间。

### 主问题 2｜并发与边界

一次 Full GC 后老年代只下降很少，如何区分内存泄漏、缓存容量过大和正常长期对象？

参考回答：比较多次 GC 后基线而非单点，按类直方图看增长对象，用 heap dump dominator tree 和 GC Roots 找引用链，并关联缓存大小、任务积压和流量。泄漏是无业务价值对象仍被强引用；大缓存可能有价值但缺容量边界；正常长期对象应与稳定业务状态匹配。

回答检查点：GC 后基线、增长趋势、引用链、缓存上限、任务队列和业务语义。

追问：为什么只看一次 heap dump 很容易误判？

追问参考：单次快照缺少增长趋势，可能恰逢流量高峰或正常缓存预热，需要时间序列和对照才能证明持续保留。

### 主问题 3｜结论与推理表达

如何为一个有 PDF 解析、Embedding 和模型调用的 Java 服务制定 JVM 故障排查顺序？

参考回答：先说明用户症状和时间线，区分延迟、OOM、CPU、线程耗尽；再看进程/容器资源、GC 日志、线程池队列和外部依赖；随后用直方图、线程 dump、JFR 或受控 heap dump验证。修复优先限制批次、并发和对象生命周期，再调整 JVM 参数，并用相同负载复测 P95/P99、GC 暂停和内存基线。

回答检查点：症状分层、时间线、资源指标、GC/线程证据、最小取证、修复后复测。

追问：为什么模型超时不能直接归因于 GC？

追问参考：网络、Provider 排队、线程池、连接池和 GC 都可能产生相同表象，必须用暂停日志与请求时间线相关性证明。
