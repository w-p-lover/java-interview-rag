# Phase29 未完成项治理与简历审计长超时

## 用户结果

统一识别已开发未收口、计划中未开发、按条件推进和明确暂缓的内容，并让简历证据审计在阿里云模型可能超过 200 秒时仍有足够的服务端与客户端等待窗口。

## 主要改动

- `ResumeEvidenceAuditService` 默认总超时调整为 420 秒。
- `ResumeEvidenceAuditConfiguration` 与 `application-storage.yml` 同步支持 `JAVA_INTERVIEW_RAG_RESUME_AUDIT_TIMEOUT`，默认 `420s`。
- 前端简历审计请求超时调整为 450 秒，避免客户端早于后端断开。
- `.env.example` 默认 Chat 模型同步为 `qwen3.8-max-0902`，并补充简历审计超时示例。
- `README.md`、技术方案、当前进度、技术决策、项目总结、后续路线和面试掌握文档统一当前模型、超时和未完成项口径。
- 明确 qwen3.8 当前只有 PDF 提取、不可用/超时安全失败证据，成功质量基线仍待模型端点恢复后一次性验证。

## 验证

- JDK 21 下通过 `ResumeEvidenceAuditServiceTest`、`ResumeEvidenceAuditControllerTest`，共 15 项。
- 前端 `npm run build` 通过，包含 `vue-tsc --noEmit` 和 Vite 生产构建。
- `git diff --check` 通过。

## 边界与后续

- 420 秒是简历审计总窗口，不代表第三方 HTTP 客户端每次重试都会立即释放底层 socket；当前仍保留最多两次模型尝试。
- 未重复调用 qwen3.8 外部模型，也未把失败测试包装为成功质量基线。
- 下一项 P0 是端点恢复后的固定样本成功质量基线；随后是五分钟可复现演示和最小知识空间访问控制。
