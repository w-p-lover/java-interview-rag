# Phase18 知识问答证据复习闭环

## 用户结果

知识问答中的证据不足、证据冲突或引用无效 claim 可以由用户确认后加入独立复习任务；任务支持幂等保存、查询、计划日期和完成，不伪造面试会话。

## 主要改动

- 新增 `knowledge.review` 领域模型、服务和 storage API。
- 新增 `t_knowledge_review_task` 迁移及 MyBatis 持久化。
- 知识问答证据面板提供“加入复习”，页面展示任务并支持改期、完成。
- 更新当前进度和后续路线；Redis 未新增改动。

## 验证

- Java 21：`KnowledgeReviewTaskServiceTest`、`RagAnswerControllerTest`。
- 前端：`npm run build`（含 `vue-tsc` 和 Vite 构建）。
- `git diff --check` 通过。

## 边界

- 任务独立于面试复习 attempt，当前不自动创建任务、不调用模型、不保存回答正文。
- 真实 MySQL 迁移执行和浏览器连接后端的交互未在本轮验证。
