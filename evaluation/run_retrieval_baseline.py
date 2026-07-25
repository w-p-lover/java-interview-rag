from __future__ import annotations

import argparse
import json
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path


CORPUS_ROOT = Path(__file__).resolve().parent / "corpus"
CORPUS_SOURCES = (
    "java/collections.md",
    "java/concurrency.md",
    "java/jvm.md",
    "mysql/transactions.md",
    "rag/rag-basics.md",
)


def build_multipart(source: str, path: Path) -> tuple[bytes, str]:
    boundary = f"----java-interview-rag-{uuid.uuid4().hex}"
    parts = [
        f"--{boundary}\r\n"
        'Content-Disposition: form-data; name="source"\r\n\r\n'
        f"{source}\r\n",
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="file"; filename="{path.name}"\r\n'
        "Content-Type: text/markdown; charset=utf-8\r\n\r\n",
    ]
    body = parts[0].encode("utf-8") + parts[1].encode("utf-8")
    body += path.read_bytes() + f"\r\n--{boundary}--\r\n".encode("utf-8")
    return body, boundary


def request_json(request: urllib.request.Request, timeout: float) -> dict:
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exception:
        detail = exception.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {exception.code}: {detail}") from exception


def import_corpus(base_url: str, timeout: float) -> None:
    for source in CORPUS_SOURCES:
        path = CORPUS_ROOT / Path(source)
        if not path.is_file():
            raise FileNotFoundError(f"评估语料不存在：{path}")
        body, boundary = build_multipart(source, path)
        request = urllib.request.Request(
            f"{base_url}/api/knowledge/documents/import",
            data=body,
            headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
            method="POST",
        )
        result = request_json(request, timeout)
        status = "已导入" if result["imported"] else "已存在"
        print(f"{source}: {status}, chunks={result['chunkCount']}")


def evaluate(base_url: str, timeout: float) -> dict:
    request = urllib.request.Request(
        f"{base_url}/api/knowledge/evaluations/retrieval",
        method="GET",
    )
    return request_json(request, timeout)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="导入固定语料并运行真实 RAG 检索基线")
    parser.add_argument("--base-url", default="http://127.0.0.1:18080", help="后端服务地址")
    parser.add_argument("--timeout", type=float, default=120.0, help="单次 HTTP 请求超时秒数")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    base_url = args.base_url.rstrip("/")
    try:
        import_corpus(base_url, args.timeout)
        report = evaluate(base_url, args.timeout)
    except (OSError, RuntimeError, ValueError, json.JSONDecodeError) as exception:
        print(f"评估执行失败：{exception}", file=sys.stderr)
        return 1

    print(json.dumps(report, ensure_ascii=False, indent=2))
    if report.get("expectedSourceHitCount") != report.get("totalCaseCount"):
        print("固定题集未全部命中期望来源。", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
