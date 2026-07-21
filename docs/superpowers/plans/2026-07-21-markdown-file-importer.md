# Markdown 文件导入编排实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立从 UTF-8 Markdown 文件到版本识别、重复判定、标题解析和 chunk 切分的最小导入入口。

**Architecture:** 新增 `MarkdownFileImporter` 读取调用方指定的文件，并使用显式逻辑来源创建 `MarkdownDocument`，随后串联现有登记器、标题解析器和切分器。新增不可变 `MarkdownImportResult` 返回文档、是否首次导入和 chunk 列表，避免用空列表同时表示重复导入和空文档成功。

**Tech Stack:** Java 17、JUnit 5、Maven、JDK NIO。

## Global Constraints

- 所有项目写入仅发生在 `D:\IdeaProjects\java-interview-rag`，不修改只读参考项目。
- 不新增 Maven 依赖，不实现目录扫描、扩展名校验、数据库持久化、并发导入或向量化。
- 文件第一版固定按 UTF-8 读取，逻辑来源由调用方显式传入，不使用本机绝对路径作为文档来源。
- 新增异常信息和必要代码注释使用简体中文。
- 验证只覆盖文件导入编排及其直接依赖，不执行无关测试。

---

### Task 1: 用失败测试固定文件导入结果语义

**Files:**
- Create: `src/test/java/com/javainterview/rag/knowledge/document/MarkdownFileImporterTest.java`

**Interfaces:**
- Consumes: `MarkdownImportRegistry`、UTF-8 Markdown 文件、逻辑来源和最大 chunk 字符数。
- Produces expectation: `MarkdownFileImporter.importFile(Path file, String source)` 返回 `MarkdownImportResult`。

- [x] **Step 1: 编写首次导入、重复导入和内容更新测试**

  测试断言：首次导入读取 UTF-8 内容并返回保留来源、标题路径和顺序的 chunk；相同来源与版本再次导入时 `imported` 为 `false` 且不返回 chunk；文件内容更新后可以再次导入。

- [x] **Step 2: 编写无效 chunk 上限测试**

  构造导入器时拒绝小于等于 0 的最大长度，避免文档已登记后才在切分阶段失败。

- [x] **Step 3: 运行导入器测试并确认 RED**

  Run: `cmd.exe /d /c "mvnw.cmd -Dtest=MarkdownFileImporterTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test"`

  Expected: 测试编译失败，原因是 `MarkdownFileImporter` 和 `MarkdownImportResult` 尚不存在。

### Task 2: 实现最小文件导入入口

**Files:**
- Create: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownImportResult.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownFileImporter.java`

**Interfaces:**
- `MarkdownImportResult(MarkdownDocument document, boolean imported, List<MarkdownChunk> chunks)`：复制 chunk 列表并保存本次导入状态。
- `MarkdownFileImporter(MarkdownImportRegistry registry, int maxChunkLength)`：复用同一登记器并在构造时校验长度上限。
- `MarkdownImportResult importFile(Path file, String source) throws IOException`：读取文件并编排现有文档处理组件。

- [x] **Step 1: 新增不可变导入结果**

  结果保存本次创建的文档、是否首次登记和不可修改的 chunk 列表；重复导入返回 `imported=false` 与空 chunk 列表。

- [x] **Step 2: 新增 UTF-8 文件读取与导入编排**

  先读取文件并创建 `MarkdownDocument`；登记器返回重复时直接返回，不再解析和切分；首次登记后依次调用现有标题解析器和 chunk 切分器。

- [x] **Step 3: 运行导入器测试并确认 GREEN**

  Run: `cmd.exe /d /c "mvnw.cmd -Dtest=MarkdownFileImporterTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test"`

  Expected: 文件导入测试全部通过，0 失败、0 错误。

### Task 3: 同步项目事实并闭环验证

**Files:**
- Modify: `README.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`
- Modify: `docs/superpowers/plans/2026-07-21-markdown-file-importer.md`

- [x] **Step 1: 同步文件导入能力与边界**

  只记录实际完成的 UTF-8 单文件读取、显式逻辑来源、重复短路、标题解析和 chunk 切分编排，明确仍未实现持久化、目录扫描和向量化。

- [x] **Step 2: 运行直接相关测试**

  Run: `cmd.exe /d /c "mvnw.cmd -Dtest=MarkdownFileImporterTest,MarkdownImportRegistryTest,MarkdownHeadingParserTest,MarkdownSectionChunkerTest,MarkdownDocumentTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test"`

  Expected: 全部 Markdown 导入链路测试通过，0 失败、0 错误。

- [x] **Step 3: 检查并提交**

  Run: `git diff --check`、`git status --short`、`git diff --stat`。

  Expected: 只有本轮文件导入器、结果模型、测试、计划和固定进度文档发生变化；随后使用 `feat: 串联 Markdown 文件导入` 提交。
