# Markdown 标题解析实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `MarkdownDocument` 解析为可供后续 chunk 切分使用的有序 section，并保留来源、标题层级和正文段落顺序。

**Architecture:** 使用不依赖第三方库的逐行扫描器识别 ATX 标题，通过标题栈生成路径，并将标题之间的正文聚合为不可变 `MarkdownSection`。扫描器跟踪反引号和波浪线围栏代码块，避免把代码示例中的 `#` 误判为标题。

**Tech Stack:** Java 17、JUnit 5、Maven。

## Global Constraints

- 所有写入仅发生在 `D:\IdeaProjects\java-interview-rag`，不修改只读参考项目。
- 不新增 Maven 依赖，不实现文件导入、持久化或 chunk 最大长度切分。
- 新增异常信息和必要代码注释使用简体中文。
- 验证仅覆盖标题解析器、现有文档模型和 Spring Boot 上下文测试。

---

### Task 1: 用失败测试固定标题解析行为

**Files:**
- Create: `src/test/java/com/javainterview/rag/knowledge/document/MarkdownHeadingParserTest.java`

**Interfaces:**
- Consumes: `MarkdownDocument.create(String source, String content)`。
- Produces expectation: `MarkdownHeadingParser.parse(MarkdownDocument)` 返回有序 `List<MarkdownSection>`。

- [x] **Step 1: 编写层级、正文顺序、空正文和代码围栏测试**

  测试应断言 section 的 `source()`、`order()`、`headingLevel()`、`headingPath()` 和 `content()`，并覆盖前言、同级/跨级标题、无正文标题、空文档，以及围栏代码块中的伪标题。

- [x] **Step 2: 运行解析器测试并确认 RED**

  Run: `mvnw.cmd -Dtest=MarkdownHeadingParserTest test`

  Expected: 测试编译失败，原因是 `MarkdownHeadingParser` 和 `MarkdownSection` 尚不存在。

### Task 2: 实现最小标题解析器

**Files:**
- Create: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownSection.java`
- Create: `src/main/java/com/javainterview/rag/knowledge/document/MarkdownHeadingParser.java`

**Interfaces:**
- `MarkdownSection(String source, int order, int headingLevel, List<String> headingPath, String content)`：不可变 section，构造时复制标题路径。
- `List<MarkdownSection> MarkdownHeadingParser.parse(MarkdownDocument document)`：按原文顺序返回不可修改列表。

- [x] **Step 1: 新增不可变 section 模型**

  使用 Java 17 `record` 保存来源、从 0 开始的顺序、0 到 6 的标题级别、标题路径和正文；校验来源、顺序、级别与正文非空引用，并用 `List.copyOf` 防止外部修改路径。

- [x] **Step 2: 新增逐行标题扫描器**

  统一换行后逐行扫描：识别最多三个前导空格的 1 到 6 级 ATX 标题；用栈维护父级路径；正文只裁剪 section 边界空行，内部行和空行保持顺序；标题没有正文时仍生成 section；纯空白且没有标题时返回空列表。

- [x] **Step 3: 忽略围栏代码块内的伪标题**

  跟踪至少三个反引号或波浪线组成的围栏，只有同类且长度不短于开始标记的独立结束行才关闭围栏；围栏内所有内容归入当前 section 正文。

- [x] **Step 4: 运行解析器测试并确认 GREEN**

  Run: `mvnw.cmd -Dtest=MarkdownHeadingParserTest test`

  Expected: 解析器测试全部通过，0 失败、0 错误。

### Task 3: 同步项目事实并闭环验证

**Files:**
- Modify: `README.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`
- Modify: `docs/superpowers/plans/2026-07-19-markdown-heading-parser.md`

- [x] **Step 1: 同步当前进度、追加开发记录并记录解析策略**

  只把实际完成的 ATX 标题解析能力写入文档，明确尚未实现文件导入、Setext 标题和 chunk 长度切分。

- [x] **Step 2: 运行直接相关测试**

  Run: `mvnw.cmd -Dtest=MarkdownHeadingParserTest,MarkdownDocumentTest,JavaInterviewRagApplicationTests test`

  Expected: 全部相关测试通过，0 失败、0 错误。

- [x] **Step 3: 检查并提交**

  Run: `git diff --check`、`git status --short`、`git diff --stat`。

  Expected: 只有本轮代码、测试、计划和固定进度文档发生变化；随后使用 `feat: 实现 Markdown 标题解析` 提交。
