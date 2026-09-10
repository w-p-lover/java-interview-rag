#!/usr/bin/env python3
"""验证默认 local profile 的最小启动与资料生命周期，不调用模型或外部存储。"""

from __future__ import annotations

import argparse
import json
import sys
import uuid
from dataclasses import dataclass
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen


@dataclass(frozen=True)
class HttpResult:
    status: int
    body: bytes
    content_type: str


def request(base_url: str, method: str, path: str, body: bytes | None = None,
            content_type: str | None = None, timeout: float = 5.0) -> HttpResult:
    headers = {"Accept": "application/json"}
    if content_type:
        headers["Content-Type"] = content_type
    req = Request(base_url.rstrip("/") + path, data=body, headers=headers, method=method)
    try:
        with urlopen(req, timeout=timeout) as response:
            return HttpResult(response.status, response.read(), response.headers.get("Content-Type", ""))
    except HTTPError as error:
        return HttpResult(error.code, error.read(), error.headers.get("Content-Type", ""))
    except URLError as error:
        raise RuntimeError(f"无法连接后端 {base_url}: {error.reason}") from error


def json_body(result: HttpResult) -> object:
    try:
        return json.loads(result.body.decode("utf-8"))
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        raise AssertionError(f"响应不是合法 JSON: HTTP {result.status}") from error


def multipart(field_name: str, filename: str, content: bytes,
              fields: dict[str, str]) -> tuple[bytes, str]:
    boundary = "----java-interview-rag-smoke-" + uuid.uuid4().hex
    chunks: list[bytes] = []
    for name, value in fields.items():
        chunks.extend([
            f"--{boundary}\r\n".encode(),
            f'Content-Disposition: form-data; name="{name}"\r\n\r\n'.encode(),
            value.encode("utf-8"),
            b"\r\n",
        ])
    chunks.extend([
        f"--{boundary}\r\n".encode(),
        f'Content-Disposition: form-data; name="{field_name}"; filename="{filename}"\r\n'.encode(),
        b"Content-Type: text/markdown\r\n\r\n",
        content,
        b"\r\n",
        f"--{boundary}--\r\n".encode(),
    ])
    return b"".join(chunks), f"multipart/form-data; boundary={boundary}"


def expect(result: HttpResult, status: int, label: str) -> object:
    if result.status != status:
        detail = result.body.decode("utf-8", errors="replace")[:240]
        raise AssertionError(f"{label}: 期望 HTTP {status}，实际 {result.status}: {detail}")
    return json_body(result)


def run(base_url: str, timeout: float) -> None:
    source = "smoke/local-demo.md"
    content = "# 本地冒烟资料\n\n## Java\n\nHashMap 使用数组与链表保存键值对。\n"
    payload, content_type = multipart("file", "local-demo.md", content.encode("utf-8"), {"source": source})

    # 固定来源只属于本次脚本，先清理可重入运行遗留。
    expect(request(base_url, "DELETE", f"/api/knowledge/documents?source={source}", timeout=timeout), 200, "清理临时资料")
    catalog = expect(request(base_url, "GET", "/api/knowledge/documents", timeout=timeout), 200, "查询目录")
    if not isinstance(catalog, dict) or "documents" not in catalog:
        raise AssertionError("查询目录: 缺少 documents 字段")
    cases = expect(request(base_url, "GET", "/api/knowledge/evaluation-cases", timeout=timeout), 200, "查询固定评估集")
    if not isinstance(cases, list) or not cases:
        raise AssertionError("查询固定评估集: 评估题集为空")

    try:
        imported = expect(request(base_url, "POST", "/api/knowledge/documents/import", payload,
                                  content_type, timeout), 200, "首次导入")
        if not isinstance(imported, dict) or not imported.get("imported") or imported.get("chunkCount", 0) < 1:
            raise AssertionError("首次导入: 未返回 imported=true 或有效 chunk")

        duplicate_payload, duplicate_type = multipart("file", "local-demo.md", content.encode("utf-8"), {"source": source})
        duplicate = expect(request(base_url, "POST", "/api/knowledge/documents/import", duplicate_payload,
                                   duplicate_type, timeout), 200, "重复导入")
        if not isinstance(duplicate, dict) or duplicate.get("imported") is not False:
            raise AssertionError("重复导入: 未返回 imported=false")

        catalog_after_import = expect(request(base_url, "GET", "/api/knowledge/documents", timeout=timeout), 200, "查询导入目录")
        documents = catalog_after_import.get("documents", []) if isinstance(catalog_after_import, dict) else []
        if not any(item.get("source") == source for item in documents if isinstance(item, dict)):
            raise AssertionError("查询导入目录: 未找到临时来源")

        search = expect(request(base_url, "GET", "/api/knowledge/search?query=HashMap&topK=5",
                                timeout=timeout), 200, "local 查资料")
        results = search.get("results", []) if isinstance(search, dict) else []
        if not any(item.get("source") == source for item in results if isinstance(item, dict)):
            raise AssertionError("local 查资料: 未召回刚导入的临时来源")

        expect(request(base_url, "DELETE", f"/api/knowledge/documents?source={source}", timeout=timeout), 200, "删除临时资料")
        expect(request(base_url, "GET", f"/api/knowledge/documents/detail?source={source}", timeout=timeout), 404,
               "删除后查询详情")

        invalid_payload, invalid_type = multipart("file", "local-demo.md", content.encode("utf-8"), {"source": " "})
        invalid = expect(request(base_url, "POST", "/api/knowledge/documents/import", invalid_payload,
                                 invalid_type, timeout), 400, "非法来源")
        if not isinstance(invalid, dict) or not invalid.get("message"):
            raise AssertionError("非法来源: 缺少中文错误消息")
        expect(request(base_url, "GET", "/api/knowledge/does-not-exist", timeout=timeout), 404, "未知路由")
    finally:
        request(base_url, "DELETE", f"/api/knowledge/documents?source={source}", timeout=timeout)

    print(f"local smoke 通过: 评估集 {len(cases)} 题，导入/去重/查资料/删除/失败路径均已验证")


def main() -> int:
    parser = argparse.ArgumentParser(description="验证 Java Interview RAG 默认 local profile 的最小 HTTP 闭环")
    parser.add_argument("--base-url", default="http://127.0.0.1:18080", help="已启动的 local 后端地址")
    parser.add_argument("--timeout", type=float, default=5.0, help="单次 HTTP 请求超时秒数")
    args = parser.parse_args()
    try:
        run(args.base_url, args.timeout)
    except (AssertionError, RuntimeError) as error:
        print(f"local smoke 失败: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
