import type {
  ResumeEvidenceAuditResult,
  ResumeEvidenceFactType,
} from "@/api/interview";

const factTypeLabels: Record<ResumeEvidenceFactType, string> = {
  EDUCATION: "教育",
  EMPLOYMENT: "任职",
  PROJECT: "项目",
  TECHNOLOGY: "技术",
  RESPONSIBILITY: "职责",
  TIME: "时间",
  RESULT: "结果",
};

const factTypeOrder = Object.keys(factTypeLabels) as ResumeEvidenceFactType[];

function normalizeLine(value: string): string {
  return value.replace(/\s+/g, " ").trim();
}

function quoteEvidence(value: string): string {
  return value
    .split(/\r?\n/)
    .map((line) => `   > ${line.trim()}`)
    .join("\n");
}

function appendList(lines: string[], title: string, values: readonly string[]): void {
  lines.push(`## ${title}`, "");
  if (!values.length) {
    lines.push("无。", "");
    return;
  }
  values.forEach((value, index) => lines.push(`${index + 1}. ${normalizeLine(value)}`));
  lines.push("");
}

export function createResumeEvidenceAuditMarkdown(
  result: ResumeEvidenceAuditResult,
  exportedAt = new Date(),
): string {
  const lines = [
    "# 简历证据审计报告",
    "",
    `- 请求标识：${result.requestId}`,
    `- Prompt 版本：${result.promptVersion}`,
    `- 导出时间：${exportedAt.toLocaleString("zh-CN", { hour12: false })}`,
    `- 已确认事实：${result.confirmedFacts.length} 项`,
    "",
    "> 本报告只整理审计时已由原文逐字支持的事实；未列出的能力不代表候选人不具备。",
    "",
    "## 已确认事实",
    "",
  ];

  for (const type of factTypeOrder) {
    const facts = result.confirmedFacts.filter((fact) => fact.type === type);
    if (!facts.length) {
      continue;
    }
    lines.push(`### ${factTypeLabels[type]}（${facts.length} 项）`, "");
    facts.forEach((fact, index) => {
      lines.push(`${index + 1}. ${normalizeLine(fact.statement)}`);
      lines.push(quoteEvidence(fact.evidence), "");
    });
  }

  if (!result.confirmedFacts.length) {
    lines.push("无。", "");
  }
  appendList(lines, "待补信息", result.missingInformation);
  appendList(lines, "人工核对", result.risks);
  appendList(lines, "修改建议", result.suggestions);
  return `${lines.join("\n").trimEnd()}\n`;
}

export function downloadResumeEvidenceAudit(result: ResumeEvidenceAuditResult): string {
  const exportedAt = new Date();
  const date = exportedAt.toISOString().slice(0, 10);
  const requestId = result.requestId.replace(/[^a-zA-Z0-9]/g, "").slice(0, 8) || "report";
  const filename = `简历证据审计-${date}-${requestId}.md`;
  const blob = new Blob(["\uFEFF", createResumeEvidenceAuditMarkdown(result, exportedAt)], {
    type: "text/markdown;charset=utf-8",
  });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = filename;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 0);
  return filename;
}
