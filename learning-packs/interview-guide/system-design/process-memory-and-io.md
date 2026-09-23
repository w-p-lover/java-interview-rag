---
id: interview-guide-os-process-memory-io-v1
title: 进程线程、虚拟内存、文件与 IO 多路复用
source: interview-guide/system-design/process-memory-and-io.md
upstream_repository: https://github.com/Snailclimb/interview-guide
upstream_commit: 969e2af8550f680688f5b50f0d895186a661a319
upstream_references: [app/src/main/resources/skills/_shared/references/network-os.md]
upstream_skill: app/src/main/resources/skills/system-design/SKILL.md
license: AGPL-3.0
transformation: 基于上游操作系统知识点独立扩展为 Java 服务资源诊断课程，并结合本项目线程池、Lucene、PDF 与网络调用；未复制上游原文、Prompt 或源码
skill: SYSTEM_DESIGN
usage: 知识库阅读、RAG 问答、SYSTEM_DESIGN 题库、专项面试与复习
---

# 进程线程、虚拟内存、文件与 IO 多路复用

## 一、进程、线程和上下文切换

进程拥有独立虚拟地址空间和资源，线程共享进程内堆、文件描述符等，同时拥有栈和调度状态。线程不是越多越快：CPU 密集任务超过核心数会增加切换，阻塞 IO 任务可适度增加并发，但受内存、连接池和下游容量约束。

上下文切换要保存恢复寄存器和调度状态，还会损害缓存局部性。排障时结合 runnable、blocked、waiting 线程、CPU 利用率和 run queue 判断，不能看到线程多就直接认定死锁。

## 二、虚拟内存、页和缺页

虚拟内存给进程连续地址视图，通过页表映射物理页。缺页可能是正常按需加载，也可能因工作集超过物理内存触发频繁换页。RSS、虚拟地址空间、堆和 page cache 不是同一指标。容器内还要看 cgroup 限额，宿主机有空闲不代表容器可继续分配。

Copy-on-Write 让 fork 初期共享物理页，写入时复制；它降低快照初始成本，但写入密集时仍可能出现内存峰值。

## 三、文件、Page Cache 与 fsync

write 返回可能只表示数据进入内核页缓存，不等于稳定落盘；fsync 提供更强持久化边界，但增加延迟。顺序 IO 通常比随机 IO 友好，mmap 减少显式拷贝但缺页和映射生命周期仍需治理。删除文件后若进程仍持有文件描述符，磁盘空间不会立即释放。

Lucene 索引既受 Java 堆影响，也依赖文件系统 page cache；只调大 JVM 堆可能挤压 page cache。Qdrant 是独立进程和存储，不应把它的内存归到 Java heap。

## 四、阻塞、非阻塞与多路复用

阻塞 IO 让线程等待一个操作；非阻塞 IO 未就绪时立即返回；select/poll/epoll 让少量线程等待多个描述符的就绪事件。epoll 降低大规模连接扫描成本，但业务处理仍可能阻塞事件循环，因此耗时解析、数据库和模型调用应转移到受控工作线程。

## 五、本项目资源映射

服务同时包含 HTTP/SSE、PDFBox 解析、Lucene 文件索引、Qdrant/Redis/MySQL 网络调用和模型请求。有界线程池把不同工作负载隔离：Dense 与 Sparse 检索独立、模型回答有并发许可、资料任务有队列上限。若把阻塞模型调用放进 Web 事件线程，会让连接可读但无人处理。

诊断延迟先看 CPU、线程状态、上下文切换、磁盘 IO、page fault、文件描述符和连接池，再关联 JVM GC 与业务指标。单看应用日志无法区分 page cache 抖动、网络等待和锁竞争。

## 六、练习题

### 主问题 1｜方案可行性

一个 Java RAG 服务同时解析 PDF、读写 Lucene 并调用远程模型，如何划分线程池和资源预算？

参考回答：按 CPU/本地 IO/远程阻塞特征隔离执行器；PDF 解析限制文件大小和并发，Lucene 写入串行或受控并给 page cache 留空间，远程调用用有界池、连接池和 deadline。总并发受 CPU、堆外、文件描述符、数据库连接和 Provider 配额共同限制。

回答检查点：工作负载分类、舱壁、有界队列、page cache、连接池、超时和拒绝。

追问：为什么一个无界 cached thread pool 可能同时耗尽内存和下游连接？

追问参考：阻塞请求会持续创建线程，每个线程占栈并竞争连接，下游越慢积压越多，形成正反馈。

### 主问题 2｜容量、一致性与恢复

容器 RSS 持续接近上限，但 Java heap 只有一半，如何定位？

参考回答：核对 cgroup limit、RSS、heap committed/used、Metaspace、DirectBuffer、线程栈、mmap 和 native 库；查看线程数、NMT、`pmap`、文件映射和 page fault。Lucene mmap/page cache、PDF native buffer 或线程爆炸都可能不体现在 heap used。

回答检查点：cgroup、堆外、线程栈、mmap、NMT、文件映射和趋势。

追问：为什么简单增大 Xmx 可能让问题更严重？

追问参考：容器总内存固定时，更大堆会挤压 native 空间和 page cache，反而更易触发 OOMKill 或 IO 抖动。

### 主问题 3｜取舍表达

epoll 能处理大量连接，为什么服务仍需要业务线程池、限流和超时？

参考回答：epoll 只高效报告 IO 就绪，不执行 PDF 解析、SQL、检索和模型调用。事件线程被阻塞仍会拖慢所有连接；业务池隔离耗时任务，限流控制到达量，超时释放资源。容量最终由最慢下游和 CPU/内存决定。

回答检查点：就绪通知边界、事件线程、业务阻塞、舱壁、限流和瓶颈资源。

追问：连接数很高但 CPU 很低，可能在等待什么？

追问参考：可能等待远程响应、连接池、磁盘、锁或限流许可，需要线程栈和分阶段耗时判断。
