# Phase31：知识复习回答幂等

## 功能成果

- 知识复习回答提交现在按任务与回答内容生成稳定的 `Idempotency-Key`。
- 后端在调用评价模型前检查已保存结果，网络重试直接复用原 attempt，不重复评分。
- MySQL attempt 表增加唯一幂等键；并发或重复请求通过 `INSERT IGNORE` 与回查收敛到同一结果。
- 同一幂等键用于不同任务或不同回答时返回明确的参数错误。

## 主要文件

- `src/main/java/com/javainterview/rag/knowledge/review/KnowledgeReviewTaskController.java`：接收 `Idempotency-Key` 请求头。
- `src/main/java/com/javainterview/rag/knowledge/review/KnowledgeReviewTaskService.java`：增加幂等检查与冲突校验。
- `src/main/java/com/javainterview/rag/knowledge/persistence/*KnowledgeReviewAttempt*`：持久化唯一键并回查已有 attempt。
- `frontend/src/api/knowledge.ts`：为复习提交生成稳定 SHA-256 请求键。
- `deploy/mysql/init/020_knowledge_review_attempt_idempotency.sql`：新增数据库唯一键。

## 验证

- JDK 21 下 `KnowledgeReviewTaskServiceTest` 与 `KnowledgeReviewTaskControllerTest` 通过。
- 前端 `npm run build`（含 `vue-tsc`）通过。
- `git diff --check` 通过。

## 边界

- 幂等键只保护同一任务和同一回答内容；不同回答仍会形成新的 attempt。
- 旧数据库需执行 020 SQL；未在本轮连接真实 MySQL 重复请求验证。
