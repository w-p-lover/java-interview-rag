# Phase15-Storage完整演示与凭据解耦

## 本轮升级

在明确授权外部访问后，对当前提交执行一次脱敏 storage 演示，验证外部存储链路和失败边界，并修复 Chat 与 Embedding 凭据耦合问题。

## 真实验证

- 临时 `storage` 实例监听 `18081`，健康检查返回 `UP`。
- 导入 `demo/2026-09-01-storage.md`，生成 2 个 Chunk；检索返回 3 条结果，首位为临时来源，标题路径为“Java 面试训练演示 / 向量生命周期”，相似度约 `0.6132`。
- 无模型项目深挖会话启动成功，返回 `PROJECT_DEEP_DIVE`、4 道题和 `maxQuestionCount=4`；随后 MySQL 恢复一致。
- 会话事件接口对新会话返回空时间线；不存在会话返回 `404`。
- 无资料问答返回 `grounded=false` 的统一中文拒答。
- 临时知识来源通过删除接口清理，临时会话通过定向 JDBC 删除；端口 `18081` 已释放。

## 发现与修复

- 两次 Chat 调用尝试均返回 HTTP `403`，没有继续重放模型请求。
- 原配置让 Chat 与 DashScope Embedding 共用 `JAVA_INTERVIEW_RAG_DASHSCOPE_API_KEY`，切换 Chat 凭据会污染 Embedding。
- `application-storage.yml` 现支持独立 Chat 覆盖：`JAVA_INTERVIEW_RAG_OPENAI_BASE_URL`、`JAVA_INTERVIEW_RAG_OPENAI_API_KEY`、`JAVA_INTERVIEW_RAG_OPENAI_CHAT_MODEL`；未覆盖时回退到原 DashScope Chat 配置。

## 修改文件

- `src/main/resources/application-storage.yml`：解耦 Chat 端点、密钥、模型的覆盖变量。
- `README.md`：补充独立 Chat 配置说明。
- `docs/04-当前进度.md`、`docs/05-开发记录.md`、`docs/07-技术决策记录.md`：记录当前真实链路、失败边界和 ADR-105。

## 验证结果

- `mvnw.cmd '-Dmaven.repo.local=.m2' '-Djava.io.tmpdir=.tmp' '-DskipTests' package`：通过。
- storage 启动、健康检查、真实 Embedding 导入/检索、MySQL 会话恢复和清理：通过。
- Chat 质量基线：未通过，外部端点返回 `403`；未执行面试评分、复习 attempt 或重复重试。

## 后续

确认可用 Chat 账号/端点后，只需重跑一次受控知识问答和项目深挖评分验收；不要重复当前已通过的 Embedding 资料或同一无模型会话数据。
