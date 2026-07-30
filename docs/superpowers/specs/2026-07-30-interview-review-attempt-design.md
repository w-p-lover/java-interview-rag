# 面试复习二次作答设计

## 背景

当前复习任务只保存正式面试中的遗漏项和完成状态，用户只能“标记已复习”，不能提交新的回答，也无法观察回答质量是否改善。本闭环把复习视为独立训练记录，不改写已完成的正式面试会话。

## 目标与边界

目标：

1. 用户可以针对一个复习任务提交第 1、2…次独立回答。
2. 每次回答持久化评价结果、评价提示词/评分规则/模型版本和知识来源元数据。
3. 返回原正式回答分数、上一次 attempt 分数以及相对差值。
4. 后端 REST 接口和 `/interview` 页面形成可恢复的纵向闭环。

本次不做动态追问、自动重开任务、定时复习、跨会话趋势、Tool Calling、语音、答案库和认证。复习 attempt 是训练记录，不是正式会话的第 N 次回答。

## 领域模型

`InterviewReviewAttempt` 以 `reviewTaskId + attemptNumber` 唯一标识顺序。它保存回答正文、`InterviewAnswerEvaluation` 快照、问题对应的知识来源元数据、`promptVersion`、`rubricVersion`、`modelName`、原回答分数、上次 attempt 分数及两个 score delta。知识来源只保存来源、文档版本、chunk 顺序、标题路径和相似度，不重复保存正文。

复习服务从不可变的 `InterviewSession` 读取原题和正式回答；每次提交前读取已有 attempts，生成稳定的 1-based attempt number，并将历史回答/评分摘要传给评价器。原始 `InterviewSessionAnswer` 永远不更新。

## API

```text
GET  /api/interviews/sessions/{sessionId}/review-tasks/{taskId}/attempts
POST /api/interviews/sessions/{sessionId}/review-tasks/{taskId}/attempts
```

请求体：

```json
{
  "answer": "……",
  "expectedAttemptNumber": 1
}
```

`expectedAttemptNumber` 必须是客户端预期的下一个 1-based 序号；服务端在调用模型前校验，过期请求返回 409。数据库唯一键仍负责并发兜底，冲突不覆盖已有记录。

响应包含 attempt 自身字段、`evaluation`、`originalOverallScore`、`previousOverallScore`、`scoreDeltaFromOriginal`、`scoreDeltaFromPrevious` 和版本元数据。列表按 attemptNumber 升序返回。

## 评价器扩展

保留正式面试的 `evaluate(profile, question, answer)` 行为。新增面向复习的评价入口，接收上次 attempts 的回答、分数和反馈摘要，并关闭动态追问生成。复习提示词明确要求只评价当前回答、参考历史改进，不把历史回答当成当前回答。

评价器暴露固定的 prompt/rubric 版本和配置注入的模型名，服务端将其写入 attempt，保证后续可以解释评价版本变化。

## 持久化

新增 `deploy/mysql/init/008_interview_review_attempt.sql` 和对应 MyBatis entity/mapper/store。表使用 MySQL 5.7 兼容语法、JSON 文本列，不使用外键，遵循现有迁移风格；唯一键为 `(review_task_id, attempt_number)`，并以 `(session_id, review_task_id, created_at)` 支撑查询。

## 前端组件边界

`InterviewReviewTaskPanel` 继续负责任务列表和布局；新增 `InterviewReviewAttemptPanel` 负责单个任务的输入框、提交按钮、历史 attempt 和分数差值展示。`useInterviewSession` 维护按 task ID 索引的 attempts、加载/提交中的 task ID 及错误，并封装 API 调用。父子组件使用 typed props/events，页面视图只做组合，不引入 Pinia 或新路由。

## 错误与一致性

- 非已完成会话、未知任务、空回答和长度超限分别返回 400/404。
- attempt 序号过期或唯一键冲突返回 409，提示刷新历史后重试。
- 模型评价失败不写入半条 attempt。
- 标记任务完成与提交 attempt 相互独立，本版本不自动改变任务状态。
