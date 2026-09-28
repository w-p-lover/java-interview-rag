<script setup lang="ts">
import { FlaskConical } from "@lucide/vue";
import { computed, shallowRef } from "vue";

import type { ExperimentDecision, RetrievalExperimentStartRequest } from "@/api/evaluationExperiment";
import RetrievalExperimentComparison from "@/components/evaluation/RetrievalExperimentComparison.vue";
import RetrievalExperimentRunList from "@/components/evaluation/RetrievalExperimentRunList.vue";
import RetrievalExperimentStartForm from "@/components/evaluation/RetrievalExperimentStartForm.vue";
import { useRetrievalExperiments } from "@/composables/useRetrievalExperiments";

const {
  runs, comparison, loading, actionKey, error,
  refresh, start, resume, compare, decide,
} = useRetrievalExperiments();

const baselineRunId = shallowRef("");
const candidateRunId = shallowRef("");
const busy = computed(() => Boolean(actionKey.value));

async function handleStart(payload: RetrievalExperimentStartRequest): Promise<void> {
  await start(payload);
}

async function handleCompare(): Promise<void> {
  if (baselineRunId.value && candidateRunId.value) {
    await compare(baselineRunId.value, candidateRunId.value);
  }
}

async function handleDecision(decision: ExperimentDecision, note: string): Promise<void> {
  if (baselineRunId.value && candidateRunId.value) {
    await decide(candidateRunId.value, baselineRunId.value, decision, note);
  }
}
</script>

<template>
  <div class="experiment-panel">
    <header id="evaluation-overview" class="page-heading" tabindex="-1">
      <div class="heading-mark" aria-hidden="true"><FlaskConical :size="24" /></div>
      <div><span>Stage 11.1</span><h1>检索实验中心</h1><p>固定数据、保存契约、比较结果，再由人工决定采用或回退。</p></div>
    </header>
    <section id="evaluation-start" class="start-band" tabindex="-1">
      <RetrievalExperimentStartForm :busy="actionKey === 'start'" @start="handleStart" />
    </section>
    <p v-if="error" class="error-banner" role="alert">{{ error }}</p>
    <RetrievalExperimentRunList
      :runs="runs"
      :loading="loading"
      :action-key="actionKey"
      :baseline-run-id="baselineRunId"
      :candidate-run-id="candidateRunId"
      @refresh="refresh()"
      @resume="resume"
      @baseline="baselineRunId = $event"
      @candidate="candidateRunId = $event"
    />
    <RetrievalExperimentComparison
      :comparison="comparison"
      :baseline-selected="Boolean(baselineRunId)"
      :candidate-selected="Boolean(candidateRunId)"
      :busy="busy"
      @compare="handleCompare"
      @decide="handleDecision"
    />
  </div>
</template>

<style scoped>
.experiment-panel { display: grid; min-width: 0; gap: var(--space-2xl); max-width: 82rem; margin-inline: auto; }
.experiment-panel > * { min-width: 0; }
.page-heading { display: grid; grid-template-columns: auto minmax(0, 1fr); align-items: start; gap: var(--space-md); padding-bottom: var(--space-lg); border-bottom: var(--rule-thin) solid var(--color-ink); }.heading-mark { display: inline-grid; width: 3rem; height: 3rem; place-items: center; border: var(--rule-thin) solid var(--color-ink); border-radius: var(--radius-control); color: var(--color-ink); background: var(--color-signal-rule); }.page-heading span { color: var(--color-muted); font-size: var(--text-xs); }.page-heading h1 { margin: var(--space-2xs) 0 0; color: var(--color-ink); font-family: var(--font-display); font-size: var(--text-2xl); }.page-heading p { max-width: 46rem; margin: var(--space-xs) 0 0; color: var(--color-muted); }
.start-band { padding: var(--space-lg); border-left: 3px solid var(--color-accent); background: var(--color-paper-2); }.error-banner { margin: 0; padding: var(--space-sm) var(--space-md); color: var(--color-error); background: var(--color-error-soft); }
</style>
