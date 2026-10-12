---
id: interview-guide-project-mysql-rr-lock-investigation-lab-v1
title: MySQL RR 锁等待双连接实战与答辩
source: interview-guide/project/mysql-rr-lock-investigation-lab.md
upstream_repository: https://github.com/Snailclimb/interview-guide
upstream_commit: ebe2697aeaa1c35fc780238f626165cf3e807782
upstream_reference: app/src/main/resources/skills/_shared/references/mysql.md
upstream_skill: app/src/main/resources/skills/java-backend/SKILL.md
upstream_license: AGPL-3.0
transformation: 只参考公开知识点和经验到机制、边界、故障的学习顺序；实验代码、案例、讲解与题面独立编写，未复制上游原文、Prompt 或源码
official_references:
  - https://dev.mysql.com/doc/refman/5.7/en/innodb-consistent-read.html
  - https://dev.mysql.com/doc/refman/5.7/en/innodb-locking.html
  - https://dev.mysql.com/doc/refman/5.7/en/innodb-parameters.html#sysvar_innodb_lock_wait_timeout
usage: 正式后端 Q7 准备、资料检索、DATABASE 题库、双连接实验与复习
---

# MySQL RR 锁等待双连接实战与答辩

## 一、这次补齐什么

正式后端 Q7 要求分析可重复读下的查询变慢和锁等待。已有 MySQL 基础课程说明了索引、ReadView 与锁，本案例补齐“看到现场之后怎么判断”的实践。先预测两条连接的结果，再运行实验，最后不看材料解释一次等待和一次不等待。

上游 MySQL 提纲提供索引、MVCC、锁和故障追问方向，本地用 MySQL 5.7.36 的真实双连接结果独立展开。学习路线是上游 mysql.md → 本案例 → `evaluation/run_mysql_lock_lesson.py` → 已有知识检索和 DATABASE 专项题库 → 面试报告与同题再练。实验不是生产事故，也不是本人经历或性能提升证明。

## 二、如何运行并阅读现场

在项目根目录运行 `python evaluation/run_mysql_lock_lesson.py --env-file D:/IdeaProjects/blog/blog-springboot/.env`。脚本只读已有配置，也接受 `BLOG_DB_HOST`、`BLOG_DB_PORT`、`BLOG_DB_USERNAME`、`BLOG_DB_PASSWORD` 环境变量；不把凭据写入结果。需要现有 PyMySQL，默认输出为项目 `output/mysql-lock-lesson.json`。

目标固定为开发库 `java_interview_rag`，版本门槛为 MySQL 5.7。脚本创建独立 InnoDB 教学表 `t_learning_mysql_lock_lab`，主键 `(run_id,item_id)`。每次随机 run_id 隔离自己的三行，item_id 为 10、20、30，balance 初始 100；保留本轮三行教学记录，不清理其他轮次。只有快照演示的 B 更新提交到本轮实验行，其余竞争写入均显式回滚，业务表不写入。

结果包含实际版本、访问计划、持锁与竞争 SQL、错误码、耗时、释放后是否恢复以及最终三行。先看 `rr_snapshot_vs_current`，再比较 `rr_existing_unique`、`rr_range_insert`、`rc_range_insert`、`rr_same_row_update`。两个连接在同一脚本中按顺序交错执行；A 持锁时 B 真正等待数据库，不是 Mock。

## 三、RR 普通读与当前读观察到什么

连接 A 以 REPEATABLE READ 开启事务，普通 SELECT 读到 item_id=20 的 balance=100。B 更新为 101 并提交，A 再次普通 SELECT 仍为 100；A 的 `SELECT ... FOR UPDATE` 则读到 101。2026-10-12 实际结果为 `100 → 100 → 101`。

原因是 A 首次一致性读建立 ReadView，后续普通读沿版本链读取可见旧版本；当前读用于锁定和处理当前可用记录。RR 不等于事务中所有读取永远相同，也不等于所有读都锁住记录。不能把“事务开始”机械等同于“普通快照已经建立”，也不要把快照读与当前读混在一起解释幻读。

## 四、完整唯一键等值与同一行竞争

A 对本轮 `(run_id,20)` 执行 FOR UPDATE，完整主键等值且记录已存在。B 插入 `(run_id,15)` 成功并回滚，说明这次相邻插入未被等值记录锁阻塞。若 B 改为更新 `(run_id,20)`，则等待并在本实验会话设定的一秒锁等待预算后得到错误码 1205；A 释放锁后 B 可以更新，再回滚。

必须同时说明“唯一索引全部列、精确等值、记录存在”这些条件。这里不能推广成所有等值查询都不锁间隙：非唯一索引、联合唯一键只匹配部分列或未命中记录，会改变访问与锁范围。记录锁保护已存在的索引记录，不能直接代替“防止重复创建”的唯一约束。

## 五、RR 范围锁与 RC 对照

A 在 RR 下执行本轮 `item_id>=10 AND item_id<30 FOR UPDATE`，B 插入 15 在实测中等待并返回 1205；A 回滚释放锁后，B 插入成功并回滚。该范围当前读对相关索引记录和间隙的保护，可以阻止新记录进入扫描范围。

同一实验改为 READ COMMITTED 后，B 插入 15 成功。RC 通常减少搜索和索引扫描中的间隙锁，但外键检查、唯一键冲突检查等仍有例外；不能概括为 RC 没有锁，更不能为了消除等待直接全局修改隔离级别。本实验没有外键，主键完整且没有唯一键冲突。

