# Phase28-SSE 问答取消边界

## 升级目标

补齐流式知识问答的客户端超时和主动取消边界，避免用户离开页面或取消请求后，前端仍等待、后端仍把断开误报为模型失败。

## 完成内容

- 前端 `streamKnowledgeQuestion` 接受 `AbortSignal`。
- 问答 composable 为每次请求创建 `AbortController`，60 秒未完成自动中止，并暴露 `cancel()`。
- 清空按钮在请求进行中改为取消当前问答，取消后显示明确提示。
- 后端 SSE 流在收到 Reactor cancel 时记录 `RagRetrievalLogStatus.CANCELLED` 和 `CLIENT_DISCONNECT`。
- 正常完成、知识不足拒答和模型异常仍使用原有状态。

## 验证结果

- Java 21 下 `RagAnswerServiceTest`、`RagAnswerControllerTest` 通过，新增客户端取消日志回归。
- 前端 `npm run build`（含 `vue-tsc --noEmit`）通过。

## 边界与后续

本阶段只覆盖客户端 60 秒超时和主动取消传播，不代表服务端已有统一端到端时限、模型调用硬超时、限流、并发舱壁或跨服务取消治理。后续只有出现真实服务端超时/并发瓶颈，才继续扩大韧性边界。
