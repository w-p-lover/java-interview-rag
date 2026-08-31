# Phase11-业务链路观测中心

## 目标

把已有 Actuator/Spring AI Observation 从“指标入口”推进为可以定位业务路由问题的可调用观测闭环。

## 完成内容

- 新增 `BusinessRequestMetricsFilter`，覆盖所有 `/api` 请求，返回 `X-Request-Id`，按稳定 Spring MVC 路由模板记录请求结果和耗时。
- 新增 `GET /api/observability/overview`，聚合当前进程的请求总量、失败总量、失败率、平均耗时、最大耗时和逐路由明细。
- 新增 Vue `/observability` 页面，展示实例总览和路由表现，支持手动刷新，不自动轮询、不持久化业务正文。
- 复用 Actuator 和 Spring AI Observation，继续关闭 Prompt、Completion、向量查询响应等正文采集。

## 变更文件

- 后端：`BusinessRequestMetricsFilter`、`BusinessObservabilityService`、`BusinessObservabilityController`、`BusinessObservabilityOverview`。
- 前端：`observability.ts`、`useObservability.ts`、`ObservabilityView.vue`、路由和工作台导航。
- 文档：README、当前进度、开发记录、技术决策、技术栈总结和后续路线。

## 验证

- JDK 21 下 Maven 编译通过。
- `JavaInterviewRagApplicationTests` 默认 local 上下文 2 个用例通过。
- `frontend/npm run build` 的 TypeScript 检查和 Vite 构建通过。
- 临时 JAR 在 `18083` 启动后，Actuator 健康状态为 `UP`；访问固定评估集后，业务概览真实返回 `GET /api/knowledge/evaluation-cases` 请求量 1、失败率 0 和耗时。

## 风险与后续

- 指标只保存在当前进程的 Micrometer registry，重启后清零。
- 当前未接入 Prometheus、Grafana、OpenTelemetry Collector 或跨服务 trace。
- 指标不包含请求 ID、会话 ID、Prompt、简历、回答或知识正文；后续如需跨服务定位，必须先定义低基数标签和脱敏策略。
