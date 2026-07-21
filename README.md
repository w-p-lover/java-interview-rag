# Java Interview RAG

Java 面试知识库与智能训练平台，基于 Java 17、Spring Boot 3，后续按需接入 Spring AI 和向量数据库，逐步实现知识文档管理、RAG 检索与评估、文本模拟面试和轻量 Agent 能力。

阶段 0 的项目治理与 Spring Boot 最小骨架已经完成，当前处于阶段 1：Markdown 知识库。项目已建立文档模型、稳定内容版本标识、ATX 标题解析、段落优先切分、进程内去重和 UTF-8 导入编排，并提供首个 Markdown 上传 API 与 Vue 知识工坊页面。

## 当前 Web 功能

- `POST /api/knowledge/documents/import` 接收 Markdown 文件与显式逻辑来源。
- 后端返回内容版本、首次或重复导入状态，以及保留标题路径和顺序的 chunk。
- Vue 页面支持选择或拖入文件、编辑逻辑来源，并展示版本指纹、重复提示和 chunk 账页。
- 当前登记仅保存在 Spring Boot 进程内；尚未接入数据库、Embedding、向量库或 RAG 问答。

## 项目主线

```text
Markdown 知识库
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
