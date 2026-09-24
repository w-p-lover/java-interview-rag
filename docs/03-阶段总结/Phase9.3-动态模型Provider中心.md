# Phase9.3：动态模型 Provider 中心

## 本轮目标

在现有只读能力快照上完成 Provider 加密持久化、动态业务路由、功能级默认值、历史模型契约和可操作 Vue 工作台，并对照 InterviewGuide `969e2af` 的 Provider Registry 语义保持本地独立实现。

## 已完成

- MySQL 保存 Provider 与 Chat/Embedding 默认值；API Key 使用 AES-256-GCM 随机 nonce 加密，接口只返回是否配置。
- `RoutingChatModel`、`RoutingEmbeddingModel` 成为 storage 真实业务 Bean；Registry 按 `providerId + version` 缓存客户端并用读写锁串行化调用和配置更新。
- 页面支持新增、编辑、启停、删除、指定能力验证和默认值选择；失败保留最近一次成功数据，API Key 编辑留空保留旧密文。
- 当前默认不可停用或删除；活跃模型任务阻止默认更新；Embedding 模型名和维度必须与已发布索引契约一致；默认值和 Provider 更新使用乐观锁。
- 面试出题、题库生成/组卷、回答评分、复习助手和简历分析通过动态 supplier 保存 `providerId/modelName#providerVersion`；旧字符串构造器继续供现有测试使用。

## 主要变更

- `src/main/java/com/javainterview/rag/modelprovider/*`：Provider 领域、MyBatis 存储、加密、Registry、路由模型、管理服务和 API。
- `src/main/java/com/javainterview/rag/interview/*`：业务历史模型契约由启动字符串改为动态 supplier，题库长任务提交时固化契约。
- `frontend/src/api/modelProvider.ts`、`frontend/src/composables/useModelProviders.ts`、`frontend/src/components/model-provider/*`：动态 Provider 工作台。
- `deploy/mysql/init/038_model_provider.sql`：Provider/default 表及历史模型契约列扩容。

## 验证

- MySQL 5.7.36 已执行 038：两表分别 13/5 列，主键、启用索引和两个外键完整；默认行版本最终为 2，只有 `configured-default`，密文不等于原 Key。
- Provider 14 项、受影响业务 51 项定向测试通过；前端 `npm run build` 通过。
- storage 在 `18103` 启动；临时别名切换/恢复成功，错误版本、不兼容 Embedding 和删除当前默认均返回 409。
- 1440px 与 375px 真实浏览器无横向溢出，表单可访问，控制台无错误。

## 边界

付费上限为 Chat 3、Embedding 1，实际调用 Chat 2、Embedding 0。当前模型固定探测成功；第二候选 `qwen3.7-plus` 首次返回权限错误后立即停止并删除，没有执行第三次业务调用或 Embedding 调用。因此核心动态机制完成，但两个不同 Chat 模型的成功隔离运行仍等待一个可访问的第二模型，不能把本轮解释为双模型质量验收通过。ASR、TTS、对象存储和通用 AI 网关不在本阶段。
