# Markdown 导入登记与重复判定实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为阶段 1 建立按“来源 + 内容版本”判断重复导入的最小登记能力。

**Architecture:** 新增进程内 `MarkdownImportRegistry`，使用结构化的来源与版本联合键记录已经登记的 `MarkdownDocument`。首次登记返回 `true`，相同来源与相同版本再次登记返回 `false`；同来源的新版本和同内容的不同来源仍可分别登记。第一版不增加持久化接口、数据库、文件读取或完整导入流水线。

**Tech Stack:** Java 17、JUnit 5、Maven。

## Global Constraints

- 所有项目写入仅发生在 `D:\IdeaProjects\java-interview-rag`，不修改只读参考项目。
- 不新增 Maven 依赖，不实现数据库持久化、文件读取、解析编排、向量化或并发登记。
- 新增异常信息和必要代码注释使用简体中文。
- 验证只覆盖导入登记及其直接依赖的 Markdown 文档版本模型，不执行无关测试。

---

### Task 1: 用失败测试固定联合去重语义

**Files:**
- Create: `src/test/java/com/javainterview/rag/knowledge/document/MarkdownImportRegistryTest.java`

**Interfaces:**
- Consumes: `MarkdownDocument.create(String source, String content)`。
- Produces expectation: `boolean MarkdownImportRegistry.register(MarkdownDocument document)` 在来源与版本首次出现时返回 `true`，历史上已登记过相同联合键时返回 `false`。

- [x] **Step 1: 编写重复版本、新版本和不同来源测试**

  测试断言：同来源且规范化换行后版本相同的文档只登记一次；同来源内容变化后可以登记；相同内容来自不同来源时可以分别登记。

- [x] **Step 2: 运行登记器测试并确认 RED**

  Run: `mvnw.cmd -Dtest=MarkdownImportRegistryTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test`

  Expected: 测试编译失败，原因是 `MarkdownImportRegistry` 尚不存在。

### Task 2: 实现最小内存登记器

**Files:**
- Create: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownImportRegistry.java`

**Interfaces:**
- `boolean register(MarkdownDocument document)`：登记文档的来源与版本联合键，并直接返回集合是否新增成功。

- [x] **Step 1: 新增来源与版本联合键集合**

  使用私有不可变联合键保存 `source` 与 `version`，避免字符串拼接产生边界歧义。

- [x] **Step 2: 实现首次登记与重复判定**

  直接返回集合 `add` 结果，不增加当前版本查询、删除、数据库接口或导入编排。

- [x] **Step 3: 运行登记器测试并确认 GREEN**

  Run: `mvnw.cmd -Dtest=MarkdownImportRegistryTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test`

  Expected: 登记器测试全部通过，0 失败、0 错误。

### Task 3: 同步项目事实并闭环验证

**Files:**
- Modify: `README.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`
- Modify: `docs/superpowers/plans/2026-07-21-markdown-import-registry.md`

- [x] **Step 1: 同步当前能力、开发记录与登记边界**

  只记录实际完成的联合去重与进程内登记能力，明确尚未实现文件读取、持久化、完整导入流水线和并发登记。

- [x] **Step 2: 运行直接相关测试**

  Run: `mvnw.cmd -Dtest=MarkdownImportRegistryTest,MarkdownDocumentTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test`

  Expected: 全部相关测试通过，0 失败、0 错误。

- [x] **Step 3: 检查并提交**

  Run: `git diff --check`、`git status --short`、`git diff --stat`。

  Expected: 只有本轮登记器、测试、计划和固定进度文档发生变化；随后使用 `feat: 实现 Markdown 导入去重登记` 提交。
