# Markdown 文档版本实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 建立阶段 1 的最小 Markdown 文档模型和跨平台稳定的内容版本标识。

**Architecture:** 使用一个不可变 `MarkdownDocument` 保存来源、原始内容和创建时计算的版本。版本对换行符做统一后，按 UTF-8 内容计算 SHA-256；后续导入层以“来源 + 版本”判断同一来源是否重复导入。

**Tech Stack:** Java 17、JUnit 5、Maven。

## Global Constraints

- 只修改当前仓库内与本任务直接相关的代码、测试和进度文档。
- 不引入数据库、Markdown 解析库或新的 Maven 依赖。
- 新增异常信息使用简体中文，验证只覆盖文档模型和现有上下文测试。

---

### Task 1: Markdown 文档模型与版本

**Files:**
- Create: `src/test/java/com/javainterview/rag/knowledge/document/MarkdownDocumentTest.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownDocument.java`

**Interfaces:**
- Consumes: 来源字符串与 Markdown 原始内容。
- Produces: `MarkdownDocument.create(String source, String content)`，以及 `source()`、`content()`、`version()`。

- [x] **Step 1: 编写失败测试**

  覆盖来源和内容保留、SHA-256 固定结果、换行符归一化、内容变化和非法输入。

- [x] **Step 2: 确认测试因模型缺失而失败**

  Run: `mvnw.cmd -Dtest=MarkdownDocumentTest test`
  Expected: 测试编译失败，提示找不到 `MarkdownDocument`。

- [x] **Step 3: 编写最小实现**

  使用 Java 17 `MessageDigest`、`StandardCharsets.UTF_8` 与 `HexFormat` 计算版本，不新增第三方依赖。

- [x] **Step 4: 运行直接相关测试**

  Run: `mvnw.cmd -Dtest=MarkdownDocumentTest test`
  Expected: 文档模型测试全部通过。

### Task 2: 文档同步与闭环验证

**Files:**
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`

- [x] **Step 1: 更新当前进度、追加开发记录和版本策略 ADR**
- [x] **Step 2: 运行文档模型测试与上下文测试**
- [x] **Step 3: 检查 Git diff，只提交本轮直接相关文件**
