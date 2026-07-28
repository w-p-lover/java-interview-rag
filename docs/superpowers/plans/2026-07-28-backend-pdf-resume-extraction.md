# 后端 PDF 简历正文提取 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 使用 Java 后端一次性提取文本型 PDF 简历或 JD 的正文，并继续复用既有文本面试接口。

**Architecture:** 新增一个仅处理临时 multipart 文件的通用正文提取边界，当前只由 PDFBox 实现 PDF 提取。前端对 PDF 调用该接口、对 Markdown/TXT 保持浏览器本地读取；提取后的字符串仍进入原有 `resumeText` 和 `jobDescription` 请求，不保存原文件。

**Tech Stack:** Java 17、Spring MVC、Apache PDFBox 3.0.5、JUnit 5、MockMvc、Vue 3、TypeScript、Axios、Vite。

## Global Constraints

- PDF 单文件最大 10 MiB；multipart 总请求允许 11 MiB，应用不持久化原文件。
- 当前只实现文本层 PDF；OCR、DOCX、对象存储、数据库迁移和向量化不在范围内。
- `application.yml` 的用户端口改动必须保持工作区状态，不能被本功能提交覆盖或暂存。
- 新增或修改的代码注释、异常与用户可见文字使用简体中文。
- 不修改 `POST /api/interviews/sessions/start` 及其 JSON 字段。

---

### Task 1: 后端通用正文提取边界与 PDFBox 实现

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/resources/application.yml`
- Create: `src/main/java/com/javainterview/rag/interview/document/InterviewDocumentTextExtractor.java`
- Create: `src/main/java/com/javainterview/rag/interview/document/InterviewDocumentExtraction.java`
- Create: `src/main/java/com/javainterview/rag/interview/document/PdfInterviewDocumentTextExtractor.java`
- Test: `src/test/java/com/javainterview/rag/interview/document/PdfInterviewDocumentTextExtractorTest.java`

**Interfaces:**
- Consumes: `MultipartFile` 的文件名、大小和字节流。
- Produces: `InterviewDocumentTextExtractor.extract(MultipartFile): InterviewDocumentExtraction`。
- Produces: `InterviewDocumentExtraction(String content, String format, int pageCount)`，当前 `format` 固定为 `"PDF"`。

- [ ] **Step 1: 为 PDFBox 和 multipart 边界写失败测试**

创建 `PdfInterviewDocumentTextExtractorTest`，用 PDFBox 在测试内存中生成两页文本 PDF 与空白页 PDF；覆盖成功文本、空文件、非 PDF、超过 10 MiB 与无文本层错误。

```java
assertEquals("PDF", extractor.extract(pdf("resume.pdf", twoPagePdf())).format());
assertEquals(2, extractor.extract(pdf("resume.pdf", twoPagePdf())).pageCount());
assertThrows(IllegalArgumentException.class,
        () -> extractor.extract(pdf("resume.pdf", new byte[0])));
assertEquals("未提取到可用文字，可能是扫描件；当前暂不支持 OCR。",
        assertThrows(IllegalArgumentException.class,
                () -> extractor.extract(pdf("resume.pdf", blankPdf()))).getMessage());
```

- [ ] **Step 2: 运行测试确认当前不存在实现**

Run:

```powershell
.\mvnw.cmd -Dtest=PdfInterviewDocumentTextExtractorTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test
```

Expected: 编译失败，提示 `PdfInterviewDocumentTextExtractor` 或 PDFBox 类型不存在。

- [ ] **Step 3: 添加最小 PDFBox 依赖、配置和实现**

在 `pom.xml` 属性中固定版本，并增加唯一解析依赖：

```xml
<pdfbox.version>3.0.5</pdfbox.version>

<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>${pdfbox.version}</version>
</dependency>
```

在现有 `spring:` 下新增 multipart 限制，保留用户现有 `server.port` 行不变：

```yaml
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 11MB
      file-size-threshold: 10MB
