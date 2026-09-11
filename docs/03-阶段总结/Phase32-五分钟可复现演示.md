# Phase32：五分钟可复现演示

## 功能成果

- 新增 `evaluation/run_demo.py`，一条命令启动 local Spring Boot 后端与 Vue 前端。
- 启动脚本等待后端 `/actuator/health` 和前端首页可访问，再执行 `run_local_smoke.py`，验证固定评估集读取、资料导入、重复导入去重、BM25 查资料、删除和非法输入失败路径。
- 成功时输出固定访问地址、推荐操作顺序和后端/前端日志位置；支持自定义端口、启动超时、冒烟超时和 `--keep-running`。
- Vite 代理目标可通过 `VITE_API_PROXY_TARGET` 覆盖，保证非默认演示端口下前端仍访问对应后端。
- 启动失败或冒烟失败时只回收本轮脚本启动的进程，不触碰用户已有服务。

## 主要文件

- `evaluation/run_demo.py`：启动、健康等待、smoke 编排、日志和进程回收。
- `evaluation/README.md`：固定命令和端口覆盖说明。
- `frontend/vite.config.ts`：支持演示脚本注入 API 代理目标。
- `docs/04-当前进度.md`、`docs/08-项目技术栈与功能总结.md`、`docs/09-后续开发路线.md`：同步当前完成状态和后续优先级。

## 验证

- `python -m py_compile evaluation/run_demo.py evaluation/run_local_smoke.py` 通过。
- `npm run build`（含 `vue-tsc --noEmit`）通过。
- 使用隔离端口 `18082/5174` 实际启动后端和前端，local smoke 验证 26 道评估题及资料导入/去重/查资料/删除/失败路径通过。
- `git diff --check` 通过。

## 边界

- 该演示只覆盖无需外部模型的 local 产品链路；简历审计、岗位匹配、面试评分等付费模型步骤仍需在 `storage` profile 和明确授权下单独执行。
- 默认端口被其他服务占用时脚本会明确失败，使用端口参数重新启动，不自动停止用户已有进程。
