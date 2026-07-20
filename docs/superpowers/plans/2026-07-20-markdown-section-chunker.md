# Markdown Section Chunk 切分实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将有序 `MarkdownSection` 按段落和最大字符数切分为可追踪来源、标题路径与全局顺序的不可变 chunk。

**Architecture:** 新增不可变 `MarkdownChunk` 保存来源、全局 chunk 顺序、原 section 顺序、标题级别、标题路径和正文；新增无状态 `MarkdownSectionChunker`，按输入 section 顺序处理正文，优先把相邻段落合并到长度上限，单个段落超过上限时再做严格定长切分。空正文 section 不产生 chunk，第一版不增加 overlap、tokenizer、Markdown AST 或外部依赖。

**Tech Stack:** Java 17、JUnit 5、Maven。

## Global Constraints

- 所有写入仅发生在 `D:\IdeaProjects\java-interview-rag`，不修改只读参考项目。
- 不新增 Maven 依赖，不实现文件导入、持久化、向量化、overlap 或 token 级切分。
- 新增异常信息和必要代码注释使用简体中文。
- 验证仅覆盖 chunk 切分、现有 Markdown 模型与解析器；不执行无关的大范围测试。

---

### Task 1: 用失败测试固定 chunk 边界与元数据

**Files:**
- Create: `src/test/java/com/javainterview/rag/knowledge/document/MarkdownSectionChunkerTest.java`

**Interfaces:**
- Consumes: `MarkdownSection(String source, int order, int headingLevel, List<String> headingPath, String content)`。
- Produces expectation: `MarkdownSectionChunker.chunk(List<MarkdownSection> sections, int maxLength)` 返回有序、不可修改的 `List<MarkdownChunk>`。

- [x] **Step 1: 编写段落合并、长度边界、超长段落和空正文测试**

  测试断言：相邻段落仅在加入分隔空行后不超过上限时合并；超过上限的单段按最大字符数连续切开；chunk 保留来源、section 顺序、标题级别和标题路径；全局顺序从 0 连续递增；空正文不产生 chunk。

- [x] **Step 2: 运行 chunk 测试并确认 RED**

  Run: `mvnw.cmd -Dtest=MarkdownSectionChunkerTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test`

  Expected: 测试编译失败，原因是 `MarkdownChunk` 和 `MarkdownSectionChunker` 尚不存在。

### Task 2: 实现最小 chunk 模型与切分器

**Files:**
- Create: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownChunk.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownSectionChunker.java`

**Interfaces:**
- `MarkdownChunk(String source, int order, int sectionOrder, int headingLevel, List<String> headingPath, String content)`：不可变 chunk，构造时复制标题路径并校验顺序、标题级别与正文。
- `List<MarkdownChunk> MarkdownSectionChunker.chunk(List<MarkdownSection> sections, int maxLength)`：保留输入顺序，按最大字符数返回不可修改列表。

- [x] **Step 1: 新增不可变 chunk 模型**

  使用 Java 17 `record` 保存 `source`、从 0 开始的全局 `order`、原 `sectionOrder`、`headingLevel`、`headingPath` 和非空白 `content`，并使用 `List.copyOf` 隔离外部修改。

- [x] **Step 2: 新增段落优先切分器**

  统一正文换行后，用一个或多个空白行识别段落边界；相邻段落以两个换行符连接，合并后不超过 `maxLength` 才放入同一 chunk；空正文直接跳过。

- [x] **Step 3: 对超长单段执行严格定长切分**

  在超长段落前先输出已累积内容，再按 `maxLength` 连续切分单段，保证每个 chunk 正文长度不超过上限且内容顺序不变；不增加 overlap。

- [x] **Step 4: 运行 chunk 测试并确认 GREEN**

  Run: `mvnw.cmd -Dtest=MarkdownSectionChunkerTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test`

  Expected: chunk 测试全部通过，0 失败、0 错误。

### Task 3: 同步项目事实并闭环验证

**Files:**
- Modify: `README.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`
- Modify: `docs/superpowers/plans/2026-07-20-markdown-section-chunker.md`

- [x] **Step 1: 同步当前能力、开发记录与字符级切分决策**

  只记录实际完成的段落优先、严格最大字符数和元数据追踪能力，明确尚未实现 overlap、token 级预算、导入登记和持久化。

- [x] **Step 2: 运行直接相关测试**

  Run: `mvnw.cmd -Dtest=MarkdownSectionChunkerTest,MarkdownHeadingParserTest,MarkdownDocumentTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test`

  Expected: 全部相关测试通过，0 失败、0 错误。

- [x] **Step 3: 检查并提交**

  Run: `git diff --check`、`git status --short`、`git diff --stat`。

  Expected: 只有本轮代码、测试、计划和固定进度文档发生变化；随后使用 `feat: 实现 Markdown 段落切分` 提交。
