# Java Interview RAG

Java 求职面试知识库与智能训练平台，基于 Java 17、Spring Boot 3，围绕求职方向、简历理解、资料辅助、模拟面试、结构化反馈和复习计划逐步建设，并按需接入 Spring AI 和向量数据库。

阶段 0 的项目治理、阶段 1 的 Markdown 资料知识库和阶段 2 的向量生命周期已经完成，阶段 3 已建立最小 RAG 回答、真实来源返回和知识不足兜底。项目已具备 Markdown 导入、解析切分、向量生命周期、可追踪检索、基于资料的回答接口、已迁移的召回日志表和固定评估题集，并提供 Vue 求职面试工作台；下一步基于题集实现批量评估，模型额度可用时再验证真实模型回答质量。

## 当前 Web 功能

- `POST /api/knowledge/documents/import` 接收 Markdown 文件与显式逻辑来源。
- `POST /api/knowledge/documents/rebuild` 重传已登记版本的来源与 Markdown 文件，修复缺失或残留向量。
- `DELETE /api/knowledge/documents?source=...` 清理逻辑来源下的全部向量与版本登记。
- `GET /api/knowledge/search?query=...&topK=5&similarityThreshold=0.0` 在 `storage` profile 下执行相似度检索，返回真实 chunk 正文、来源、版本、顺序、标题路径和相似度。
- `POST /api/knowledge/answer` 在 `storage` profile 下根据 `{"question":"..."}` 检索并生成回答，返回回答是否由可靠资料支撑及实际使用的来源列表。
- `GET /api/knowledge/retrieval-logs?limit=20` 在 `storage` profile 下查询独立数据库中最近 RAG 召回记录，最多返回 100 条。
- `GET /api/knowledge/evaluation-cases` 返回内置的固定评估题集，包含问题、期望逻辑来源和预期回答要点，不调用模型或数据库。
- 逻辑来源按大小写敏感的精确字符串区分，不能包含首尾空白。
- 后端返回内容版本、首次或重复导入状态，以及保留标题路径和顺序的 chunk。
- Vue 首页使用 Hallmark Workbench 结构表达求职面试产品主线，采用柔和的暖灰与珊瑚信号色，不伪造未实现模块的数据。
- 当前可用区域支持选择或拖入简历、岗位描述和面试资料，并以“选择文件 → 版本判定 → 标题解析 → 生成切片”连接输入与结果，展示版本指纹、重复提示和 chunk 明细。
- 顶部命令面板支持点击或 `Ctrl/Cmd + K` 打开，使用方向键、Enter 和 Esc 完成区段跳转与关闭。
- 默认 `local` profile 的登记仅保存在 Spring Boot 进程内且不写向量；`storage` profile 使用 MySQL 持久化登记，并通过 DashScope Embedding 将首次导入的 chunk 批量写入 Qdrant。
- 相同来源和版本的重复导入会在向量化前短路；同一来源导入新内容时先写入新版本，再清理旧版本向量。已登记版本可以手动重建并收敛同来源其他版本和同版本多余 chunk；回答接口只将相似度不低于 0.60 的资料交给模型，未命中时不调用模型并返回知识不足。

## 项目主线

```text
求职目标与简历档案
→ Markdown 资料知识库
→ 文档解析与切分
→ 向量生命周期
→ RAG 问答与来源引用
→ 召回日志与 RAG 评估
→ 文本模拟面试
→ 动态追问与结构化评分
→ 错题和复习计划
→ Tool Calling
→ 必要时扩展 MCP
```

## 项目边界

- 本项目是独立的新项目，不修改 `D:\IdeaProjects\blog\blog-springboot`。
- Java 是主后端技术路线。
- 第一阶段优先完成 RAG 核心闭环和评估，不提前建设复杂 Agent 平台。
- 外部参考项目只用于设计借鉴，是否采用由真实需求和评估结果决定。

## 本地运行

准备 Java 17 或更高版本的 JDK。先在仓库根目录启动后端：

```shell
.\mvnw.cmd spring-boot:run
```

后端默认监听 `http://127.0.0.1:18080`。再启动前端：

```shell
cd frontend
npm install
npm run dev
```

浏览器访问 `http://127.0.0.1:5173`。Vite 会将 `/api` 代理到本地后端。

## MySQL 与 Qdrant

