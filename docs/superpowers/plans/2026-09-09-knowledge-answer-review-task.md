# 知识问答复习任务 Implementation Plan

> **For agentic workers:** 本计划在当前会话内执行，完成后独立提交。

**Goal:** 将知识问答中的证据异常 claim 转为用户确认后可查询、改期、完成的独立复习任务。

**Architecture:** 新增独立 `t_knowledge_review_task` 表和 storage profile 服务，任务以问题 + claim + 证据状态生成确定性幂等 ID，不伪造面试 session。知识问答页在证据核对项上提供确认入口，并展示已保存任务及完成/改期操作。

**Tech Stack:** Java 21 编译基线、Spring Boot、MyBatis-Plus、MySQL、Vue 3、TypeScript。

## Global Constraints

- Redis 仅作为已有配置参考，本轮不新增或修改 Redis 能力。
- 所有写入仅发生在 `D:\IdeaProjects\java-interview-rag`。
- 只修改与本闭环直接相关的代码、迁移、前端和事实文档。

### Task 1: 后端任务模型与持久化

**Files:** 新增 knowledge review domain/entity/mapper/store/service/controller 与 `deploy/mysql/init/018_knowledge_review_task.sql`；新增对应单测。

- [ ] 定义任务状态、请求/响应和 store 接口。
- [ ] 实现 MySQL 表、幂等插入、按状态查询、完成和改期。
- [ ] 实现 service 校验：仅允许四类证据异常，任务 ID 由规范化问题/claim/status/source 编号确定性生成。
- [ ] 暴露 `POST/GET/PATCH /api/knowledge/review-tasks`。
- [ ] 覆盖重复确认、非法状态、空文本、完成幂等和日期清理测试。

### Task 2: 知识问答页交互闭环

**Files:** 修改 `frontend/src/api/knowledge.ts`、`KnowledgeClaimEvidence.vue`、`KnowledgeAnswerPanel.vue`、`KnowledgeAnswerView.vue`，必要时新增任务列表组件。

- [ ] 在异常 claim 上提供“加入复习”确认按钮，成功后显示任务状态。
- [ ] 加载当前已保存知识复习任务，展示问题、claim、证据状态和来源编号。
- [ ] 支持计划日期、完成和错误重试；不影响问答结果与原有来源定位。
- [ ] 前端类型检查与生产构建。

### Task 3: 文档与验证

- [ ] 最小 Java 相关测试、前端构建和 `git diff --check`。
- [ ] 更新 `docs/04-当前进度.md`、`docs/09-后续开发路线.md`，记录已实现边界和未做的自动建任务。
- [ ] 写阶段总结，提交单一功能闭环。
