# Phase24-Dense 故障 Sparse 降级

## 升级目标

补齐“超时、降级与并发保护”方向的第一段真实运行闭环：当 Hybrid 检索的 Dense/Qdrant 通道不可用时，RAG 问答仍可受控切换到 Sparse-only，并明确告诉调用方本次回答使用了降级路径。

## 完成内容

- `MarkdownHybridSearchService` 捕获 Dense 运行时异常，继续调用现有 Sparse 检索，返回 `SPARSE_ONLY`、降级标记和异常类型；Sparse 失败仍向上抛出，不吞掉真实故障。
- `RagAnswerService` 将检索策略和降级状态贯穿同步回答、SSE 完成事件、来源核对和召回日志使用的回答结果；无可靠 Sparse 候选时仍直接拒答，不调用 Chat。
- 同步接口和前端问答页透传并展示降级状态，提示用户本次已切换为关键词检索。
- 保留旧结果对象构造函数，兼容原有 Dense-only 测试和调用点。

## 变更文件

- 后端：`MarkdownHybridSearchService`、`RagAnswerService`、`RagAnswerResult`、`RagAnswerStreamEvent`、`RagAnswerResponse`。
- 前端：`knowledge.ts`、`useKnowledgeAnswer.ts`、`KnowledgeAnswerPanel.vue`。
- 测试：Hybrid 服务、Hybrid Controller、RAG 服务、RAG Controller 相关回归。
- 路线与技术总结：`docs/04-当前进度.md`、`docs/08-项目技术栈与功能总结.md`、`docs/09-后续开发路线.md`。

## 验证结果

- Java 21 下四个相关测试类通过：`MarkdownHybridSearchServiceTest`、`MarkdownHybridSearchControllerTest`、`RagAnswerServiceTest`、`RagAnswerControllerTest`。
- 前端 `npm run build` 通过，包含 `vue-tsc --noEmit` 和 Vite 生产构建。

## 边界与后续

本阶段只完成 Dense -> Sparse-only 的受控降级，不声称已具备统一端到端超时、限流、并发舱壁、SSE 取消或自动重试治理。下一轮优先处理索引、Embedding 和解析器版本治理：让模型、维度、Chunk/解析策略和索引 schema 变化能够被识别，并支持有边界的重建或回退。
