<script setup lang="ts">
import { Play } from "@lucide/vue";
import { shallowRef } from "vue";

import type { EvaluationSplit, RetrievalExperimentStartRequest, RetrievalStrategy } from "@/api/evaluationExperiment";

defineProps<{ busy: boolean }>();

const emit = defineEmits<{
  start: [payload: RetrievalExperimentStartRequest];
}>();

const name = shallowRef("Hybrid DEV 候选");
const strategy = shallowRef<RetrievalStrategy>("HYBRID");
const split = shallowRef<EvaluationSplit>("DEV");

function submit(): void {
  const normalized = name.value.trim();
  if (normalized) emit("start", { name: normalized, strategy: strategy.value, split: split.value });
}
</script>

<template>
  <form class="experiment-form" @submit.prevent="submit">
    <label class="form-field">
      <span>实验名称</span>
      <input v-model="name" maxlength="100" required />
    </label>
    <label class="form-field">
      <span>检索策略</span>
      <select v-model="strategy">
        <option value="DENSE">Dense</option>
        <option value="SPARSE">Sparse</option>
        <option value="HYBRID">Hybrid</option>
      </select>
    </label>
    <label class="form-field">
      <span>数据切分</span>
      <select v-model="split">
        <option value="DEV">DEV · 20 题</option>
        <option value="HOLDOUT">HOLDOUT · 6 题</option>
      </select>
    </label>
    <button class="run-button" type="submit" :disabled="busy || !name.trim()">
      <Play :size="16" aria-hidden="true" />
      {{ busy ? "正在创建" : "启动实验" }}
    </button>
  </form>
</template>

<style scoped>
.experiment-form { display: grid; grid-template-columns: minmax(14rem, 2fr) minmax(9rem, 1fr) minmax(9rem, 1fr) auto; align-items: end; gap: var(--space-md); }
.form-field { display: grid; gap: var(--space-2xs); color: var(--color-muted); font-size: var(--text-xs); }
.form-field input, .form-field select { width: 100%; min-height: 2.75rem; padding-inline: var(--space-sm); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); color: var(--color-ink); background: var(--color-surface); font: inherit; }
.run-button { display: inline-flex; min-height: 2.75rem; align-items: center; gap: var(--space-xs); padding-inline: var(--space-md); border: var(--rule-thin) solid var(--color-accent); border-radius: var(--radius-control); color: var(--color-accent-ink); background: var(--color-accent); font: inherit; font-weight: 700; }
@media (max-width: 56rem) { .experiment-form { grid-template-columns: 1fr 1fr; } }
@media (max-width: 34rem) { .experiment-form { grid-template-columns: minmax(0, 1fr); } .run-button { justify-content: center; } }
</style>
