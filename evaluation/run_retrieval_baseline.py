from __future__ import annotations

import argparse
import datetime
import json
import sys
import urllib.error
import urllib.request
import uuid
from pathlib import Path


CORPUS_ROOT = Path(__file__).resolve().parent / "corpus"
CORPUS_SOURCES = (
    "java/collections.md",
    "java/concurrent-collections.md",
    "java/concurrency.md",
    "java/jvm.md",
    "mysql/indexes.md",
    "mysql/transactions.md",
    "rag/agent-basics.md",
    "rag/rag-basics.md",
)
STRUCTURED_CORPUS_SOURCE = "java/structured-java-notes.pdf"

RATE_FIELDS = (
    "expectedSourceHitRate",
    "hitAt1Rate",
    "meanRecallAt5",
    "meanReciprocalRank",
    "headingPathRecallRate",
    "pageNumberRecallRate",
    "blockTypeRecallRate",
    "structureRecallRate",
    "rejectionAccuracy",
)


def build_multipart(source: str, path: Path) -> tuple[bytes, str]:
    boundary = f"----java-interview-rag-{uuid.uuid4().hex}"
    parts = [
        f"--{boundary}\r\n"
        'Content-Disposition: form-data; name="source"\r\n\r\n'
        f"{source}\r\n",
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="file"; filename="{path.name}"\r\n'
        f"Content-Type: {'application/pdf' if path.suffix.lower() == '.pdf' else 'text/markdown; charset=utf-8'}\r\n\r\n",
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


def import_corpus(base_url: str, timeout: float, include_structured_pdf: bool) -> list[dict]:
    imported_documents = []
    sources = CORPUS_SOURCES + ((STRUCTURED_CORPUS_SOURCE,) if include_structured_pdf else ())
    for source in sources:
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
        imported_documents.append({
            "source": source,
            "imported": result["imported"],
            "chunkCount": result["chunkCount"],
        })
    return imported_documents


def evaluate(base_url: str, timeout: float, compare_query_decomposition: bool,
             evaluate_structure: bool) -> dict:
    path = "/api/knowledge/evaluations/retrieval"
    if compare_query_decomposition:
        path += "/query-decomposition"
    elif evaluate_structure:
        path += "/structure"
    request = urllib.request.Request(
        f"{base_url}{path}",
        method="GET",
    )
    return request_json(request, timeout)


def validate_report(report: dict, structure: bool = False) -> list[str]:
    errors = []
    expected_total = 3 if structure else 20
    expected_answerable = 3 if structure else 17
    expected_no_answer = 0 if structure else 3
    if report.get("totalCaseCount") != expected_total:
        errors.append(f"固定评估题数量不是 {expected_total}")
    if report.get("answerableCaseCount") != expected_answerable:
        errors.append(f"可回答题数量不是 {expected_answerable}")
    if report.get("noAnswerCaseCount") != expected_no_answer:
        errors.append(f"无答案题数量不是 {expected_no_answer}")
    if report.get("structureCaseCount") != (3 if structure else 0):
        errors.append("结构评估题数量不正确")
    for field in RATE_FIELDS:
        value = report.get(field)
        if not isinstance(value, (int, float)) or not 0.0 <= value <= 1.0:
            errors.append(f"指标 {field} 缺失或超出 0 到 1")
    return errors


def validate_comparison(comparison: dict) -> list[str]:
    errors = []
    if comparison.get("candidateStrategy") != "deterministic-clause-rrf-v1":
        errors.append("候选查询拆分策略标识不正确")
    if not isinstance(comparison.get("decomposedCaseCount"), int):
        errors.append("拆分题数量缺失")
    if not isinstance(comparison.get("additionalQueryCount"), int):
        errors.append("额外查询数量缺失")
    queries = comparison.get("retrievalQueries")
    if not isinstance(queries, dict) or len(queries) != 20:
        errors.append("逐题查询计划不是 20 题")
    for name in ("baseline", "candidate"):
        report = comparison.get(name)
        if not isinstance(report, dict):
            errors.append(f"{name} 报告缺失")
        else:
            errors.extend(f"{name}: {error}" for error in validate_report(report))
    return errors


def print_summary(report: dict) -> None:
    print("\n检索基线摘要")
    print(f"- Hit@1: {report['hitAt1Rate']:.4f}")
    print(f"- Mean Recall@5: {report['meanRecallAt5']:.4f}")
    print(f"- MRR: {report['meanReciprocalRank']:.4f}")
    print(f"- 标题路径召回率: {report['headingPathRecallRate']:.4f}")
    print(f"- 无答案拒答准确率: {report['rejectionAccuracy']:.4f}")


def print_structure_summary(report: dict) -> None:
    print("\nPDF 结构检索摘要")
    print(f"- Hit@1: {report['hitAt1Rate']:.4f}")
    print(f"- 标题路径召回率: {report['headingPathRecallRate']:.4f}")
    print(f"- 页码召回率: {report['pageNumberRecallRate']:.4f}")
    print(f"- 内容块类型召回率: {report['blockTypeRecallRate']:.4f}")
    print(f"- 完整结构召回率: {report['structureRecallRate']:.4f}")


def print_comparison_summary(comparison: dict) -> None:
    print(f"\n查询拆分策略: {comparison['candidateStrategy']}")
    print(f"- 拆分题数: {comparison['decomposedCaseCount']}")
    print(f"- 额外查询数: {comparison['additionalQueryCount']}")
    print("\n原查询", end="")
    print_summary(comparison["baseline"])
    print("\n查询拆分候选", end="")
    print_summary(comparison["candidate"])


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="导入固定语料并运行真实 RAG 检索基线")
    parser.add_argument("--base-url", default="http://127.0.0.1:18080", help="后端服务地址")
    parser.add_argument("--timeout", type=float, default=120.0, help="单次 HTTP 请求超时秒数")
    parser.add_argument("--output", type=Path, help="可选的评估结果 JSON 输出路径")
    parser.add_argument(
        "--allow-embedding-calls",
        action="store_true",
        help="确认允许语料导入和检索评估产生真实 Embedding 调用",
    )
    mode = parser.add_mutually_exclusive_group()
    mode.add_argument(
        "--compare-query-decomposition",
        action="store_true",
        help="比较原查询与确定性子句拆分策略，不改变生产检索",
    )
    mode.add_argument(
        "--evaluate-structure",
        action="store_true",
        help="导入合成结构化 PDF 并评估标题、页码和内容块类型召回",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if not args.allow_embedding_calls:
        print(
            "未执行评估：语料导入或检索可能产生真实 Embedding 调用；"
            "确认费用授权后请显式添加 --allow-embedding-calls。",
            file=sys.stderr,
        )
        return 2
    base_url = args.base_url.rstrip("/")
    try:
        corpus = import_corpus(base_url, args.timeout, args.evaluate_structure)
        evaluation_result = evaluate(
            base_url, args.timeout, args.compare_query_decomposition, args.evaluate_structure)
    except (OSError, RuntimeError, ValueError, json.JSONDecodeError) as exception:
        print(f"评估执行失败：{exception}", file=sys.stderr)
        return 1

    errors = (validate_comparison(evaluation_result)
              if args.compare_query_decomposition
              else validate_report(evaluation_result, args.evaluate_structure))
    if errors:
        for error in errors:
            print(f"评估报告无效：{error}", file=sys.stderr)
        return 1
    if args.output:
        output = {
            "runAt": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
            "embeddingModel": "text-embedding-v3",
            "collection": "java-interview-rag-chunks",
            "corpusDocumentCount": len(corpus),
            "importedDocumentCount": sum(1 for document in corpus if document["imported"]),
            "embeddedChunkCountThisRun": sum(document["chunkCount"] for document in corpus),
            "note": (
                "独立知识库中同时存在规划报告；本次比较原查询与确定性查询拆分，只执行 Embedding 检索，不调用 Chat。"
                if args.compare_query_decomposition else
                "本次使用合成两页 PDF 评估标题、页码和内容块类型召回，只执行 Embedding 检索，不调用 Chat。"
                if args.evaluate_structure else
                "独立知识库中同时存在规划报告；报告保留真实竞争来源。本次只执行 Embedding 检索，不调用 Chat。"
            ),
            "report": evaluation_result,
        }
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(output, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print(f"评估结果已写入：{args.output}")
    if args.compare_query_decomposition:
        print_comparison_summary(evaluation_result)
    elif args.evaluate_structure:
        print_structure_summary(evaluation_result)
    else:
        print_summary(evaluation_result)
    print(json.dumps(evaluation_result, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
