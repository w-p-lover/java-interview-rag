# PDF Parsing Capability Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将当前“整份 PDF 提取纯文本”升级为具备逐页证据、双栏阅读顺序、混合页识别、质量提示和可持续评估的 Java PDF 解析能力。

**Architecture:** 继续使用 PDFBox 3.0.5。底层分析器一次加载文档，逐页收集 `TextPosition` 和实际绘制的图像，完成行重建、保守双栏识别与页面分类；上传编排层汇总文档类型、OCR 路由页和告警；现有前端文件字段消费结构化摘要，不改变面试会话正文契约。

**Tech Stack:** Java 17、Spring Boot 3.5、Apache PDFBox 3.0.5、JUnit 5、Vue 3.5、TypeScript 5.9、Vite 8。

## Global Constraints

- 所有项目写入只发生在 `D:\IdeaProjects\java-interview-rag`，`blog-springboot` 只读。
- 不覆盖或提交当前工作区已有的前端视觉、Docker、端口和 profile 改动。
- 新增注释、异常和用户提示默认使用简体中文。
- 不引入 Rust/Python 主运行时、付费模型、OCR 服务、数据库或向量库调用。
- 保留 10 MiB、扩展名、加密和损坏 PDF 的现有中文错误契约。

---

### Task 1: 页面证据和版面分析

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/document/PdfDocumentAnalyzer.java`
- Create: `src/main/java/com/javainterview/rag/interview/document/PdfPageAnalysis.java`
- Test: `src/test/java/com/javainterview/rag/interview/document/PdfDocumentAnalyzerTest.java`

**Interfaces:**
- Consumes: 已加载的 `PDDocument`。
- Produces: `PdfDocumentAnalyzer#analyze(PDDocument)`，返回按页排序的正文、文本字符数、图像数、页面内容类型和版面类型。

- [x] **Step 1:** 用 PDFBox 生成单栏、清晰双栏、跨栏标题、图像页和混合页测试输入。
- [x] **Step 2:** 实现 `TextPosition` 收集、坐标行聚合和缺失空格恢复。
- [x] **Step 3:** 仅在中间 gutter 清楚、左右均有连续文本行且垂直区间重叠时启用双栏顺序。
- [x] **Step 4:** 使用图形流统计实际绘制图像，并将页面分类为 `TEXT`、`TEXT_WITH_IMAGES`、`IMAGE_ONLY`、`EMPTY`。
- [x] **Step 5:** 运行 `PdfDocumentAnalyzerTest`，确认单栏不回归、双栏左后右、跨栏标题在前、混合/图像页分类正确。

### Task 2: 结构化上传结果和质量告警

**Files:**
- Modify: `src/main/java/com/javainterview/rag/interview/document/PdfInterviewDocumentTextExtractor.java`
- Modify: `src/main/java/com/javainterview/rag/interview/document/InterviewDocumentExtraction.java`
- Create: `src/main/java/com/javainterview/rag/interview/document/PdfDocumentType.java`
- Create: `src/main/java/com/javainterview/rag/interview/document/PdfPageContentType.java`
- Create: `src/main/java/com/javainterview/rag/interview/document/PdfPageLayout.java`
- Create: `src/main/java/com/javainterview/rag/interview/document/PdfPageExtraction.java`
- Test: `src/test/java/com/javainterview/rag/interview/document/PdfInterviewDocumentTextExtractorTest.java`
- Test: `src/test/java/com/javainterview/rag/interview/web/InterviewDocumentControllerTest.java`

**Interfaces:**
- Consumes: Task 1 的逐页分析结果。
- Produces: 兼容原 `content/format/pageCount` 的扩展 JSON，并增加文档类型、文本页数、图像页数、双栏调整页数、`pagesNeedingOcr`、告警和逐页摘要。

- [x] **Step 1:** 扩展不可变响应模型并保留三参数兼容构造器。
- [x] **Step 2:** 汇总 `TEXT_BASED`、`MIXED`、`IMAGE_BASED` 和 `EMPTY` 文档类型。
- [x] **Step 3:** 全文无文本时保持 400；部分页面无文本时返回已提取正文并明确列出 OCR 页，不把结果伪装成完整成功。
- [x] **Step 4:** 更新控制器契约测试，核对新增字段的 JSON 形状。

### Task 3: 面试文件入口质量反馈

**Files:**
- Modify: `frontend/src/api/interview.ts`
- Modify: `frontend/src/utils/interviewDocumentText.ts`
- Modify: `frontend/src/components/interview/InterviewTextFileField.vue`

**Interfaces:**
- Consumes: Task 2 的结构化 PDF 响应。
- Produces: 文件选择成功摘要中的版面调整信息，以及混合 PDF 的明确质量告警。

- [x] **Step 1:** 增加与后端枚举一致的 TypeScript 类型。
- [x] **Step 2:** 在现有组件局部状态保存摘要和告警，不改变 `v-model` 正文契约。
- [x] **Step 3:** 成功状态显示页数、字符数和双栏调整页数；存在 OCR 页时显示具体页码并提示用户检查或粘贴补充文本。
- [x] **Step 4:** 执行 Vue 类型检查与生产构建。

### Task 4: 扩展质量基线和事实文档

**Files:**
- Modify: `src/test/java/com/javainterview/rag/interview/document/PdfExtractionBaselineTest.java`
- Modify: `evaluation/results/2026-08-12-pdf-extraction-baseline.json`
- Modify: `evaluation/pdf/README.md`
- Modify: `docs/04-当前进度.md`
- Append: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`
- Modify: `docs/08-项目技术栈与功能总结.md`

**Interfaces:**
- Consumes: Tasks 1-3 的真实运行结果。
- Produces: 同一评估器下可比较的候选报告、渲染 QA 结果和项目事实记录。

- [x] **Step 1:** 将基线扩展为单栏、双栏、图像页和混合文档，记录页面分类与版面调整页数。
- [x] **Step 2:** 生成并结构化比较 `pdfbox-layout-aware-v2` 报告，确认阅读顺序改善且字符命中不下降。
- [x] **Step 3:** 使用 Poppler 渲染新增 fixture 并目视检查版式。
- [x] **Step 4:** 运行 PDF 相关后端测试和前端构建，不执行无关大范围测试。
- [x] **Step 5:** 更新事实文档，精确暂存本轮文件，检查 staged snapshot 后提交。

## Development Boundary After This Plan

本轮完成本地文本层 PDF 的结构化解析和质量反馈。后续发展顺序为：先加入脱敏真实中文简历、表格和复杂侧栏样本，再根据同一评估器结果增强版面算法；逐页 OCR 只在明确供应商、成本、隐私和人工确认策略后接入，不能由“检测到需要 OCR”自动触发外部调用。
