# Phase9-PDF章节结构化Chunk

## 升级内容

- PDF 页面分析新增行级字号、字重、等宽字体、坐标和视觉行分段证据。
- 知识资料解析过滤重复页眉页脚和独立页码，识别标题、正文、列表、代码与表格，并让正文跨页继承真实章节。
- PDF 结构块使用真实逻辑来源进入版本登记、章节目录、Chunk、Qdrant 和重建链路；Markdown 原解析路径不变。
- `page_number` 与 `block_type` 贯穿导入响应、检索结果、相邻上下文、回答提示和召回日志，前端导入结果可直接核对。

## 直接相关文件

- `src/main/java/com/javainterview/rag/interview/document/PdfDocumentAnalyzer.java`
- `src/main/java/com/javainterview/rag/interview/document/PdfTextLineAnalysis.java`
- `src/main/java/com/javainterview/rag/knowledge/document/PdfKnowledgeStructureParser.java`
- `src/main/java/com/javainterview/rag/knowledge/document/MarkdownContentImporter.java`
- `src/main/java/com/javainterview/rag/knowledge/document/KnowledgeDocumentOutlineBuilder.java`
- `src/main/java/com/javainterview/rag/knowledge/vector/QdrantMarkdownChunkIndexer.java`
- `src/main/java/com/javainterview/rag/knowledge/vector/MarkdownSimilaritySearchService.java`
- `src/main/java/com/javainterview/rag/knowledge/rag/RagAnswerService.java`
- `frontend/src/api/knowledge.ts`
- `frontend/src/components/ChunkLedger.vue`

## 验证

- 两页确定性 PDF 覆盖重复页眉、独立页码、一级/二级标题、跨页正文、列表、等宽代码和三行表格。
- 9 个直接相关测试类共 41 个测试通过；前端 TypeScript 检查与 Vite 生产构建通过；Maven 跳过测试打包通过。
- 一次新 PDF 的真实 Embedding 校验生成 6 个结构 Chunk；代码查询首位命中，相似度 `0.8586`，返回 `1. HashMap`、第 1 页和 `CODE`。没有调用 Chat，验证来源随后删除。

## 边界与下一步

- 当前能力是面向 RAG 的确定性版面感知结构识别，不是完整文档理解。
- 双栏页面不启用表格判断，以避免把并排正文误报为表格；合并单元格、跨页表格、图片语义和 OCR 尚未支持。
- 下一闭环进入结构感知检索：用固定多块问题验证章节路径、页码和块类型能否改善召回与引用，再决定是否增加章节聚合或轻量文档结构图。
