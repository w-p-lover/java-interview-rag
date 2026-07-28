# 后端 PDF 简历正文提取设计

## 目标

让 `/interview` 的简历和可选 JD 使用 Java 后端提取带文本层 PDF 的正文，改善复杂版式 PDF 的可治理性，同时继续复用既有 `resumeText` 和 `jobDescription` 面试契约。

## 已确认范围

- PDF 在提交给面试前上传到本项目后端解析一次，解析结果回填到前端可编辑文本框。
- 应用不保存原 PDF、不建表、不写 MySQL、知识库、向量库或浏览器 localStorage。
- 只支持带文本层的 PDF；扫描件 OCR、DOCX 和长期简历档案不在本轮范围。
- Markdown、Markdown 扩展名和 TXT 继续在浏览器本地以 `File.text()` 读取，避免不必要地上传纯文本。

## 方案选择

### 方案 A：后端 PDFBox 解析（采用）

- 新增 `POST /api/interviews/documents/extract` multipart 接口，只接收一个 PDF 文件。
- 使用 Apache PDFBox 的 `PDFTextStripper` 并启用 `setSortByPosition(true)`，按页面读取正文，返回字符内容和总页数。
- 前端仅将 PDF 交给该接口，Markdown/TXT 的读取路径不变；解析成功后仍通过既有 JSON 面试接口提交正文。

这一方案让解析策略、PDFBox 版本和回归样本集中在 Java 项目中。位置排序能改善常见双栏和定位文本的阅读顺序，但不能承诺完全恢复任意复杂表格或图形化简历的语义结构。

### 方案 B：继续在浏览器增强 PDF.js（不采用）

可以补充坐标排序，但解析策略仍分散在浏览器侧，且很难为真实 PDF 样本建立与后端行为一致的回归边界。

### 方案 C：同时引入 OCR（不采用）

OCR 需要单独处理图片预处理、语言模型、准确率样本、资源成本与隐私边界。用户已明确暂缓，本轮在扫描件无文本时只返回中文错误。

## 接口与数据流

```text
PDF 文件
  -> POST /api/interviews/documents/extract (multipart)
  -> InterviewPdfTextExtractor (PDFBox，位置排序)
  -> { content, format: "PDF", pageCount }
  -> 前端可编辑简历/JD 文本框
  -> 既有 POST /api/interviews/sessions/start JSON 请求
```

- `InterviewDocumentController` 只处理 multipart 绑定、HTTP 响应和中文参数错误。
- `InterviewPdfTextExtractor` 只负责校验、打开 PDF、按位置提取文本和关闭 PDFBox 资源。
- `InterviewDocumentExtraction` 表达提取结果，不包含原文件字节、路径或持久化标识。
- 前端 `interview.ts` 新增上传 API；`interviewDocumentText.ts` 保留统一入口，但 PDF 分支改为调用后端，文本分支保持本地读取。

## 校验与隐私边界

- PDF 文件最大 10 MiB；服务层再次校验大小，Spring multipart 限制作为入口保护。
- 要求文件名扩展名为 `.pdf`，并由 PDFBox 验证实际文件结构；空文件、加密、损坏和无文本层均返回明确中文错误。
- 提取正文仍受既有简历 50,000 字符、JD 30,000 字符上限约束；前端保留原有正文，解析失败不覆盖它。
- 解析后立即关闭 `PDDocument`，不由业务代码创建持久化文件、数据库记录或知识库导入。Servlet multipart 实现可能在请求生命周期内使用框架临时缓冲；该临时缓冲不作为业务文件存储。

## 依赖与配置

- Maven 增加 Apache PDFBox `3.0.5` 的单一依赖，版本由项目属性显式固定。
- 设置 Spring multipart 单文件限制为 10 MiB，请求限制仅预留 multipart 元数据空间，并设置 10 MiB 内存阈值，避免应用主动将合规 PDF 写入业务临时目录。
- 删除前端 `pdfjs-dist` 依赖和 worker 引用，避免保留两个 PDF 解析器。

## 错误响应

| 场景 | HTTP | 前端提示 |
| --- | --- | --- |
| 非 PDF、空文件、超过大小、正文超限 | 400 | 返回后端的具体中文校验信息 |
| 加密 PDF | 400 | PDF 已加密，当前无法提取正文，请先解除密码保护 |
| 损坏或非 PDF 二进制 | 400 | PDF 解析失败，请确认文件完整 |
| 无文本层 | 400 | 未提取到可用文字，可能是扫描件；当前暂不支持 OCR |

## 验收与验证

- 单元测试覆盖有效两页 PDF、按页面分隔、空文件、非 PDF、超过 10 MiB、加密或损坏文件，以及无文本层 PDF。
- MVC 测试覆盖 multipart 成功响应和中文 400 错误体，确认接口不依赖模型、数据库、Qdrant 或知识库。
- 生成并使用一个双栏文本 PDF 作为位置排序回归样本，重点验证两栏内容不会交叉为单个词序列；该样本仅用于测试，完成后不保留真实个人资料。
- 执行针对性 Maven 测试和 `frontend npm run build`；浏览器验证 PDF 成功回填、失败保留原文本以及 Markdown/TXT 回归。

## 不做的事

- 不保证所有复杂版式、表格、图标或图片型 PDF 的阅读顺序完全正确。
- 不实现 OCR、DOCX、病毒扫描、长期文件归档、对象存储、数据库迁移或向量化。
- 不修改既有面试创建、画像、出题、评分和会话恢复接口。