我们通过竞争行为证明“15 的插入在 RR 被阻塞、RC 未被阻塞”，没有查询完整底层锁清单，不把它冒充所有边界端点的精确锁区间证明。锁区间受索引、谓词和实际扫描路径影响，面试时先展示 SQL 再谈范围。

## 六、执行计划与等待需要两条证据链

本轮完整主键等值 EXPLAIN 为 `type=const / key=PRIMARY / rows=1`；只有 balance 条件而无该列索引的计划为 `type=ALL / key=NULL / rows=3 / Using where`。这是三行教学数据的访问计划，rows 是估计，不是执行耗时，不支持“索引优化提升若干倍”的说法。也没有因为 ALL 就给生产表加索引。

排查真实慢 SQL 时，先确认完整 SQL、参数、数据量、统计信息、耗时窗口，再看 possible_keys、key/key_len、type、rows/filtered 和 Extra。锁等待则要另外找持锁者、等待者、事务开始时间、未提交操作和加锁顺序。MySQL 5.7 可在有权限时关联 information_schema 的 INNODB_TRX、INNODB_LOCK_WAITS、INNODB_LOCKS，结合 PROCESSLIST；死锁查看 `SHOW ENGINE INNODB STATUS`。本实验未采集这些管理视图，不宣称已获得生产现场。

一个查询命中主键仍然可能因另一事务持锁而慢；反过来小表全扫未必慢。先区分等待锁、扫描/排序、连接池排队和下游延迟，再决定优化方向，不能把所有慢请求归结为“索引失效”。

## 七、1205、1213 与恢复边界

本轮只制造 1205 锁等待超时，没有制造 1213 死锁。1205 不代表已经检测到等待环，也不一定自动回滚整个事务；默认情况下通常回滚超时语句，事务中其他已完成语句和持有的锁可能仍在。脚本明确回滚 B、再回滚 A，之后验证同一实验 SQL 可以执行。

1213 表示死锁受害事务被回滚；应用需要按完整业务事务和幂等边界决定是否有上限重试。生产处理先定位持锁者和事务年龄，缩短事务、调整合理索引、固定多对象加锁顺序；终止其他连接需要确认所有者与业务影响。不要自动 KILL 连接，也不要仅提高 innodb_lock_wait_timeout。脚本只修改自己两个会话的锁等待预算，没有改变全局数据库配置。

## 八、对应本项目真实代码

`MySqlResumeCenterStore.saveVersion` 在事务外先查文件 Hash，这是减少重复成本的快速路径。事务内 `saveInTransaction` 再查 Hash，已有简历时由 `ResumeCenterMapper.lockRecord` 使用 `WHERE id=? FOR UPDATE` 锁住记录，计算 nextVersionNumber、插入版本，再更新当前版本指针。

现有唯一记录的精确查找对应本实验的主键记录锁。同一简历并发保存需要在这条记录上串行；`(resume_id,version_number)` 唯一约束负责版本编号兜底，content_hash 唯一约束负责重复文件，`updateCurrentVersion` 的 `WHERE id=? AND version=?` 负责防旧状态覆盖。不能因为用了 FOR UPDATE，就删除唯一键或乐观条件。代码实际使用短 TransactionTemplate，不是在锁内调用慢 Chat。

这里是对当前代码的静态对照；本轮没有并发调用真实简历接口，没有改 Mapper、索引或事务传播。实验表只是复现机制，不能证明真实简历保存所有并发边界均已通过，也不声称解决了 JRAG-008 的模型生成时延。

## 九、30 秒与 90 秒回答

30 秒：先把慢扫描和锁等待分开，拿 SQL、参数、隔离级别与事务时间，再用 EXPLAIN 看访问路径，用等待关系找真正持锁者。RR 普通读靠 ReadView，当前读会锁索引记录和相关间隙；完整唯一键命中与范围扫描的锁范围不同。修复优先短事务、合理索引和固定顺序，不直接加超时或改隔离级别。

90 秒：我在开发库做了双连接实验。A 的 RR 普通读先看到 100，B 更新到 101 提交后 A 仍读 100，但 FOR UPDATE 读到 101。A 锁已有完整主键 20 时相邻插入 15 可执行，更新同一行会等待；A 锁范围 10 到 30 时插入 15 在 RR 超时、在 RC 可执行。两个 1205 场景释放持锁事务后都恢复。它证明具体 SQL 的竞争行为，没有证明生产吞吐或全部锁端点。项目简历保存用短事务记录锁串行编号，唯一键兜底重复，version 条件防旧写，这几个职责各不相同。

## 十、作答、再练与掌握标准

先闭卷回答三个问题：普通读为什么保持 100 而当前读看到 101；为什么完整主键命中和 RR 范围扫描对插入 15 的影响不同；主键命中仍慢时如何区分扫描成本、持锁事务和 1205/1213。再运行脚本对照预测，在资料库阅读本案例，通过 DATABASE 题库完成独立练习并从报告进入同题再练。

再练时必须补出自己的因果链：SQL 与实际索引 → 当前读/一致性读 → 持锁与等待者 → 恢复条件 → 实现与验证边界。不以工具名或模型高分代替理解。能准确解释三种未发生的情况也是标准：本轮未测死锁、未采集生产锁清单、未证明任何性能提升。
