# 真实简历 PDF 与证据审计验证 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 用一份用户授权的真实校招 PDF 复现并修复右对齐日期错序，再以修复后的正文完成一次生产简历证据审计，形成不含个人信息的真实质量结论。

**Architecture:** 保持现有 Java/PDFBox 与单次 Spring AI Structured Output 主链路。生产修复只在 `PdfDocumentAnalyzer` 中把轻微垂直偏移的文本段归入同一视觉行并按横坐标排序；原始 PDF、提取正文和完整审计结果只作为临时检查材料，永久结果仅保存计数、版本、token、耗时和人工复核结论。

**Tech Stack:** Java 17、Microsoft JDK 21、PDFBox 3、JUnit 5、Spring AI 1.0、DashScope OpenAI-compatible `qwen3.7-plus`

**Spec:** `docs/04-当前进度.md`、`docs/09-后续开发路线.md` 与用户在 2026-08-24 授权的真实 PDF 完整验证要求

## Global Constraints

- 所有项目写入仅发生在 `D:\IdeaProjects\java-interview-rag`，外部真实 PDF 只读。
- 不提交姓名、联系方式、原始 PDF、提取正文、完整事实、证据、风险或建议。
- 新增或修改的代码注释、日志、异常和用户可见提示使用简体中文。
- 不引入 OCR、Python 主解析链路、图工作流、多 Agent 或新中间件。
- 使用仓库内 `.m2`、`.tmp`，JDK 21 编译 Java 17 目标，只运行直接相关测试。

---

### Task 1: 轻微错位的右侧元数据排序

**Files:**
- Modify: `src/test/java/com/javainterview/rag/interview/document/PdfDocumentAnalyzerTest.java`
- Modify: `src/main/java/com/javainterview/rag/interview/document/PdfDocumentAnalyzer.java`

**Interfaces:**
- Consumes: `PdfDocumentAnalyzer.analyze(PDDocument)` 与现有 `PdfPageAnalysis.content()`。
- Produces: 同一视觉行内的左侧主体先于右侧日期/地点输出，`PdfPageLayout` 契约不变。

- [x] **Step 1: 写入失败测试**

在 `PdfDocumentAnalyzerTest` 生成两行脱敏经历：左侧主体使用 12pt、右侧日期使用 9pt 并向上偏移 3.5pt；断言仍为 `SINGLE_COLUMN`，且每行顺序均为主体后接日期。

- [x] **Step 2: 运行测试并确认失败**

```powershell
.\mvnw.cmd '-Dtest=PdfDocumentAnalyzerTest' '-Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository' test
```

预期新测试在修改生产代码前因日期先于主体而失败。

- [x] **Step 3: 实现最小视觉行排序**

在 `PdfDocumentAnalyzer.buildLines` 已生成 `TextLine` 后，将纵坐标差不超过 4pt 的段归为同一视觉行；视觉行按平均纵坐标排序，行内按 `xStart` 排序。保留现有字符聚合、栏间大间隙拆段和双栏识别逻辑。

- [x] **Step 4: 运行 PDF 聚焦回归**

```powershell
.\mvnw.cmd '-Dtest=PdfDocumentAnalyzerTest,PdfInterviewDocumentTextExtractorTest,PdfExtractionBaselineTest' '-Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository' test
```

预期新增错位日期测试通过，既有单栏、双栏、日期侧标、复杂侧栏、混合和无文本样本全部不退化。

### Task 2: 真实 PDF 重解析与视觉复核

**Files:**
- Create temporarily: `src/test/java/com/javainterview/rag/interview/document/RealResumePdfInspectionTest.java`
- Write temporarily: `.tmp/real-resume-validation/*`

**Interfaces:**
- Consumes: 外部只读 PDF 路径与修复后的 `PdfInterviewDocumentTextExtractor.extract(MultipartFile)`。
- Produces: 临时页面 PNG、提取正文和无正文元数据，用于人工核对后全部删除。

- [x] **Step 1: 使用生产解析器重提取并渲染全部页面**

一次性测试通过系统属性读取外部 PDF，输出页数、文本页、图像页、OCR 候选、重排页和字符数；使用 PDFBox `PDFRenderer` 在 `.tmp` 生成 PNG。

- [x] **Step 2: 核对真实顺序**

目检 PNG 与正文，确认教育、工作和两个项目均满足“主体名称先于对应日期/地点”，大章节、技术栈、职责与量化指标仍可读。

- [x] **Step 3: 删除隐私材料**

删除一次性测试源码、PNG、提取正文、元数据、Surefire 报告和临时编译类；确认 Git 只剩脱敏代码、测试和文档改动。

### Task 3: 生产审计官真实单样本验证

**Files:**
- Create temporarily: `src/test/java/com/javainterview/rag/knowledge/web/AuthorizedRealResumeAuditTest.java`
- Create: `evaluation/results/2026-08-24-real-resume-audit-summary.json`
- Modify: `evaluation/README.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`

**Interfaces:**
- Consumes: 修复后提取正文、生产 `DashScopeCompatibleChatConfiguration`、`ResumeEvidenceAuditService` 和用户授权的 1 次真实 Chat 调用。
- Produces: 临时完整审计结果，以及永久无正文报告中的角色/Prompt/模型版本、事实类型计数、候选/合法证据计数、缺失/风险/建议数量、token、耗时和人工复核结论。

- [x] **Step 1: 执行一次真实审计**

从只读参考配置加载 API Key 到进程环境，使用生产 ChatModel 与内存日志存储调用 `ResumeEvidenceAuditService.audit`；不启动数据库、Qdrant 或 Web 服务。

- [x] **Step 2: 人工复核临时结果**

逐项检查事实引文属于修复后的正文，项目与时间是否正确关联，明确量化结果是否召回，技能清单是否被误当成项目事实，以及缺失/风险是否符合校招简历现状。

- [x] **Step 3: 固化脱敏结论并清理正文**

永久 JSON 不写入个人信息或模型正文；删除临时完整结果、正文、测试源码、编译类和测试报告。

- [x] **Step 4: 更新事实文档并提交**

更新当前进度、开发记录、技术决策和评估说明；检查 staged snapshot、`git diff --cached --check` 与 `git show --check`，只提交本闭环文件。
