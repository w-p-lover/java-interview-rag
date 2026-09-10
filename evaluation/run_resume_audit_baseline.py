from __future__ import annotations

import argparse
import datetime
import json
import sys
import urllib.error
import urllib.request
from pathlib import Path


RATE_FIELDS = (
    "auditSuccessRate",
    "structureParseSuccessRate",
    "factAccuracyRate",
    "expectedFactRecallRate",
    "evidenceLegalRate",
    "missingInformationRecallRate",
    "hallucinationRate",
    "riskSignalRecallRate",
)

FACT_TYPES = (
    "EDUCATION",
    "EMPLOYMENT",
    "PROJECT",
    "TECHNOLOGY",
    "RESPONSIBILITY",
    "TIME",
    "RESULT",
)


def request_json(base_url: str, timeout: float) -> dict:
    request = urllib.request.Request(
        f"{base_url}/api/interviews/evaluations/resume-audits",
        data=b"",
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exception:
        detail = exception.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {exception.code}: {detail}") from exception


def validate_report(report: dict) -> list[str]:
    errors = []
    if report.get("totalCaseCount") != 11:
        errors.append("固定简历审计样本数量不是 11")
    if report.get("datasetVersion") != "resume-evidence-audit-dataset-v7":
        errors.append("简历审计数据集版本不正确")
    metrics = report.get("metrics")
    if not isinstance(metrics, dict):
        return errors + ["简历审计指标缺失"]
    for field in RATE_FIELDS:
        value = metrics.get(field)
        if not isinstance(value, (int, float)) or not 0.0 <= value <= 1.0:
            errors.append(f"指标 {field} 缺失或超出 0 到 1")
    if metrics.get("factCapacityLimit") != 30:
        errors.append("事实容量上限不是 30")
    if not isinstance(metrics.get("maxCandidateFactCount"), int):
        errors.append("最大候选事实数量缺失")
    if not isinstance(metrics.get("capacityReachedCaseCount"), int):
        errors.append("触达容量上限的样本数量缺失")
    fact_type_metrics = report.get("factTypeMetrics")
    if not isinstance(fact_type_metrics, list) or [
        item.get("type") for item in fact_type_metrics if isinstance(item, dict)
    ] != list(FACT_TYPES):
        errors.append("事实类型指标不完整或顺序不正确")
    else:
        for item in fact_type_metrics:
            for field in ("recallRate", "accuracyRate"):
                value = item.get(field)
                if not isinstance(value, (int, float)) or not 0.0 <= value <= 1.0:
                    errors.append(f"事实类型 {item.get('type')} 的 {field} 无效")
    cases = report.get("cases")
    if not isinstance(cases, list) or len(cases) != 11:
        errors.append("逐样本结果不是 11 条")
    forbidden_fields = {"resumeText", "confirmedFacts", "evidence", "suggestions", "risks"}
    if forbidden_fields.intersection(report):
        errors.append("报告顶层包含不应保存的审计正文")
    for case in cases if isinstance(cases, list) else []:
        if forbidden_fields.intersection(case):
            errors.append(f"样本 {case.get('caseId', 'unknown')} 包含不应保存的审计正文")
    return errors


def print_summary(report: dict) -> None:
    metrics = report["metrics"]
    print("\n简历证据审计基线摘要")
    print(f"- 通过样本: {report['passedCaseCount']}/{report['totalCaseCount']}")
    print(f"- 审计成功率: {metrics['auditSuccessRate']:.4f}")
    print(f"- 结构解析成功率: {metrics['structureParseSuccessRate']:.4f}")
    print(f"- 事实准确率: {metrics['factAccuracyRate']:.4f}")
    print(f"- 预期事实召回率: {metrics['expectedFactRecallRate']:.4f}")
    print(f"- 证据合法率: {metrics['evidenceLegalRate']:.4f}")
    print(f"- 信息缺失识别率: {metrics['missingInformationRecallRate']:.4f}")
    print(f"- 虚构率: {metrics['hallucinationRate']:.4f}")
    responsibility = next(
        item for item in report["factTypeMetrics"] if item["type"] == "RESPONSIBILITY"
    )
    print(f"- 职责事实召回率: {responsibility['recallRate']:.4f}")
    print(
        f"- 最大候选事实数: {metrics['maxCandidateFactCount']} / "
        f"{metrics['factCapacityLimit']}"
    )
    print(f"- P95 耗时: {metrics['p95ElapsedMillis']} ms")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="运行十一类固定简历证据审计真实模型基线")
    parser.add_argument("--base-url", default="http://127.0.0.1:18080", help="后端服务地址")
    parser.add_argument("--timeout", type=float, default=900.0, help="整批 HTTP 请求超时秒数")
    parser.add_argument("--output", type=Path, help="可选的脱敏评估结果 JSON 输出路径")
    parser.add_argument(
        "--allow-chat-calls",
        action="store_true",
        help="确认允许本次固定评估产生 11 次真实 Chat 调用",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if not args.allow_chat_calls:
        print(
            "未执行评估：该命令会产生 11 次真实 Chat 调用；"
            "确认费用授权后请显式添加 --allow-chat-calls。",
            file=sys.stderr,
        )
        return 2
    try:
        report = request_json(args.base_url.rstrip("/"), args.timeout)
    except TimeoutError as exception:
        print(
            "评估客户端等待超时；服务端可能仍在继续执行剩余 Chat 调用。"
            "请先查询 /api/interviews/resume-audits/requests 核对本次 Prompt 版本的请求数量，"
            "不要直接重跑。",
            file=sys.stderr,
        )
        return 1
    except (OSError, RuntimeError, ValueError, json.JSONDecodeError) as exception:
        print(f"评估执行失败：{exception}", file=sys.stderr)
        return 1

    errors = validate_report(report)
    if errors:
        for error in errors:
            print(f"评估报告无效：{error}", file=sys.stderr)
        return 1

    if args.output:
        output = {
            "runAt": datetime.datetime.now().astimezone().isoformat(timespec="seconds"),
            "note": "十一类固定样本；报告不含简历、事实证据、风险或建议正文。",
            "report": report,
        }
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(
            json.dumps(output, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
        print(f"评估结果已写入：{args.output}")

    print_summary(report)
    print(json.dumps(report, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
