# Phase12-面试官对话运行时

## 本阶段目标

将正式面试回答从“提交后等待同步 JSON”升级为可感知、可恢复的角色对话回合，同时保持现有结构化评分、证据验真、动态追问和报告链路不变。

## 完成内容

- 新增 `InterviewConversationContext`：只从当前会话最近 6 条回答构造上下文，每条回答最多保留 500 字摘要，并携带题目、总分和遗漏；不携带完整简历、JD、跨会话内容或监控正文。
- `InterviewAnswerEvaluator` 增加上下文评价入口，`InterviewAnswerEvaluationService` 将上下文作为受控 Prompt 区域，`ObservableInterviewAnswerEvaluator` 保持原有评价日志和失败观测。
- 新增 `POST /api/interviews/sessions/{sessionId}/answers/stream`：使用 SSE 发送回答接收、评价开始、评价完成、追问生成或会话完成事件，最后发送完整 `session` 事件。结构化评分仍通过 `call().entity(...)` 完成，不流式输出不完整 JSON。
- 新增 `GET /api/interviews/sessions/{sessionId}/events`：根据已持久化题目与回答重建不含回答正文的事件时间线；现有 MySQL 会话表和乐观锁不变。
- 前端正式答题改用原生 `fetch` 解析 SSE，展示当前评价阶段，结束后继续刷新报告与复习任务。

## 调用链

```text
提交回答
  -> SSE ANSWER_SUBMITTED
  -> SSE EVALUATION_STARTED
  -> 当前会话最近摘要上下文
  -> 结构化评分 + 回答/知识证据验真
  -> 有界追问决策与会话乐观锁更新
  -> SSE EVALUATION_COMPLETED
  -> SSE FOLLOW_UP_GENERATED / SESSION_COMPLETED
  -> SSE session（完整会话）
```

## 技术边界

- 同步 `/answers` 保留兼容；SSE 只负责阶段事件和最终对象，评分本身仍是同步结构化 Chat 调用。
- 会话记忆严格限定 `sessionId`，只发送最近 6 条摘要，不保存新的原始消息日志。
- `GET /events` 的事件时间戳在读取时生成，因此可以恢复阶段顺序，不能还原精确历史发生时间。
- 本阶段没有引入 Graph Runtime、Redis、MQ、Temporal、HITL 或跨进程 checkpoint；Graph 的进入条件仍是出现真实条件分支、修正循环、人工暂停或跨进程恢复需求。

## 验证与遗留

- JDK 21 下后端 `mvnw.cmd -DskipTests compile` 通过。
- 前端 `npm run build`（含 `vue-tsc --noEmit`）通过，`git diff --check` 通过。
- 未调用真实 Chat、Embedding、MySQL 或 Qdrant；SSE 与事件恢复的生产链路仍需在下一次获得授权的 storage 演示中验证。