项目采用独立数据库 `java_interview_rag` 和独立 Qdrant collection `java-interview-rag-chunks`，不会写入 `blog-springboot` 的业务数据。本地 Compose 使用 MySQL 8；当前共享服务器实际为 MySQL 5.7.36，仓库 SQL 同时兼容 5.7 与 8。

本机具备 Docker Compose 且需要新建本地基础设施时，先根据 [`.env.example`](.env.example) 创建本地 `.env`，再启动基础设施：

```shell
docker compose up -d mysql qdrant
```

项目也会只读导入 `D:/IdeaProjects/blog/blog-springboot/.env`。因此在已有服务器环境下，不需要复制主项目密码或 API Key；新项目会复用其中的 MySQL 主机、端口、账号、密码、Qdrant 地址和 DashScope Key，但强制使用独立数据库 `java_interview_rag` 与 collection `java-interview-rag-chunks`。如需覆盖，可使用 `JAVA_INTERVIEW_RAG_` 前缀变量。

以存储 profile 启动后端：

```shell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=storage
```

`storage` 会使用服务器 `.env` 提供的连接信息，通过 Druid、MyBatis-Plus 和 MySQL 持久化“来源 + 内容版本”登记，并使用 Spring AI 的 DashScope Embedding 与 Qdrant 客户端维护 chunk 向量。向量写入按每批最多 10 个 chunk 调用 Embedding，避免大文档超过当前 DashScope 服务端批量上限。新版本成功写入后会按结构化元数据过滤删除同来源旧向量；手动重建要求重传精确来源和 Markdown 文件，只允许已登记版本，写入失败时保留登记以便重试；显式删除先清理 Qdrant，再删除该来源的全部 MySQL 登记。MySQL `source` 使用 `utf8mb4_bin`，查询和删除 SQL 额外使用 `BINARY` 精确比较，以保持和 Java、Qdrant 相同的大小写敏感语义。Qdrant 启动探测失败只记录警告；MySQL 是该 profile 的必要依赖。Qdrant schema 自动初始化关闭，服务器需要预先创建 collection；MySQL 中需要预先创建 `java_interview_rag` 数据库并按顺序执行仓库内初始化 SQL。

最小检索接口允许 `topK` 取 1 到 20、相似度阈值取 0 到 1；默认阈值为 0.0，用于保留阶段 3 的原始召回基线。调用方可以提高阈值，Qdrant 没有满足条件的结果时返回空列表，不生成没有来源支撑的答案。

回答接口启用 DashScope Chat 自动装配，默认使用 `qwen3.7-plus` 与 `0.2` 温度。它固定检索前 5 条且相似度不低于 `0.60` 的资料，将每条资料编号并要求模型只根据资料回答；没有可靠来源或模型返回空内容时，统一返回“知识库中没有足够可靠的资料回答这个问题。”。此前 `qwen-plus` 的受控真实调用已验证失败日志路径，但因免费额度耗尽返回 HTTP 403；当前模型尚未发起真实生成验证。

阶段 4 的首个闭环记录每次回答的问题、`topK`、相似度阈值、Prompt 版本、命中来源元数据与相似度、耗时、回答状态和失败类型，不保存 chunk 正文或模型回答。迁移脚本为 `deploy/mysql/init/003_rag_retrieval_log.sql`，已执行到独立数据库 `java_interview_rag`；日志查询接口具备表结构前提，日志存储故障仍只会告警且不影响回答。

固定评估题集通过 `GET /api/knowledge/evaluation-cases` 暴露 6 个基线问题，每题包含稳定标识、问题、期望逻辑来源和预期回答要点。题集不自动导入资料；后续批量评估前，调用方需要使用对应的逻辑来源导入真实 Markdown，才能比较召回来源和回答要点。

## 本地验证

```shell
.\mvnw.cmd test
cd frontend
npm run build
```

## 文档入口

- [项目定位](docs/00-项目定位.md)
- [需求边界](docs/01-需求边界.md)
- [技术方案](docs/02-技术方案.md)
- [阶段路线](docs/03-阶段路线.md)
- [当前进度](docs/04-当前进度.md)
- [开发记录](docs/05-开发记录.md)
- [参考项目调研](docs/06-参考项目调研.md)
- [技术决策记录](docs/07-技术决策记录.md)

开发和自动任务必须遵守 [AGENTS.md](AGENTS.md)。
