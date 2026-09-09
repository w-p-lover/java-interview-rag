# 索引版本漂移细分 Implementation Plan

> **For agentic workers:** 本轮在主线程按步骤执行，保持只读巡检，不引入自动修复。

**Goal:** 让知识索引巡检区分当前版本契约“全部不匹配”和“部分不匹配”，帮助调用方判断重建范围。

**Architecture:** 保持 Chunk 数量状态与索引版本契约状态正交。`StoreCheck` 新增契约状态字段，由现有当前版本总数和匹配数确定；数量异常仍由原 `StoreStatus` 表达，整体一致性要求两者都正常。

**Tech Stack:** Java 17 records、Spring Boot、JUnit 5。

**Spec:** `docs/09-后续开发路线.md` 的“索引与模型版本治理”完成标准。

## Global Constraints

- 只修改 `D:\IdeaProjects\java-interview-rag`。
- 不自动迁移、重建或修改 MySQL 登记。
- 保留现有 `indexVersionMatched` 字段兼容性。
- 仅运行直接相关的一致性巡检测试。

### Task 1: 扩展巡检契约状态

**Files:**
- Modify: `src/main/java/com/javainterview/rag/knowledge/consistency/KnowledgeIndexConsistencyReport.java`
- Modify: `src/main/java/com/javainterview/rag/knowledge/consistency/KnowledgeIndexConsistencyService.java`
- Test: `src/test/java/com/javainterview/rag/knowledge/consistency/KnowledgeIndexConsistencyServiceTest.java`

- [ ] 增加 `IndexVersionStatus`：`MATCHED`、`PARTIAL_MISMATCH`、`ALL_MISMATCH`、`UNVERIFIABLE`。
- [ ] 在 `inspectStore` 中根据当前版本 Chunk 总数与元数据匹配数推导状态；当前版本数量为 0 或预期数量不可知时保持 `UNVERIFIABLE`，不误报全量失配。
- [ ] 将新状态加入 `StoreCheck`，旧构造器默认 `MATCHED`，并让 `consistent` 要求契约状态为 `MATCHED`。
- [ ] 增加全量和部分不匹配测试，断言状态与整体不一致。

### Task 2: 最小验证与进度同步

**Files:**
- Modify: `docs/04-当前进度.md`

- [ ] 运行 `KnowledgeIndexConsistencyServiceTest`。
- [ ] 检查 `git diff --check` 和差异范围。
- [ ] 在当前进度记录本轮只读诊断能力与边界。
