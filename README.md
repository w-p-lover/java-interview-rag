# Java Interview RAG

Java 求职面试知识库与智能训练平台，基于 Java 17、Spring Boot 3，围绕求职方向、简历理解、资料辅助、模拟面试、结构化反馈和复习计划逐步建设，并按需接入 Spring AI 和向量数据库。

阶段 0 的项目治理与 Spring Boot 最小骨架已经完成，当前处于阶段 1：资料知识库。项目已建立文档模型、稳定内容版本标识、ATX 标题解析、段落优先切分、可切换导入登记和 UTF-8 导入编排，并提供首个 Markdown 上传 API 与 Vue 求职面试工作台。

## 当前 Web 功能

- `POST /api/knowledge/documents/import` 接收 Markdown 文件与显式逻辑来源。
- 后端返回内容版本、首次或重复导入状态，以及保留标题路径和顺序的 chunk。
- Vue 首页以求职面试工作台表达“求职目标 → 资料准备 → 模拟面试 → 训练复盘”的产品主线。
- 当前可用区域支持选择或拖入简历、岗位描述和面试资料，并展示版本指纹、重复提示和 chunk 明细。
- 默认 `local` profile 的登记仅保存在 Spring Boot 进程内；`storage` profile 已提供 MySQL 持久化登记与 DashScope/Qdrant 接入配置。
- 当前尚未执行 chunk 向量写入，也未实现 RAG 问答。

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

项目采用独立数据库 `java_interview_rag` 和独立 Qdrant collection `java-interview-rag-chunks`，不会写入 `blog-springboot` 的业务数据。

本机具备 Docker Compose 时，先根据 [`.env.example`](.env.example) 创建本地 `.env`，再启动基础设施：

```shell
docker compose up -d mysql qdrant
```

提供有效的 `JAVA_INTERVIEW_RAG_DASHSCOPE_API_KEY` 后，以存储 profile 启动后端：

```shell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=storage
```

`storage` 会使用 Druid、MyBatis-Plus 和 MySQL 持久化“来源 + 内容版本”登记，并创建 Spring AI 的 DashScope Embedding 与 Qdrant 客户端。Qdrant 启动探测失败只记录警告；MySQL 是该 profile 的必要依赖。当前 Qdrant schema 自动初始化关闭，collection 的创建和 chunk 写入将在向量生命周期闭环中统一实现。

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
