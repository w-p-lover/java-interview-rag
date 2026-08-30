# Phase10-本地演示冒烟闭环

## 升级内容

- 新增零依赖 `evaluation/run_local_smoke.py`，把默认 `local` profile 的最小 HTTP 演示收敛为可重复命令。
- 覆盖固定评估集读取、Markdown 首次导入、同版本重复导入短路、来源删除、删除后 404、非法来源 400 和未知路由 404。
- 脚本固定使用独立临时来源，并在结束时清理，避免把演示资料留在进程内登记器中。

## 直接相关文件

- `evaluation/run_local_smoke.py`
- `docs/04-当前进度.md`
- `docs/05-开发记录.md`
- `README.md`

## 验证

- 使用仓库既定 JDK 21 启动默认 `local` profile 后，执行 `python evaluation/run_local_smoke.py`。
- 脚本不调用 Chat、Embedding、MySQL 或 Qdrant；成功输出评估题数量及导入、去重、删除和失败路径均通过。
- 另执行 Python 语法检查，确保脚本可被直接运行。

## 边界与下一步

- 该 smoke 只证明无外部依赖的本地 HTTP 契约，不证明 `storage` profile、远程数据库、Qdrant 或真实模型可用。
- 下一步仍按授权条件运行一次 PDF 结构检索 Embedding 基线；没有授权时使用本 smoke 作为启动和回归入口。
