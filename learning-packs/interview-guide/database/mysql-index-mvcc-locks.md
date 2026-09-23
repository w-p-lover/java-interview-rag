---
id: interview-guide-database-mysql-index-mvcc-locks-v1
title: MySQL 索引、执行计划、MVCC 与锁
source: interview-guide/database/mysql-index-mvcc-locks.md
upstream_repository: https://github.com/Snailclimb/interview-guide
upstream_commit: 969e2af8550f680688f5b50f0d895186a661a319
upstream_references:
  - app/src/main/resources/skills/_shared/references/mysql.md
  - app/src/main/resources/skills/_shared/references/database.md
  - app/src/main/resources/skills/_shared/references/db-design.md
upstream_skill: app/src/main/resources/skills/java-backend/SKILL.md
license: AGPL-3.0
transformation: 基于上游数据库知识点独立扩展为面试课程，结合本项目 MySQL 5.7.36 的真实索引、执行计划和并发写入代码形成案例；未复制上游原文、Prompt 或源码
usage: 知识库阅读、RAG 问答、DATABASE 题库、专项面试与复习
---

# MySQL 索引、执行计划、MVCC 与锁

## 学习目标

学完后应能把四条链路连起来：从查询条件设计联合索引；用 `EXPLAIN` 验证而不是猜测索引；用 Undo 版本链与 ReadView 解释一致性读；用 Record、Gap、Next-Key Lock 解释当前读、幻读和死锁。面试回答最终要落到本项目的唯一键、复合索引、乐观锁和 `FOR UPDATE` 分别保护什么。

## 一、B+ 树索引不是“加了就快”

InnoDB 的主键索引是聚簇索引，叶子节点保存行数据；普通二级索引的叶子节点保存索引列和主键值。二级索引找到主键后，再访问聚簇索引取得其余列，就是常说的回表。若查询所需列都能从当前索引取得，便形成覆盖索引，可以减少回表，但代价是更大的索引和更高的写入维护成本。

B+ 树适合磁盘索引，因为非叶子节点主要保存键和子节点指针，扇出高、树高低；叶子节点有序并相互连接，等值、范围和排序查询都能复用有序性。它并不意味着所有条件都能同时高效使用：联合索引首先按第一列排序，第一列相同时才按第二列排序，因此存在最左前缀边界。

### 联合索引的设计顺序

通常先放稳定的等值过滤列，再放范围或排序列，但这不是脱离 SQL 和数据分布的口诀。应同时判断：

1. 查询实际使用哪些等值、范围、排序和返回列。
2. 列选择性是否足以缩小扫描范围。
3. 范围条件之后的列还能否继续缩小索引扫描边界。
4. 是否值得增加返回列形成覆盖索引。
5. 新索引给 INSERT、UPDATE、DELETE 带来的维护和存储成本。

`(status, last_heartbeat_at)` 适合“先按任务状态，再找心跳截止时间”的租约扫描；只按 `last_heartbeat_at` 查询时，不能假设它一定能有效利用这个联合索引。`(resume_id, version_number)` 既支持某份简历的版本定位，也用唯一约束阻止同一记录出现重复版本号。

### 常见失效或收益下降场景

- 对索引列做函数、运算或类型转换，可能使条件无法直接转成索引范围。
- 字符串与数字隐式转换可能改变比较方式并丢失索引优势。
- `LIKE '%keyword'` 无法利用普通 B+ 树的左侧有序前缀。
- 联合索引跳过最左列，通常无法按预期缩小扫描范围。
- 返回行占表中比例过高时，优化器可能认为全表扫描比大量回表更便宜。
- 表很小时，全表扫描本身可能就是成本更低的选择。

所以“EXPLAIN 没选索引”不自动等于索引失效，也不自动等于 SQL 必须修改。先看候选索引、数据量、选择性和真实耗时。

## 二、EXPLAIN 要形成证据链

面试中不要只背 `type` 从好到坏。建议按下面顺序读：

1. `possible_keys`：优化器认为理论上可用的索引。
2. `key` 与 `key_len`：最终选择哪个索引，实际使用到多长的索引前缀。
3. `type`：访问方式，如 `const`、`eq_ref`、`ref`、`range`、`index`、`ALL`。
4. `ref`：索引查找拿什么列或常量比较。
5. `rows` 与 `filtered`：预计扫描行数和条件过滤比例，是估算而非真实计数。
6. `Extra`：是否覆盖索引、额外排序、临时表或其他执行信息。

