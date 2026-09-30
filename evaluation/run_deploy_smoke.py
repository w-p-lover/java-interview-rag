#!/usr/bin/env python3
"""验证容器部署暴露的只读健康、前端反代和核心入口，不调用模型或写入业务数据。"""

from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


def request(url: str, timeout: float, accept: str = "application/json") -> tuple[int, bytes, str]:
    try:
        with urlopen(Request(url, headers={"Accept": accept}), timeout=timeout) as response:
            return response.status, response.read(), response.headers.get("Content-Type", "")
    except HTTPError as error:
        return error.code, error.read(), error.headers.get("Content-Type", "")
    except URLError as error:
        raise RuntimeError(f"无法连接 {url}: {error.reason}") from error


def json_value(body: bytes, label: str) -> object:
    try:
        return json.loads(body.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        raise AssertionError(f"{label}: 响应不是合法 JSON") from error


def expect_json(url: str, expected_status: int, timeout: float, label: str) -> object:
    status, body, _ = request(url, timeout)
    if status != expected_status:
        detail = body.decode("utf-8", errors="replace")[:240]
        raise AssertionError(f"{label}: 期望 HTTP {expected_status}，实际 {status}: {detail}")
    return json_value(body, label)


def check_migrations(root: Path) -> int:
    files = sorted((root / "deploy" / "mysql" / "init").glob("*.sql"))
    numbers = [int(match.group(1)) for path in files
               if (match := re.match(r"(\d+)_.*\.sql$", path.name))]
    if numbers != list(range(1, len(numbers) + 1)):
        raise AssertionError(f"MySQL 迁移编号不连续: {numbers}")
    return len(numbers)


def run(root: Path, backend_url: str, frontend_url: str, timeout: float) -> None:
    migration_count = check_migrations(root)
    health = expect_json(f"{backend_url.rstrip('/')}/actuator/health", 200, timeout, "后端健康")
    if not isinstance(health, dict) or health.get("status") != "UP":
        raise AssertionError(f"后端健康状态异常: {health}")

    cases = expect_json(f"{backend_url.rstrip('/')}/api/knowledge/evaluation-cases", 200, timeout, "固定评估集")
    if not isinstance(cases, list) or not cases:
        raise AssertionError("固定评估集为空")
    documents = expect_json(f"{backend_url.rstrip('/')}/api/knowledge/documents", 200, timeout, "资料目录")
    if not isinstance(documents, dict) or "documents" not in documents:
        raise AssertionError("资料目录缺少 documents 字段")
    expect_json(f"{backend_url.rstrip('/')}/api/interviews/evaluation-experiments/score-stability",
                200, timeout, "评分实验入口")

    status, body, content_type = request(f"{frontend_url.rstrip('/')}/", timeout, "text/html")
    if status != 200 or "text/html" not in content_type or b"<div id=\"app\">" not in body:
        raise AssertionError(f"前端首页异常: HTTP {status}, Content-Type={content_type}")
    proxied_cases = expect_json(f"{frontend_url.rstrip('/')}/api/knowledge/evaluation-cases",
                                200, timeout, "前端反代固定评估集")
    if not isinstance(proxied_cases, list) or len(proxied_cases) != len(cases):
        raise AssertionError("前端反代后的评估集数量与后端不一致")

    print(f"deploy smoke 通过: 迁移 {migration_count} 个，后端健康、资料目录、评分实验、前端首页和 /api 反代均正常")


def main() -> int:
    parser = argparse.ArgumentParser(description="验证 Java Interview RAG 容器部署只读入口")
    parser.add_argument("--backend-url", default="http://127.0.0.1:18080")
    parser.add_argument("--frontend-url", default="http://127.0.0.1:5173")
    parser.add_argument("--timeout", type=float, default=5.0)
    args = parser.parse_args()
    if args.timeout <= 0:
        parser.error("--timeout 必须大于 0")
    try:
        run(Path(__file__).resolve().parents[1], args.backend_url, args.frontend_url, args.timeout)
    except (AssertionError, RuntimeError, ValueError) as error:
        print(f"deploy smoke 失败: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
