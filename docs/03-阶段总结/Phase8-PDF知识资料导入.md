# Phase8-PDF知识资料导入

## 升级内容

- 知识库上传接口支持文本型 PDF，并继续复用 Markdown 的版本登记、Chunk 切分和 Qdrant 索引。
- 复用现有 PDFBox 页面分析，保留页码、双栏调整和 OCR 候选告警。
- 导入响应和前端结果账页展示 PDF 格式、页数和解析提醒。
- Qdrant metadata 增加 `document_format` 与 `page_number`，为下一阶段页面/章节结构化检索提供数据基础。

## 直接相关文件

- `src/main/java/com/javainterview/rag/interview/document/PdfDocumentAnalyzer.java`
- `src/main/java/com/javainterview/rag/interview/document/PdfPageAnalysis.java`
- `src/main/java/com/javainterview/rag/knowledge/document/PdfKnowledgeDocumentExtractor.java`
- `src/main/java/com/javainterview/rag/knowledge/document/PdfKnowledgeDocumentExtraction.java`
- `src/main/java/com/javainterview/rag/knowledge/document/PdfKnowledgePage.java`
- `src/main/java/com/javainterview/rag/knowledge/web/MarkdownDocumentController.java`
- `frontend/src/components/ImportPanel.vue`
- `frontend/src/components/ChunkLedger.vue`

## 验证

- 后端 PDF 提取、知识导入接口、Qdrant metadata、相邻扩展和 RAG 回答相关测试共 27 个通过。
- 前端 `vue-tsc --noEmit` 和 Vite 生产构建通过。
- 独立 storage 实例真实导入两页合成 PDF，生成 2 个页码 Chunk；查询首位命中 PDF 第 1 页，相似度 `0.9413`，第 2 页同时进入 Top5。验证后已清理合成来源并停止实例。

## 边界与下一步

- 当前只保证文本型 PDF；整份扫描 PDF 拒绝，混合 PDF 返回告警并跳过无文本页。
- 当前页面标题是结构化页码标记，不等于完整文章章节识别。
- 下一阶段实现页面/章节结构化 Chunk，补充代码块、表格、列表和跨页章节关系，再以固定样本验证检索收益。
