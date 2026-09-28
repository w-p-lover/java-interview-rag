<script setup lang="ts">
import { Check, GitCompareArrows, RotateCcw } from "@lucide/vue";
import { computed, shallowRef } from "vue";

import type { ExperimentDecision, RetrievalExperimentComparison } from "@/api/evaluationExperiment";

const props = defineProps<{
  comparison: Readonly<RetrievalExperimentComparison> | null;
  baselineSelected: boolean;
  candidateSelected: boolean;
  busy: boolean;
}>();

const emit = defineEmits<{
  compare: [];
  decide: [decision: ExperimentDecision, note: string];
}>();

const note = shallowRef("");
const canCompare = computed(() => props.baselineSelected && props.candidateSelected && !props.busy);

function formatMetric(code: string, value: number): string {
  return code === "latencyP95Ms" ? `${value.toFixed(1)} ms` : `${(value * 100).toFixed(1)}%`;
}

function deltaClass(code: string, delta: number, higherIsBetter: boolean): string {
  if (Math.abs(delta) < 0.000001) return "neutral";
  const improved = higherIsBetter ? delta > 0 : delta < 0;
  return improved ? "improved" : "regressed";
}
</script>

<template>
  <section id="evaluation-comparison" class="comparison-section" tabindex="-1">
    <header class="comparison-heading">
      <div><h2>基线与候选</h2><p>只有数据集、切分、语料和索引契约一致的成功运行可以比较。</p></div>
      <button class="compare-button" type="button" :disabled="!canCompare" @click="emit('compare')">
        <GitCompareArrows :size="17" aria-hidden="true" />
        {{ busy ? "正在比较" : "生成对照" }}
      </button>
    </header>
    <div v-if="comparison" class="comparison-result">
      <div class="run-pair">
        <div><span>基线</span><strong>{{ comparison.baseline.run.name }}</strong><small>{{ comparison.baseline.run.strategy }}</small></div>
        <div><span>候选</span><strong>{{ comparison.candidate.run.name }}</strong><small>{{ comparison.candidate.run.strategy }}</small></div>
        <div class="guard-result" :class="comparison.safeguardsPassed ? 'passed' : 'failed'">
          <span>守门结果</span><strong>{{ comparison.safeguardsPassed ? "建议进入人工采用" : "建议回退" }}</strong><small>{{ comparison.safeguardFailures.join("、") || "关键质量指标未退化" }}</small>
        </div>
      </div>
      <div class="metric-grid">
        <div v-for="metric in comparison.metrics" :key="metric.code" class="metric-row">
          <strong>{{ metric.label }}</strong>
          <span>{{ formatMetric(metric.code, metric.baseline) }}</span>
          <span>{{ formatMetric(metric.code, metric.candidate) }}</span>
          <span :class="deltaClass(metric.code, metric.delta, metric.higherIsBetter)">{{ metric.delta > 0 ? "+" : "" }}{{ formatMetric(metric.code, metric.delta) }}</span>
        </div>
      </div>
      <div class="decision-editor">
        <label><span>人工结论说明</span><textarea v-model="note" maxlength="500" rows="3" placeholder="记录采用理由、退化指标或后续动作"></textarea></label>
        <div class="decision-actions">
          <button type="button" :disabled="busy" @click="emit('decide', 'ADOPTED', note)"><Check :size="16" aria-hidden="true" />采用候选</button>
          <button type="button" class="reject" :disabled="busy" @click="emit('decide', 'REJECTED', note)"><RotateCcw :size="16" aria-hidden="true" />回退候选</button>
        </div>
        <p v-if="comparison.candidate.run.decision" class="saved-decision">已保存：{{ comparison.candidate.run.decision === "ADOPTED" ? "采用" : "回退" }}{{ comparison.candidate.run.decisionNote ? `，${comparison.candidate.run.decisionNote}` : "" }}</p>
      </div>
    </div>
    <div v-else class="comparison-empty">从实验运行中分别选择基线和候选。</div>
  </section>
</template>

<style scoped>
.comparison-section { display: grid; gap: var(--space-md); }
.comparison-heading { display: flex; align-items: end; justify-content: space-between; gap: var(--space-md); }.comparison-heading h2, .comparison-heading p { margin: 0; }.comparison-heading h2 { color: var(--color-ink); font-family: var(--font-display); font-size: var(--text-xl); }.comparison-heading p { margin-top: var(--space-2xs); color: var(--color-muted); font-size: var(--text-sm); }
.compare-button, .decision-actions button { display: inline-flex; min-height: 2.5rem; align-items: center; gap: var(--space-xs); padding-inline: var(--space-md); border: var(--rule-thin) solid var(--color-accent); border-radius: var(--radius-control); color: var(--color-accent-ink); background: var(--color-accent); font: inherit; font-weight: 700; }
.comparison-result { display: grid; gap: var(--space-lg); padding-block: var(--space-lg); border-block: var(--rule-thin) solid var(--color-rule-2); }.run-pair { display: grid; grid-template-columns: 1fr 1fr 1.4fr; gap: var(--space-md); }.run-pair > div { display: grid; gap: var(--space-2xs); padding-left: var(--space-md); border-left: 3px solid var(--color-rule-2); }.run-pair span, .run-pair small { color: var(--color-muted); font-size: var(--text-xs); }.run-pair .passed { border-color: var(--color-success); }.run-pair .failed { border-color: var(--color-error); }
.metric-grid { display: grid; }.metric-row { display: grid; grid-template-columns: minmax(10rem, 1.5fr) repeat(3, minmax(7rem, 1fr)); gap: var(--space-sm); padding: var(--space-sm) 0; border-top: var(--rule-thin) solid var(--color-rule); font-size: var(--text-sm); }.metric-row span { color: var(--color-muted); font-variant-numeric: tabular-nums; }.metric-row .improved { color: var(--color-success); }.metric-row .regressed { color: var(--color-error); }.metric-row .neutral { color: var(--color-muted); }
.decision-editor { display: grid; grid-template-columns: minmax(0, 1fr) auto; align-items: end; gap: var(--space-md); }.decision-editor label { display: grid; gap: var(--space-2xs); color: var(--color-muted); font-size: var(--text-xs); }.decision-editor textarea { width: 100%; padding: var(--space-sm); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); color: var(--color-ink); background: var(--color-surface); font: inherit; resize: vertical; }.decision-actions { display: flex; gap: var(--space-xs); }.decision-actions .reject { border-color: var(--color-rule-2); color: var(--color-ink-2); background: transparent; }.saved-decision { grid-column: 1 / -1; margin: 0; color: var(--color-muted); font-size: var(--text-sm); }
.comparison-empty { min-height: 10rem; display: grid; place-items: center; border-block: var(--rule-thin) solid var(--color-rule-2); color: var(--color-muted); }
@media (max-width: 48rem) { .comparison-heading { align-items: start; flex-direction: column; }.run-pair { grid-template-columns: minmax(0, 1fr); }.metric-row { grid-template-columns: minmax(8rem, 1.4fr) repeat(3, minmax(5rem, 1fr)); min-width: 35rem; }.metric-grid { overflow-x: auto; }.decision-editor { grid-template-columns: minmax(0, 1fr); }.decision-actions { flex-wrap: wrap; } }
</style>
