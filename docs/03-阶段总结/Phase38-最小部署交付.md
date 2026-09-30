# Phase38：最小部署交付

## 功能成果

- Compose 从“只启动基础设施”扩展为 MySQL、Qdrant、Redis、storage 后端和 Nginx 前端的完整启动链。
- Qdrant 健康后由一次性 `qdrant-init` 服务创建缺失的 `java-interview-rag-chunks` collection，已存在时幂等跳过。
- 后端只有在 MySQL、Redis 健康且 Qdrant 初始化成功后才启动；前端只有在后端健康后才启动。
- 新增后端/前端镜像定义、Nginx SPA 回退与 `/api`/`/actuator` 反向代理，SSE 关闭代理缓冲并延长读取超时。
- 新增 `evaluation/run_deploy_smoke.py`，只读检查 42 个迁移编号连续性、后端健康、资料目录、评分实验入口、前端首页和 `/api` 反代。

## 主要文件

- `docker-compose.yml`：完整服务依赖、健康门槛和端口。
- `deploy/backend.Dockerfile`：Java 17 多阶段构建和 Actuator 健康检查。
- `deploy/frontend.Dockerfile`：Vue 构建与 Nginx 静态运行时。
- `deploy/nginx.conf`：SPA 回退、API/Actuator 反代和 SSE 设置。
- `.dockerignore`：排除凭据、构建产物、缓存和本地对象。
- `evaluation/run_deploy_smoke.py`：无模型、无写入的部署只读验收。
- `README.md`、`evaluation/README.md`：部署命令、检查范围和边界。

## 验证

- `python -m py_compile evaluation/run_deploy_smoke.py` 通过。
- 迁移目录静态检查确认当前 `001` 至 `042` 连续。
- 当前机器未安装 Docker CLI，未执行真实镜像构建、Compose 启动、容器健康检查或备份恢复。
- 本轮未调用 Chat/Embedding，未修改数据库和外部服务。

## 未完成边界

- 需要具备 Docker 的环境执行 `docker compose up -d --build` 和 `python evaluation/run_deploy_smoke.py`。
- MySQL、Qdrant、Redis 和对象存储的备份恢复仍需在部署环境完成一次真实演练；本轮不宣称 11.2 全部验收完成。