```

定义只包含当前必要操作的接口和结果类型：

```java
public interface InterviewDocumentTextExtractor {
    InterviewDocumentExtraction extract(MultipartFile file) throws IOException;
}

public record InterviewDocumentExtraction(String content, String format, int pageCount) {
}
```

实现 `PdfInterviewDocumentTextExtractor`：先检查空文件、`10 * 1024 * 1024` 字节和 `.pdf` 扩展名，再用 `Loader.loadPDF(file.getBytes())` 打开文档。创建 `PDFTextStripper` 后执行 `setSortByPosition(true)`、`setPageSeparator("\\n\\n")` 和 `getText(document)`；空白结果抛出扫描件提示，`InvalidPasswordException` 转为加密提示，其他 `IOException` 转为损坏提示。使用 try-with-resources 关闭 `PDDocument`，不写文件、数据库或向量库。

- [ ] **Step 4: 运行解析器测试**

Run:

```powershell
.\mvnw.cmd -Dtest=PdfInterviewDocumentTextExtractorTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test
```

Expected: 所有解析器测试通过；不启动模型、MySQL、Qdrant 或 OCR。

- [ ] **Step 5: 提交后端解析器**

```powershell
git add pom.xml src/main/resources/application.yml src/main/java/com/javainterview/rag/interview/document src/test/java/com/javainterview/rag/interview/document
git commit -m "feat: 增加 PDF 正文提取工具"
```

提交前确认暂存区不包含 `application.yml` 的端口 hunk；只暂存本任务新增的 multipart 配置 hunk。

### Task 2: 暴露临时解析接口并接入 Vue 文件字段

**Files:**
- Create: `src/main/java/com/javainterview/rag/interview/web/InterviewDocumentController.java`
- Test: `src/test/java/com/javainterview/rag/interview/web/InterviewDocumentControllerTest.java`
- Modify: `frontend/src/api/interview.ts`
- Modify: `frontend/src/utils/interviewDocumentText.ts`
- Modify: `frontend/src/components/interview/InterviewTextFileField.vue`
- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`

**Interfaces:**
- Consumes: `POST /api/interviews/documents/extract`，multipart 参数名为 `file`。
- Produces: `{ "content": string, "format": "PDF", "pageCount": number }`。
- Consumes: `extractInterviewDocumentText(file: File)`。
- Produces: 现有 `InterviewDocumentExtraction`，让 `InterviewTextFileField` 继续通过 `v-model` 回填正文。

- [ ] **Step 1: 为 multipart 控制器写失败测试**

使用 `MockMvcBuilders.standaloneSetup` 和 mock `InterviewDocumentTextExtractor`，覆盖成功响应和服务抛出 `IllegalArgumentException` 时的中文 400 响应。

```java
mockMvc.perform(multipart("/api/interviews/documents/extract")
                .file(new MockMultipartFile("file", "resume.pdf", "application/pdf", "PDF".getBytes())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value("Java 后端项目经历"))
        .andExpect(jsonPath("$.format").value("PDF"))
        .andExpect(jsonPath("$.pageCount").value(2));
```

- [ ] **Step 2: 运行控制器测试确认当前接口不存在**

Run:

```powershell
.\mvnw.cmd -Dtest=InterviewDocumentControllerTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test
```

Expected: 编译失败，提示 `InterviewDocumentController` 不存在。

- [ ] **Step 3: 实现控制器与前端 API 切换**

控制器应保持无 `storage` profile 限制，使本地解析不依赖模型或数据库：

```java
@RestController
@RequestMapping("/api/interviews/documents")
public class InterviewDocumentController {
    @PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public InterviewDocumentExtraction extract(@RequestParam MultipartFile file) throws IOException {
        return textExtractor.extract(file);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }
}
```

在 `frontend/src/api/interview.ts` 新增 `extractInterviewPdfText(file)`：创建 `FormData`、以 `file` 字段提交到 `/interviews/documents/extract`，并返回接口响应。`interviewDocumentText.ts` 保留 Markdown/TXT 的 `file.text()` 分支，将 `.pdf` 分支改为该 API；删除 PDF.js worker、动态 import、文本项归一化和浏览器端 10 MiB 常量。保留前端 `.pdf` 扩展名校验，并使用后端的中文错误。