`Using index` 通常表示覆盖索引，不等于“只要走索引都会显示”。`Using filesort` 表示不能直接用所选索引顺序完成排序，不代表一定写磁盘文件。`Using temporary` 常见于某些分组、去重或排序组合，需要结合结果集规模判断影响。

### 本项目 MySQL 5.7.36 的真实计划

2026-09-23 对开发库执行只读 `SHOW INDEX` 与 `EXPLAIN`：

- `t_knowledge_bank_question` 只有 9 行。按 `BINARY source` 查询并按 `updated_at, id` 排序时，计划是 `type=ALL`、`possible_keys=NULL`、`Using where; Using filesort`。`source` 上虽有 150 字符前缀索引，但 `BINARY` 表达式和排序尾列会影响候选与排序复用；当前表极小，全表扫描也未构成性能证据。
- `t_knowledge_bank_generation_task` 只有 1 行。代表性的 `status + last_heartbeat_at` 租约查询能看到 `possible_keys=idx_bank_generation_lease`，但优化器仍选择 `ALL`。这说明“存在候选索引”和“成本模型最终选择索引”是两件事。
- 空的 `t_resume_version` 上，按 `resume_id` 查询并按 `version_number DESC` 排序时选择 `uk_resume_version_number(resume_id, version_number)`，访问类型为 `ref`，没有额外排序信息。

这些结果只能证明当前开发库计划，不能外推大数据量。上线前应在接近真实基数、选择性和参数分布的数据上复查，并结合慢日志、实际耗时和锁等待，而不是只截图一张 EXPLAIN。

## 三、MVCC 解决的是一致性读

InnoDB 行记录包含事务相关隐藏信息，更新旧值通过 Undo Log 形成版本链。ReadView 描述当前一致性读能看到哪些事务版本；读取时沿版本链找到对当前视图可见的版本。

在 `READ COMMITTED` 下，一般每次一致性读都会建立新的视图，所以同一事务的两次普通查询可能看到其他事务中途提交的不同结果。在 `REPEATABLE READ` 下，同一事务中的一致性读通常复用首次建立的视图，因此能保持重复读取结果。这里说的是普通非锁定一致性读，不应把所有 SQL 都归为“快照读”。

`SELECT ... FOR UPDATE`、`SELECT ... FOR SHARE` 或 5.7 中的 `LOCK IN SHARE MODE`，以及 UPDATE、DELETE，都需要基于当前可用记录执行并配合加锁，属于当前读。于是同一事务里，普通 SELECT 与 `FOR UPDATE` 看见的版本可能不同；这不是 MVCC 失效，而是两类读取目标不同。

### 长事务为什么危险

长事务不仅长期占用连接和锁，还会让旧 ReadView 持续存活，使仍可能被读取的 Undo 版本不能及时清理。结果可能是 Undo 膨胀、历史链变长、锁等待扩大和故障恢复成本增加。因此本项目把慢模型调用放在事务外，数据库只承担短事务状态更新和结果落库。

## 四、Record、Gap 与 Next-Key Lock

InnoDB 行锁实际锁的是索引记录。Record Lock 锁定某个索引记录；Gap Lock 锁定索引记录之间的间隙，主要限制其他事务向间隙插入；Next-Key Lock 可以理解为记录锁与前方间隙锁的组合。

在默认 `REPEATABLE READ` 下，范围当前读可能使用 Next-Key Lock 防止其他事务在扫描范围插入新记录，从而控制幻读。使用唯一索引做精确等值查找且记录存在时，通常只需记录锁；范围、非唯一索引或未命中条件的锁定范围会更复杂。

由于锁跟索引访问路径相关，没有合适索引时，数据库可能扫描并锁住比业务预期更多的记录或范围。索引不仅影响查询性能，也影响锁冲突面。设计并发写入时要同时查看 SQL、实际执行计划、隔离级别和访问顺序。

### 死锁不是简单“调大超时”

两个事务以不同顺序持有对方需要的锁，会形成等待环。InnoDB 能检测部分死锁并回滚一个事务，但应用仍要处理失败。常见治理方式是固定加锁顺序、缩短事务、确保过滤条件使用合适索引、减少一次事务修改的对象数量，并对可安全重试的整个事务设置有上限重试。仅调大等待超时不会消除等待环。

## 五、本项目的并发一致性案例

### 题库：唯一键、查询索引和乐观锁分工

