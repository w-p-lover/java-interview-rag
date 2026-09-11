#!/usr/bin/env python3
"""启动 local 后端与 Vue 前端，并执行最小 HTTP 冒烟，输出固定演示入口。"""

from __future__ import annotations

import argparse
import os
import signal
import subprocess
import sys
import time
from pathlib import Path
from urllib.error import HTTPError
from urllib.error import URLError
from urllib.request import Request, urlopen


def http_status(url: str, timeout: float = 2.0) -> int | None:
    try:
        with urlopen(Request(url, method="GET"), timeout=timeout) as response:
            return response.status
    except HTTPError as error:
        return error.code
    except Exception:
        return None


def wait_until(url: str, timeout: float, label: str) -> None:
    deadline = time.monotonic() + timeout
    last_status: int | None = None
    while time.monotonic() < deadline:
        last_status = http_status(url)
        if last_status in (200, 204, 503):
            return
        time.sleep(0.25)
    suffix = f"，最后状态 HTTP {last_status}" if last_status is not None else ""
    raise RuntimeError(f"等待{label}超时（{timeout:.1f}s）{suffix}")


def terminate_process(process: subprocess.Popen[bytes], label: str) -> None:
    if process.poll() is not None:
        return
    try:
        if os.name == "nt":
            process.send_signal(signal.CTRL_BREAK_EVENT)
        else:
            process.terminate()
        process.wait(timeout=8)
    except Exception:
        process.kill()
        process.wait(timeout=8)
    print(f"已停止{label}")


def start_process(command: list[str], cwd: Path, log_path: Path, env: dict[str, str]) -> subprocess.Popen[bytes]:
    log = log_path.open("wb")
    creationflags = subprocess.CREATE_NEW_PROCESS_GROUP if os.name == "nt" else 0
    process = subprocess.Popen(
        command,
        cwd=cwd,
        env=env,
        stdout=log,
        stderr=subprocess.STDOUT,
        creationflags=creationflags,
    )
    process._demo_log = log  # type: ignore[attr-defined]
    return process


def close_log(process: subprocess.Popen[bytes]) -> None:
    log = getattr(process, "_demo_log", None)
    if log is not None:
        log.close()


def run(args: argparse.Namespace) -> int:
    root = Path(__file__).resolve().parents[1]
    frontend = root / "frontend"
    temp = root / ".tmp"
    temp.mkdir(parents=True, exist_ok=True)
    backend_log = temp / "demo-backend.log"
    frontend_log = temp / "demo-frontend.log"

    env = os.environ.copy()
    java21 = Path(r"C:\Users\IT074\.jdks\ms-21.0.10")
    if (java21 / "bin" / "java.exe").exists():
        env["JAVA_HOME"] = str(java21)
    env["MAVEN_USER_HOME"] = str(root / ".m2")
    env["TMP"] = str(temp)
    env["TEMP"] = str(temp)
    env["VITE_API_PROXY_TARGET"] = f"http://127.0.0.1:{args.backend_port}"

    backend_command = [
        str(root / "mvnw.cmd"), "-q", "-DskipTests",
        f"-Dmaven.repo.local={root / '.m2' / 'repository'}",
        f"-Djava.io.tmpdir={temp}",
        "-Dspring-boot.run.profiles=local",
        f"-Dspring-boot.run.arguments=--server.port={args.backend_port}",
        "spring-boot:run",
    ]
    frontend_command = [
        "npm.cmd", "run", "dev", "--", "--host", "127.0.0.1",
        "--port", str(args.frontend_port),
    ]

    backend: subprocess.Popen[bytes] | None = None
    frontend_process: subprocess.Popen[bytes] | None = None
    try:
        if http_status(f"http://127.0.0.1:{args.backend_port}/actuator/health") is not None:
            raise RuntimeError(f"后端端口 {args.backend_port} 已被占用，请先停止旧服务或更换 --backend-port")
        if http_status(f"http://127.0.0.1:{args.frontend_port}/") is not None:
            raise RuntimeError(f"前端端口 {args.frontend_port} 已被占用，请先停止旧服务或更换 --frontend-port")

        print("[1/4] 启动 local 后端（无 MySQL/Qdrant/Chat 依赖）")
        backend = start_process(backend_command, root, backend_log, env)
        wait_until(f"http://127.0.0.1:{args.backend_port}/actuator/health", args.startup_timeout, "后端健康检查")

        print("[2/4] 启动 Vue 前端")
        frontend_process = start_process(frontend_command, frontend, frontend_log, env)
        wait_until(f"http://127.0.0.1:{args.frontend_port}/", args.startup_timeout, "前端页面")

        print("[3/4] 执行 local 资料生命周期冒烟")
        smoke_command = [
            sys.executable, str(root / "evaluation" / "run_local_smoke.py"),
            "--base-url", f"http://127.0.0.1:{args.backend_port}",
            "--startup-timeout", str(args.startup_timeout),
        ]
        subprocess.run(smoke_command, cwd=root, env=env, check=True, timeout=args.smoke_timeout)

        print("[4/4] 演示环境已就绪")
        print(f"后端健康：http://127.0.0.1:{args.backend_port}/actuator/health")
        print(f"前端入口：http://127.0.0.1:{args.frontend_port}/")
        print("固定操作：资料准备加载演示资料 → 显式导入 → 资料问答 → 加载脱敏面试资料 → 审计/匹配/深挖 → 模拟面试 → 统一复习中心")
        print(f"日志文件：{backend_log}、{frontend_log}")

        if args.keep_running:
            print("--keep-running 已启用，按 Ctrl+C 停止演示服务")
            while True:
                if backend.poll() is not None:
                    raise RuntimeError("后端进程提前退出，请查看 demo-backend.log")
                if frontend_process.poll() is not None:
                    raise RuntimeError("前端进程提前退出，请查看 demo-frontend.log")
                time.sleep(1)
        return 0
    except KeyboardInterrupt:
        print("收到中断，正在停止演示服务")
        return 130
    finally:
        if frontend_process is not None:
            terminate_process(frontend_process, "前端")
            close_log(frontend_process)
        if backend is not None:
            terminate_process(backend, "后端")
            close_log(backend)


def main() -> int:
    parser = argparse.ArgumentParser(description="启动 Java Interview RAG 五分钟可复现演示")
    parser.add_argument("--backend-port", type=int, default=18080)
    parser.add_argument("--frontend-port", type=int, default=5173)
    parser.add_argument("--startup-timeout", type=float, default=60.0)
    parser.add_argument("--smoke-timeout", type=float, default=90.0)
    parser.add_argument("--keep-running", action="store_true", help="冒烟通过后保持前后端运行")
    args = parser.parse_args()
    if args.startup_timeout <= 0 or args.smoke_timeout <= 0:
        parser.error("超时参数必须大于 0")
    try:
        return run(args)
    except (RuntimeError, subprocess.CalledProcessError, subprocess.TimeoutExpired) as error:
        print(f"演示启动失败: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
