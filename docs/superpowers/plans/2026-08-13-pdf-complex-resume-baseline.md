# PDF Complex Resume Baseline Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用确定性、脱敏的复杂简历 PDF 样本验证并收紧当前 PDFBox 版面解析边界，使中文正文、日期侧标、无边框技能表和复杂侧栏都有可重复的质量结论。

**Architecture:** 继续复用 `PdfExtractionBaselineTest` 的现场生成与结构化报告机制，不保存真实简历、个人信息或字体文件。先扩展同一评估器，再只针对稳定复现的阅读顺序缺陷修改 `PdfDocumentAnalyzer`；OCR、表格结构化和外部服务不进入本轮。

**Tech Stack:** Java 17、PDFBox 3.0.5、JUnit 5、Jackson、Maven Wrapper。

## Global Constraints

- 所有写入只发生在 `D:\IdeaProjects\java-interview-rag`。
- `D:\IdeaProjects\blog\blog-springboot` 仅可只读参考。
- 不覆盖或提交当前工作区已有的前端、Docker、Hallmark 和本机配置改动。
- 新增或修改的代码注释、异常和用户提示默认使用简体中文。
- 不调用 Chat、Embedding、OCR、MySQL 或 Qdrant，不引入新的生产依赖。
- 只执行 PDF 解析直接相关的测试，不做无意义的全量验证。

---

### Task 1: 扩展复杂简历确定性样本

**Files:**
- Modify: `src/test/java/com/javainterview/rag/interview/document/PdfExtractionBaselineTest.java`
- Modify: `evaluation/pdf/README.md`

**Interfaces:**
- Consumes: `PdfInterviewDocumentTextExtractor#extract(MultipartFile)`。
- Produces: 新增 `chinese-resume`、`date-rail`、`skills-table` 和 `complex-sidebar` 四类评估结果。

- [x] **Step 1: 增加测试运行时中文字体加载**

```java
private PDFont loadChineseFont(PDDocument document) throws IOException {
    return PDType0Font.load(document, Path.of("C:/Windows/Fonts/simhei.ttf").toFile());
}
```

仅在 Windows 字体存在时生成中文样本；缺少字体的环境使用 JUnit assumption 明确跳过整条基线，不把环境缺失写成解析成功。

- [x] **Step 2: 生成四类脱敏样本**

```text
chinese-resume: 中文姓名占位、Java 后端、Spring AI、RAG 项目
date-rail: 左侧经历正文 + 右侧窄日期列，预期保持逐行单栏顺序
skills-table: 左右技能和值对齐但存在中部穿越标题，预期保守单栏
complex-sidebar: 左侧窄联系方式/技能侧栏 + 右侧宽经历，预期先侧栏后主栏
```

- [x] **Step 3: 先运行扩展基线并记录真实失败**

Run:

```shell
./mvnw.cmd -Dtest=PdfExtractionBaselineTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test
```

Expected: 新用例若暴露稳定顺序缺陷，报告比较或通过数断言失败；不得先修改预期迁就实现。

### Task 2: 收紧版面顺序算法

**Files:**
- Modify if required: `src/main/java/com/javainterview/rag/interview/document/PdfDocumentAnalyzer.java`
- Modify: `src/test/java/com/javainterview/rag/interview/document/PdfDocumentAnalyzerTest.java`

**Interfaces:**
- Consumes: `List<TextLine>` 与页面宽度。
- Produces: 保守的 `SINGLE_COLUMN` 或 `TWO_COLUMN` 页面正文顺序。

- [x] **Step 1: 检查新增布局是否暴露稳定失败**

```java
assertEquals(PdfPageLayout.SINGLE_COLUMN, result.layout());
assertOrder(result.content(), "经历一", "2022-2024", "经历二", "2020-2022");
```

或对真实宽侧栏用例断言 `TWO_COLUMN` 且 `侧栏一 -> 侧栏二 -> 主栏一 -> 主栏二`。实际四类新增样本均通过，没有触发生产算法修正。

- [x] **Step 2: 根据评估结果决定是否修正生产算法**

只调整已经由样本证明错误的列宽、连续行、垂直重叠或穿越内容判定；不增加通用表格识别器、坐标树或新的配置项。本轮没有失败证据，因此保持生产解析器不变。

- [x] **Step 3: 运行分析器与基线测试**

```shell
./mvnw.cmd -Dtest=PdfDocumentAnalyzerTest,PdfExtractionBaselineTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test
```

Expected: 所有原样本和新增样本通过，旧双栏修复不回归。

### Task 3: 固化报告、验证和事实文档

**Files:**
- Create: `evaluation/results/2026-08-13-pdf-complex-resume-baseline.json`
- Modify: `README.md`
- Modify: `docs/04-当前进度.md`
- Append: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`
- Modify if capability boundary changes: `docs/08-项目技术栈与功能总结.md`

**Interfaces:**
- Consumes: `target/pdf-extraction-baseline/report.json` 和生成的 fixture。
- Produces: 可审查的仓库报告、真实进度记录和单一 Git 提交。

- [x] **Step 1: 固化当次 JSON 报告**

将测试生成报告作为新的候选报告，不覆盖 `v1`、`v2` 历史结果；报告必须包含总用例数、通过数、关键短语命中率、阅读顺序和逐用例证据。

- [x] **Step 2: 执行直接相关验证**

```shell
./mvnw.cmd -Dtest=PdfDocumentAnalyzerTest,PdfInterviewDocumentTextExtractorTest,PdfExtractionBaselineTest,InterviewDocumentControllerTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test
```

Expected: PDF 相关测试全部通过；不运行前端构建、模型、数据库或向量库验证。

- [x] **Step 3: 检查生成 PDF**

使用 Poppler 将新增 fixture 渲染为 PNG，目视确认中文字体、侧栏、日期和技能表版式与测试定义一致；渲染产物只保留在 `target/`。

- [x] **Step 4: 更新事实文档并提交**

只记录实际样本数、通过数、算法边界、验证命令和未覆盖风险。精确暂存本轮文件，检查 staged snapshot 后提交：

```shell
git commit -m "test: 扩展复杂简历PDF解析基线"
```

不暂存用户已有的前端、Docker、Hallmark 或 `application.yml` 改动。
