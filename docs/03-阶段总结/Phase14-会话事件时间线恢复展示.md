# Phase14-会话事件时间线恢复展示

## 本轮升级

角色对话运行时原本已经提供 SSE 阶段事件和 `GET /api/interviews/sessions/{sessionId}/events` 恢复接口，但前端恢复会话时没有读取该接口，用户刷新后无法看到已完成的阶段。此次将已有能力接入 Vue 页面，形成“提交事件 → 页面追加 → 刷新恢复 → 继续训练”的可见闭环。

## 修改文件

- `frontend/src/api/interview.ts`：增加会话事件列表查询方法。
- `frontend/src/composables/useInterviewSession.ts`：维护事件列表；恢复时读取后端时间线；接收 SSE 阶段时追加；开始新会话和重置时清理旧列表；事件读取失败不阻断会话恢复。
- `frontend/src/components/interview/InterviewConversationTimeline.vue`：新增阶段时间线展示，显示事件类型、题号、摘要和本地时间，不展示回答正文。
- `frontend/src/views/InterviewView.vue`：在进行中和已完成会话页面接入时间线。
- `docs/04-当前进度.md`、`docs/05-开发记录.md`、`docs/08-项目技术栈与功能总结.md`：同步当前事实与边界。

## 验证结果

- `frontend` 执行 `npm run build`：通过，包含 `vue-tsc --noEmit` 和 Vite 生产构建。
- `git diff --check`：通过。
- 未执行真实 Chat、Embedding、MySQL、Qdrant 调用；未修改后端事件重建逻辑。

## 重要边界

- 时间线数据仍来自现有会话状态重建接口，历史事件时间戳按接口读取时生成；本轮没有新增事件表、消息表或跨进程 checkpoint。
- 事件接口失败只影响辅助展示，主会话仍可恢复；SSE 多轮事件使用列表位置参与渲染 key，避免每轮序号重置造成重复 key。

## 后续

只有出现真实分支、修正循环、人工确认或跨进程恢复需求时，才继续评估 Graph/HITL/checkpoint；当前不因时间线展示接入而扩大到图工作流。
