import type { InterviewSession } from "@/api/interview";

function normalizeLine(value: string): string {
  return value.replace(/\s+/g, " ").trim();
}

function appendList(
  lines: string[],
  title: string,
  values: readonly string[],
  headingLevel = "###",
): void {
  lines.push(`${headingLevel} ${title}`, "");
  if (!values.length) {
    lines.push("无。", "");
    return;
  }
  values.forEach((value, index) => lines.push(`${index + 1}. ${normalizeLine(value)}`));
  lines.push("");
}

function appendQuotedText(lines: string[], value: string): void {
  value.trim().split(/\r?\n/).forEach((line) => lines.push(`> ${line}`));
  lines.push("");
}

export function createInterviewSessionReportMarkdown(
  session: InterviewSession,
  exportedAt = new Date(),
): string {
  if (session.status !== "COMPLETED" || !session.report) {
    throw new Error("只有已完成的面试可以导出报告");
  }

  const modeLabel = session.mode === "PROJECT_DEEP_DIVE" ? "项目深挖" : "模拟面试";
  const report = session.report;
  const lines = [
    "# 模拟面试复盘报告",
    "",
    `- 目标岗位：${normalizeLine(session.targetRole)}`,
    `- 面试模式：${modeLabel}`,
    `- 会话标识：${session.id}`,
    `- 完成时间：${new Date(report.completedAt).toLocaleString("zh-CN", { hour12: false })}`,
    `- 导出时间：${exportedAt.toLocaleString("zh-CN", { hour12: false })}`,
    `- 回答题数：${report.answeredQuestions}`,
    "",
    "## 综合评分",
    "",
    `- 综合分：${report.overallAverageScore}`,
    `- 技术分：${report.averageTechnicalScore}`,
    `- 完整度：${report.averageCompletenessScore}`,
    `- 表达分：${report.averageCommunicationScore}`,
    "",
  ];

  appendList(lines, "本场表现", report.strengths);
  appendList(lines, "需要补充", report.gaps);
  lines.push("## 逐题复盘", "");

  const answers = [...session.answers].sort((left, right) => left.questionIndex - right.questionIndex);
  answers.forEach((answer, answerIndex) => {
    const question = session.questions[answer.questionIndex];
    const evaluation = answer.evaluation;
    lines.push(`### 第 ${answerIndex + 1} 题`, "");
    lines.push(`**题目：** ${normalizeLine(question?.question ?? `题号 ${answer.questionIndex + 1}`)}`, "");
    if (question?.objective) {
      lines.push(`**考察目标：** ${normalizeLine(question.objective)}`, "");
    }
    lines.push("**我的回答：**", "");
    appendQuotedText(lines, answer.answer);
    lines.push(
      `**评分：** 综合 ${evaluation.overallScore} / 技术 ${evaluation.technicalScore}`
        + ` / 完整 ${evaluation.completenessScore} / 表达 ${evaluation.communicationScore}`,
      "",
    );
    appendList(lines, "回答优点", evaluation.strengths, "####");
    appendList(lines, "回答遗漏", evaluation.gaps, "####");
    lines.push("#### 改进建议", "", normalizeLine(evaluation.improvementSuggestion), "");
    if (evaluation.answerEvidence.length) {
      appendList(lines, "回答中的有效依据", evaluation.answerEvidence, "####");
    }
    if (evaluation.knowledgeEvidence.length) {
      lines.push("#### 参考知识来源", "");
      evaluation.knowledgeEvidence.forEach((evidence, index) => {
        const heading = evidence.headingPath.length ? ` / ${evidence.headingPath.join(" > ")}` : "";
        lines.push(`${index + 1}. ${evidence.source}${heading}`);
        lines.push(`   > ${normalizeLine(evidence.quote)}`);
      });
      lines.push("");
    }
    lines.push("---", "");
  });

  return `${lines.join("\n").trimEnd()}\n`;
}

export function downloadInterviewSessionReport(session: InterviewSession): string {
  const exportedAt = new Date();
  const date = exportedAt.toISOString().slice(0, 10);
  const safeRole = session.targetRole.replace(/[\\/:*?"<>|\s]+/g, "-").slice(0, 24) || "面试";
  const filename = `模拟面试复盘-${safeRole}-${date}.md`;
  const blob = new Blob(["\uFEFF", createInterviewSessionReportMarkdown(session, exportedAt)], {
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
