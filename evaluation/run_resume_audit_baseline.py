from __future__ import annotations

import argparse
import datetime
import json
import sys
import time
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


def request_json(base_url: str, path: str, method: str, timeout: float) -> dict:
    request = urllib.request.Request(
        f"{base_url}{path}",
        data=b"" if method == "POST" else None,
        method=method,
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exception:
        detail = exception.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {exception.code}: {detail}") from exception


def wait_for_terminal_run(
    base_url: str, run: dict, timeout: float, poll_interval: float
) -> dict:
    run_id = run.get("runId")
    if not isinstance(run_id, str) or not run_id:
        raise ValueError("评估启动响应缺少运行 ID")
    print(f"评估运行 ID：{run_id}")
    deadline = time.monotonic() + timeout
    while run.get("status") not in {"SUCCEEDED", "PARTIAL_FAILED"}:
        if time.monotonic() >= deadline:
            raise TimeoutError(f"等待运行 {run_id} 完成超时")
        time.sleep(poll_interval)
        run = request_json(
            base_url,
            f"/api/interviews/evaluations/resume-audits/{run_id}",
            "GET",
            min(30.0, timeout),
        )
        print(
            f"- {run.get('status')}: 成功 {run.get('succeededCaseCount', 0)}，"
            f"失败 {run.get('failedCaseCount', 0)}，待处理 {run.get('pendingCaseCount', 0)}"
        )
    return run


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
    parser.add_argument("--timeout", type=float, default=900.0, help="等待运行终态的总秒数")
    parser.add_argument("--poll-interval", type=float, default=2.0, help="状态查询间隔秒数")
    parser.add_argument("--run-id", help="只恢复查询指定运行；不会新建运行")
    parser.add_argument(
        "--resume-failed",
        action="store_true",
        help="与 --run-id 一起使用，显式重试该运行中未成功的样本",
    )
    parser.add_argument("--output", type=Path, help="可选的脱敏评估结果 JSON 输出路径")
    parser.add_argument(
        "--allow-chat-calls",
        action="store_true",
        help="确认允许本次固定评估产生 11 次真实 Chat 调用",
    )
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if args.timeout <= 0 or args.poll_interval <= 0:
        print("--timeout 和 --poll-interval 必须大于 0。", file=sys.stderr)
        return 2
    if args.resume_failed and not args.run_id:
        print("--resume-failed 必须与 --run-id 一起使用。", file=sys.stderr)
        return 2
    may_start_calls = args.run_id is None or args.resume_failed
    if may_start_calls and not args.allow_chat_calls:
        print(
            "未执行评估：启动或恢复可能产生真实 Chat 调用；"
            "确认费用授权后请显式添加 --allow-chat-calls。",
            file=sys.stderr,
        )
        return 2
    base_url = args.base_url.rstrip("/")
    try:
        if args.run_id:
            path = f"/api/interviews/evaluations/resume-audits/{args.run_id}"
            if args.resume_failed:
                path += "/resume"
            run = request_json(
                base_url, path, "POST" if args.resume_failed else "GET", min(30.0, args.timeout)
            )
        else:
            run = request_json(
                base_url,
                "/api/interviews/evaluations/resume-audits",
                "POST",
                min(30.0, args.timeout),
            )
        run = wait_for_terminal_run(base_url, run, args.timeout, args.poll_interval)
        report = run.get("report")
        if not isinstance(report, dict):
            raise ValueError("评估已结束但聚合报告缺失")
    except TimeoutError as exception:
        print(
            f"{exception}；请使用 --run-id 恢复查询，不要新建运行。",
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
    if run.get("status") == "PARTIAL_FAILED":
        print(
            f"评估存在失败样本；确认费用授权后可使用 --run-id {run['runId']} "
            "--resume-failed --allow-chat-calls 只重试未成功样本。",
            file=sys.stderr,
        )
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
