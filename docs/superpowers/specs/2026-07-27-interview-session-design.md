# 可持久化文本面试会话设计

## 目标

把已确认的目标岗位、面试准备画像和 3 道首轮题目保存为可恢复的文本面试会话，并支持按题目顺序提交回答。第三道题回答后会话自动完成，为后续逐题评分与动态追问建立稳定状态边界。

## 方案比较

1. 仅使用进程内 Map：实现最少，但应用重启后会话丢失，无法形成真实训练记录。
2. 分别建立会话、题目和回答三张表：查询和扩展最规范，但当前固定 3 题阶段会引入过多表、Mapper 和事务边界。
3. 使用单行会话聚合：目标岗位、确认画像、题目和回答保存为 JSON 快照，当前题号、状态和版本使用独立列；本轮选择该方案。

## 数据模型

`InterviewSession` 保存 UUID、目标岗位、确认画像、3 道题、已提交回答、当前题号、状态、乐观锁版本和创建/更新时间。状态只有 `IN_PROGRESS` 与 `COMPLETED`。

会话创建时当前题号为 0、回答为空、版本为 0。每次回答只能提交给当前题目；保存成功后当前题号和版本各加 1。回答第三题后当前题号为 3，状态自动变为 `COMPLETED`。已完成会话拒绝继续回答。

## 持久化

新增 `t_interview_session`。`profile_json`、`questions_json` 和 `answers_json` 使用 `MEDIUMTEXT` 保存 Jackson JSON 快照；`current_question_index`、`status`、`version`、`created_at`、`updated_at` 使用独立列。

更新回答时 SQL 同时匹配会话 ID、旧版本和 `IN_PROGRESS` 状态。影响行数为 0 代表会话被并发更新或已经完成，服务返回冲突，不覆盖另一请求的回答。

## HTTP 契约

- `POST /api/interviews/sessions`：接收 `targetRole`、确认后的 `profile` 和恰好 3 道 `questions`，创建并返回完整会话。
- `GET /api/interviews/sessions/{sessionId}`：读取完整会话，用于页面刷新或进程重启后恢复。
- `POST /api/interviews/sessions/{sessionId}/answers`：接收非空 `answer`，顺序提交给当前题目并返回更新后的会话。

不存在的会话返回 HTTP 404；非法输入返回 400；已完成或并发更新返回 409。

## 范围

- 接口只在 `storage` profile 暴露并使用项目独立 MySQL。
- 不调用 Chat、Embedding 或 Qdrant，不产生模型费用。
- 不执行远程迁移，只提交兼容 MySQL 5.7/8 的幂等 SQL。
- 不实现跳题、改答案、删除会话、参考答案、评分、追问、前端或用户身份。
- 当前画像只保存结构化证据，不保存原始完整简历和 JD。

## 验证

- 领域服务测试覆盖创建、顺序回答、第三题完成、完成后拒绝回答和乐观锁冲突。
- MySQL Store 测试覆盖 JSON 快照、读取恢复和带版本条件更新。
- 控制器测试覆盖创建、查询、提交回答及 400/404/409 状态。
- 默认 `local` 上下文继续不加载 MySQL 会话能力。
