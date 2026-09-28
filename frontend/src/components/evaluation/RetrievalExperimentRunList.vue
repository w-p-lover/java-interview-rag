<script setup lang="ts">
import { RefreshCw, RotateCcw } from "@lucide/vue";

import type { RetrievalExperimentRunView } from "@/api/evaluationExperiment";

defineProps<{
  runs: readonly RetrievalExperimentRunView[];
  loading: boolean;
  actionKey: string;
  baselineRunId: string;
  candidateRunId: string;
}>();

const emit = defineEmits<{
  refresh: [];
  resume: [runId: string];
  baseline: [runId: string];
  candidate: [runId: string];
}>();

function statusLabel(status: string): string {
  return ({ QUEUED: "排队中", RUNNING: "运行中", SUCCEEDED: "已完成", PARTIAL_FAILED: "部分失败" } as Record<string, string>)[status] ?? status;
}
</script>

<template>
  <section id="evaluation-runs" class="run-section" tabindex="-1">
    <header class="section-heading">
      <div><h2>实验运行</h2><p>运行契约相同的重复提交会复用已有结果。</p></div>
      <button class="icon-button" type="button" title="刷新实验运行" aria-label="刷新实验运行" @click="emit('refresh')">
        <RefreshCw :size="17" aria-hidden="true" />
      </button>
    </header>
    <div v-if="loading" class="empty-row">正在读取实验运行…</div>
    <div v-else-if="runs.length" class="run-table-wrap">
      <table class="run-table">
        <thead><tr><th>实验</th><th>契约</th><th>进度</th><th>结论</th><th>对照选择</th><th><span class="visually-hidden">操作</span></th></tr></thead>
        <tbody>
          <tr v-for="item in runs" :key="item.run.runId">
            <td data-label="实验"><strong>{{ item.run.name }}</strong><span>{{ item.run.strategy }} / {{ item.run.datasetSplit }}</span></td>
            <td data-label="契约"><code :title="item.run.contractHash">{{ item.run.gitSha.slice(0, 8) }}</code><span>{{ item.run.datasetVersion }}</span></td>
            <td data-label="进度"><strong :class="`status-${item.run.status.toLowerCase()}`">{{ statusLabel(item.run.status) }}</strong><span>{{ item.succeededCaseCount }}/{{ item.run.totalCaseCount }} 成功<span v-if="item.failedCaseCount">，{{ item.failedCaseCount }} 失败</span></span></td>
            <td data-label="结论"><strong>{{ item.run.decision === "ADOPTED" ? "采用" : item.run.decision === "REJECTED" ? "回退" : "未决策" }}</strong><span>{{ item.run.decisionNote || item.run.errorType || "-" }}</span></td>
            <td data-label="对照选择">
              <div class="selection-actions">
                <button type="button" :class="{ selected: baselineRunId === item.run.runId }" :disabled="item.run.status !== 'SUCCEEDED'" @click="emit('baseline', item.run.runId)">基线</button>
                <button type="button" :class="{ selected: candidateRunId === item.run.runId }" :disabled="item.run.status !== 'SUCCEEDED'" @click="emit('candidate', item.run.runId)">候选</button>
              </div>
            </td>
            <td data-label="操作">
              <button v-if="item.run.status === 'PARTIAL_FAILED'" class="icon-button" type="button" title="恢复未成功样本" aria-label="恢复未成功样本" :disabled="actionKey === `resume:${item.run.runId}`" @click="emit('resume', item.run.runId)"><RotateCcw :size="16" aria-hidden="true" /></button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    <div v-else class="empty-row">还没有实验运行。</div>
  </section>
</template>

<style scoped>
.run-section { display: grid; min-width: 0; gap: var(--space-md); overflow: hidden; }
.section-heading { display: flex; align-items: end; justify-content: space-between; gap: var(--space-md); }
.section-heading h2, .section-heading p { margin: 0; }.section-heading h2 { color: var(--color-ink); font-family: var(--font-display); font-size: var(--text-xl); }.section-heading p { margin-top: var(--space-2xs); color: var(--color-muted); font-size: var(--text-sm); }
.icon-button { display: inline-grid; width: 2.5rem; height: 2.5rem; padding: 0; place-items: center; border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); color: var(--color-ink-2); background: var(--color-surface); }
.run-table-wrap { width: 100%; min-width: 0; overflow-x: auto; border-block: var(--rule-thin) solid var(--color-rule-2); }
.run-table { width: 100%; min-width: 58rem; border-collapse: collapse; text-align: left; }
.run-table th { padding: var(--space-sm); color: var(--color-muted); font-size: var(--text-xs); font-weight: 600; }.run-table td { padding: var(--space-md) var(--space-sm); border-top: var(--rule-thin) solid var(--color-rule); vertical-align: top; }.run-table td > strong, .run-table td > span, .run-table td > code { display: block; }.run-table td > span { max-width: 18rem; margin-top: var(--space-2xs); color: var(--color-muted); font-size: var(--text-xs); overflow-wrap: anywhere; }.run-table code { color: var(--color-ink-2); font-family: var(--font-mono); font-size: var(--text-xs); }
.selection-actions { display: flex; gap: var(--space-xs); }.selection-actions button { min-height: 2rem; padding-inline: var(--space-sm); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); color: var(--color-ink-2); background: transparent; }.selection-actions button.selected { border-color: var(--color-accent); color: var(--color-accent-ink); background: var(--color-accent-soft); }
.status-succeeded { color: var(--color-success); }.status-partial_failed { color: var(--color-error); }.status-running, .status-queued { color: var(--color-accent-strong); }
.empty-row { min-height: 10rem; display: grid; place-items: center; border-block: var(--rule-thin) solid var(--color-rule-2); color: var(--color-muted); }
.visually-hidden { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); }
@media (max-width: 42rem) {
  .run-table-wrap { overflow-x: hidden; }
  .run-table { display: block; min-width: 0; }
  .run-table thead { display: none; }
  .run-table tbody { display: grid; gap: var(--space-md); }
  .run-table tr { display: grid; width: 100%; padding-block: var(--space-sm); border-top: var(--rule-thin) solid var(--color-rule-2); }
  .run-table td { display: grid; grid-template-columns: 5.5rem minmax(0, 1fr); width: 100%; padding: var(--space-xs) 0; border-top: 0; }
  .run-table td::before { grid-row: 1 / span 2; content: attr(data-label); color: var(--color-muted); font-size: var(--text-xs); }
  .run-table td > span { max-width: none; }
  .selection-actions { flex-wrap: wrap; }
}
</style>