`InterviewTextFileField.vue` 保持现有 `<script setup lang="ts">` 和单一 `v-model` 契约，只将读取中文案改为“正在提取正文”，将默认说明改为“PDF 仅在本次请求中发送到后端提取，不会进入资料库或保存”。成功页数与错误时保留原正文的行为不变。

移除 `pdfjs-dist`：

```powershell
Set-Location frontend
npm uninstall pdfjs-dist
```

- [ ] **Step 4: 运行后端接口测试与前端构建**

Run:

```powershell
Set-Location D:\IdeaProjects\java-interview-rag
.\mvnw.cmd -Dtest=PdfInterviewDocumentTextExtractorTest,InterviewDocumentControllerTest -Dmaven.repo.local=D:\IdeaProjects\java-interview-rag\.m2\repository -Djava.io.tmpdir=D:\IdeaProjects\java-interview-rag\.tmp test
Set-Location frontend
npm run build
```

Expected: 两个直接相关的后端测试通过，Vite 构建通过且产物不再包含 PDF.js worker。

- [ ] **Step 5: 浏览器验证并提交接口与前端迁移**

在本地后端运行时，通过 `/interview` 选择文本型 PDF，确认返回正文和页数；选择扫描件确认中文 OCR 暂不支持错误并保留旧正文；再选择 Markdown/TXT 确认仍可本地回填。不要点击“开始模拟面试”，避免模型与数据库调用。

```powershell
git add src/main/java/com/javainterview/rag/interview/web src/test/java/com/javainterview/rag/interview/web frontend/src/api/interview.ts frontend/src/utils/interviewDocumentText.ts frontend/src/components/interview/InterviewTextFileField.vue frontend/package.json frontend/package-lock.json
git commit -m "feat: 接入后端 PDF 简历解析"
```

### Task 3: 同步项目文档与完成闭环

**Files:**
- Modify: `README.md`
- Modify: `docs/01-需求边界.md`
- Modify: `docs/02-技术方案.md`
- Modify: `docs/04-当前进度.md`
- Modify: `docs/05-开发记录.md`
- Modify: `docs/07-技术决策记录.md`
- Modify: `docs/superpowers/plans/2026-07-28-backend-pdf-resume-extraction.md`

**Interfaces:**
- Documents describe the implemented `POST /api/interviews/documents/extract` boundary and do not claim OCR or persistent file storage.

- [ ] **Step 1: 替换已过时的浏览器 PDF.js 描述**

将 README、需求边界、技术方案和当前进度从“浏览器本地 PDF.js 解析”改为“后端 PDFBox 临时解析”；Markdown/TXT 本地读取、PDF 不持久化、扫描件 OCR 暂不支持、DOCX 暂不开放必须保留。

- [ ] **Step 2: 追加事实记录与新 ADR**

在 `docs/05-开发记录.md` 追加本轮实现、PDFBox 版本、实际测试和浏览器验证结果。在 `docs/07-技术决策记录.md` 追加一个明确替代 ADR-035 的决策，说明改用服务端位置排序的理由、multipart 临时缓冲边界及复杂版式仍非完全保证。

- [ ] **Step 3: 勾选计划并审查提交范围**

将本计划的完成项改为 `[x]`，执行：

```powershell
git diff --check
git status --short --branch
git diff --stat
git diff -- src/main/resources/application.yml
```

Expected: diff 无空白错误，`application.yml` 的 `server.port` 用户改动仍不在暂存区。

- [ ] **Step 4: 提交文档闭环**

```powershell
git add README.md docs/01-需求边界.md docs/02-技术方案.md docs/04-当前进度.md docs/05-开发记录.md docs/07-技术决策记录.md docs/superpowers/plans/2026-07-28-backend-pdf-resume-extraction.md
git diff --cached --check
git commit -m "docs: 记录后端 PDF 解析边界"
```
