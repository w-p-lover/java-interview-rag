# MySQL 与 Qdrant 独立存储接入设计

## 目标

参考只读项目 `blog-springboot` 的 MySQL、Druid、MyBatis-Plus、DashScope Embedding 和 Qdrant 接入方式，为当前项目建立独立、可切换的存储基础设施。

本轮完成后：

- 默认本地启动继续使用进程内导入登记器，不依赖外部服务。
- 启用 `storage` profile 后，导入版本登记写入独立 MySQL 数据库 `java_interview_rag`。
- `storage` profile 同时配置 DashScope Embedding 与独立 Qdrant collection `java-interview-rag-chunks`。
- Qdrant 启动探测失败只记录警告，不阻塞应用启动。
- 仓库提供 MySQL 与 Qdrant 的最小 Compose、初始化 SQL 和无密钥示例配置。

## 边界

本轮不实现 chunk 向量写入、更新、删除、重建、RAG 检索或模拟面试。原因是当前导入编排只有“版本登记”和“切片返回”，尚未定义向量状态机；提前写入向量会留下关系数据与 Qdrant 不一致的半成品。

不复用 `blog-springboot` 的数据库、collection、账号、密码或 API Key，也不引入 Redis、RabbitMQ、Elasticsearch和主项目应用容器。

## Profile 设计

使用三个 profile：

| Profile | 职责 |
| --- | --- |
| `local` | 默认 profile；禁用外部数据源和 AI 自动配置，使用进程内登记器 |
| `mysql` | 配置 Druid、MySQL、MyBatis-Plus，并使用 MySQL 登记器 |
| `vector` | 配置 DashScope Embedding、Qdrant 和非阻塞健康检查 |

`storage` 是 profile 组，展开为 `mysql,vector`。这样正式接入可以使用 `--spring.profiles.active=storage`，针对性排查时也可以只启用 `mysql` 或 `vector`。

私有值通过 `.env` 或 `config/application-private.yml` 注入，环境变量统一使用 `JAVA_INTERVIEW_RAG_` 前缀。示例文件只提供占位值。

## 导入登记边界

将当前具体类 `MarkdownImportRegistry` 抽为接口：

```java
public interface MarkdownImportRegistry {
    boolean register(MarkdownDocument document);
}
```

提供两个实现：

- `InMemoryMarkdownImportRegistry`：保留现有 `HashSet` 行为，供默认启动和普通单元测试使用。
- `MySqlMarkdownImportRegistry`：向版本表插入一条记录；命中数据库唯一键时返回 `false`，其他数据库异常继续抛出，避免把连接失败伪装为重复导入。

`MarkdownContentImporter` 保持依赖接口，现有 API 响应和重复导入语义不变。

## 关系表设计

表名：`t_knowledge_document_version`。

| 字段 | 含义 |
| --- | --- |
| `id` | 自增主键 |
| `source` | 稳定逻辑来源 |
| `version` | 规范化内容的 SHA-256 |
| `content_length` | Java 字符长度，用于基础审计 |
| `created_at` | 首次登记时间 |

建立唯一键 `uk_source_version(source, version)`，由数据库保证跨进程和并发场景下相同来源与版本只能登记一次。

本轮不增加 `chunk_count` 和 `import_status`。现有 `register` 在解析切片之前发生，且尚未执行向量写入；此时填写切片数或“成功”状态会产生错误事实。进入向量生命周期闭环时再以状态机和补偿语义扩展表结构。

## Qdrant 设计

沿用 `blog-springboot` 的 Spring AI 1.0.0、Spring AI Alibaba 1.0.0.2、DashScope `text-embedding-v3` 和 Qdrant 1.10.1 接入方式，但使用独立 collection：

```text
java-interview-rag-chunks
```

Qdrant gRPC 默认端口为 `6334`，HTTP 健康检查端口为 `6333`。应用启动后执行一次 `topK=1` 的探测查询；异常只输出中文警告，不阻塞其他 Web 能力。

本轮不声称 collection 已创建或连接成功。本机当前没有 Docker、MySQL 或 Qdrant 服务，真实连通需要后续启动 Compose 并提供 DashScope API Key。

## 基础设施

仓库根目录提供精简 `docker-compose.yml`：

- MySQL 8.0，数据库名固定为 `java_interview_rag`。
- Qdrant 1.10.1，暴露 `6333` 和 `6334`。
- 两个独立命名卷。
- MySQL 首次启动时执行版本表初始化 SQL。

Compose 不包含应用容器，避免当前前后端开发流程被一起改造。

## 验证策略

- 先通过编译失败证明登记器接口重构测试处于 RED，再实现内存版本并复跑。
- 通过 Mapper 边界单元测试验证 MySQL 首次插入和唯一键冲突语义，不依赖真实数据库。
- 通过直接调用健康检查 runner 验证 Qdrant 异常不会向外抛出。
- 执行后端相关测试和 Maven 编译，验证默认 `local` profile 不要求数据库或模型密钥。
- 校验 Compose 与 YAML 能被结构化解析；由于本机没有 Docker 和外部服务，不执行伪造的连通测试。