`t_knowledge_bank_question` 的 `uk_bank_question_content(content_key)` 阻止并发生成相同规范化题目；`idx_bank_question_source(source(150), updated_at)` 服务按资料读取最近题目；`version` 条件更新防止旧页面覆盖新版题目。唯一键解决“结果不能重复”，普通索引解决“如何找到候选”，乐观锁解决“旧状态不能覆盖新状态”，三者不能互相替代。

生成任务的 `uk_bank_generation_request(request_id)` 负责请求幂等，`uk_bank_generation_active(active_key)` 负责同资料当前版本只能有一个活动任务，`idx_bank_generation_lease(status, last_heartbeat_at)` 支撑按状态和心跳定位租约。attempt fencing 仍要写进 UPDATE 条件，因为普通索引不会自动阻止旧 Worker 的迟到写入。

### 简历版本：当前读、乐观锁和唯一键分工

`MySqlResumeCenterStore.saveVersion` 在短事务内通过 `SELECT ... FOR UPDATE` 锁定简历记录，再计算下一个版本号、插入不可变版本并更新当前指针。`uk_resume_version_number(resume_id, version_number)` 是最终并发兜底；`uk_resume_version_hash(content_hash)` 阻止相同文件并发重复；`UPDATE ... WHERE id=? AND version=?` 防止持有旧版本号的请求覆盖新指针。

方法进入事务前先查一次 Hash 是快速路径，事务内再次检查和捕获唯一键冲突才是并发正确性边界。面试时应说明每一层减少什么成本、兜住什么竞态，而不是笼统地说“加锁保证线程安全”。

## 六、面试回答模板

面对“这条 SQL 怎么优化”，可以按下面顺序回答：

1. 先给出 SQL 的过滤、关联、排序、返回列和数据量，不脱离查询谈索引。
2. 说明候选联合索引的列顺序、选择性、范围边界和是否覆盖。
3. 用 `EXPLAIN` 的 `possible_keys -> key/key_len -> type -> rows/filtered -> Extra` 验证。
4. 说明该 SQL 是一致性读还是当前读，使用哪个隔离级别。
5. 若有并发写入，说明实际索引访问路径对应的 Record/Gap/Next-Key Lock 范围。
6. 给出慢日志、真实数据分布、锁等待和压测结果，最后再决定保留或回退索引。

## 七、练习题

### 主问题 1

有联合索引 `(status, last_heartbeat_at)`，为什么 `EXPLAIN` 仍可能选择全表扫描？你会如何判断这个索引是否设计正确？

回答检查点：候选索引与最终选择的区别、表规模和选择性、最左前缀、范围与排序、`rows/filtered/Extra`、慢日志和真实数据验证。

追问：`Using filesort` 是否一定表示发生了磁盘排序？

### 主问题 2

在 MySQL 默认 RR 下，普通 SELECT 和 `SELECT ... FOR UPDATE` 为什么可能看到不同版本？范围当前读又为什么可能阻塞其他事务插入？

回答检查点：Undo 版本链、ReadView、一致性读、当前读、索引记录锁、Gap/Next-Key Lock、唯一索引精确查找与范围查找差异。

追问：缺少合适索引为什么可能扩大锁冲突范围？

### 主问题 3

本项目保存简历新版本时同时使用 `FOR UPDATE`、`(resume_id, version_number)` 唯一键、文件 Hash 唯一键和 `version` 乐观锁。这四层分别解决什么竞态，为什么不能只保留一种？

回答检查点：当前记录串行化、版本号唯一兜底、全局文件去重、旧请求覆盖保护、事务外快速检查与事务内最终校验。

追问：如果两个请求都在事务外查不到相同 Hash，最后如何保证只保存一份？

## 八、掌握标准

- 能根据具体 SQL 和数据分布设计联合索引，不只背最左匹配。
- 能完整读取 `EXPLAIN`，并解释小表全扫为什么可能合理。
- 能区分覆盖索引、回表、ICP 和排序复用，避免把 `Using index` 当成“用了索引”的通用标记。
- 能用 Undo 版本链和 ReadView 解释 RC、RR 的一致性读差异。
- 能区分快照读、当前读以及 Record、Gap、Next-Key Lock。
- 能结合本项目说明唯一键、普通索引、悲观锁、乐观锁和 attempt fencing 的职责边界。
- 能用真实计划、慢日志、数据基数和锁等待支撑优化结论，不把单次 EXPLAIN 当作生产性能证明。
