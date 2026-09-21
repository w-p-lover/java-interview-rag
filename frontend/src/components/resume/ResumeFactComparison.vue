<script setup lang="ts">
import {
  ArrowRight,
  CheckCheck,
  GitCompareArrows,
  Minus,
  PencilLine,
  Plus,
} from "@lucide/vue";
import { computed } from "vue";

import type {
  ResumeDetail,
  ResumeEvidenceFactChange,
  ResumeVersion,
} from "@/api/resumeCenter";
import type {
  ResumeEvidenceConfirmedFact,
  ResumeEvidenceFactType,
} from "@/api/interview";
import { useResumeFactComparison } from "@/composables/useResumeFactComparison";

const props = defineProps<{
  detail: ResumeDetail | null;
}>();

const comparison = useResumeFactComparison(() => props.detail);
const hasDifferences = computed(() => Boolean(
  comparison.result.value
  && (comparison.result.value.addedFacts.length
    || comparison.result.value.removedFacts.length
    || comparison.result.value.changedFacts.length),
));

const factTypeLabels: Record<ResumeEvidenceFactType, string> = {
  EDUCATION: "教育",
  EMPLOYMENT: "任职",
  PROJECT: "项目",
  TECHNOLOGY: "技术",
  RESPONSIBILITY: "职责",
  TIME: "时间",
  RESULT: "结果",
};

function versionLabel(version: ResumeVersion): string {
  return `v${version.versionNumber} · ${version.originalFilename}`;
}

function factKey(fact: ResumeEvidenceConfirmedFact): string {
  return `${fact.type}|${fact.statement}|${fact.evidence}`;
}

function changeKey(change: ResumeEvidenceFactChange): string {
  return `${factKey(change.before)}|${factKey(change.after)}`;
}
</script>

<template>
  <section class="comparison" aria-labelledby="resume-fact-comparison-title">
    <header class="comparison-heading">
      <div>
        <h2 id="resume-fact-comparison-title">事实版本比较</h2>
        <p>仅含已确认事实</p>
      </div>
      <span v-if="comparison.result.value">
        {{ comparison.result.value.unchangedFacts.length }} 项未变化
      </span>
    </header>

    <p v-if="!detail" class="empty-state">选择简历记录后比较版本事实。</p>
    <p v-else-if="detail.versions.length < 2" class="empty-state">至少需要两个简历版本。</p>
    <template v-else>
      <div class="comparison-controls">
        <label>
          <span>基准版本</span>
          <select v-model="comparison.baseVersionId.value" :disabled="comparison.loading.value">
            <option v-for="version in detail.versions" :key="version.id" :value="version.id">
              {{ versionLabel(version) }}
            </option>
          </select>
        </label>
        <ArrowRight class="direction-icon" :size="18" aria-hidden="true" />
        <label>
          <span>目标版本</span>
          <select v-model="comparison.targetVersionId.value" :disabled="comparison.loading.value">
            <option v-for="version in detail.versions" :key="version.id" :value="version.id">
              {{ versionLabel(version) }}
            </option>
          </select>
        </label>
        <button
          type="button"
          :disabled="!comparison.canCompare.value"
          @click="comparison.compare"
        >
          <GitCompareArrows :size="17" aria-hidden="true" />
          <span>{{ comparison.loading.value ? "比较中…" : "比较事实" }}</span>
        </button>
      </div>

      <p v-if="comparison.error.value" class="comparison-error" role="alert">
        {{ comparison.error.value }}
      </p>

      <template v-if="comparison.result.value">
        <div class="comparison-summary" aria-live="polite">
          <span class="added"><Plus :size="15" aria-hidden="true" />新增 <strong>{{ comparison.result.value.addedFacts.length }}</strong></span>
          <span class="removed"><Minus :size="15" aria-hidden="true" />删除 <strong>{{ comparison.result.value.removedFacts.length }}</strong></span>
          <span class="changed"><PencilLine :size="15" aria-hidden="true" />变化 <strong>{{ comparison.result.value.changedFacts.length }}</strong></span>
          <span><CheckCheck :size="15" aria-hidden="true" />未变 <strong>{{ comparison.result.value.unchangedFacts.length }}</strong></span>
        </div>

        <p v-if="!hasDifferences" class="same-state">两个版本的已确认事实一致。</p>
        <div v-else class="comparison-results">
          <section v-if="comparison.result.value.changedFacts.length" class="result-section">
            <h3><PencilLine :size="16" aria-hidden="true" />事实变化</h3>
            <ol>
              <li v-for="change in comparison.result.value.changedFacts" :key="changeKey(change)" class="changed-fact">
                <span class="fact-type">{{ factTypeLabels[change.after.type] }}</span>
                <div class="fact-transition">
                  <div><small>原事实</small><strong>{{ change.before.statement }}</strong><blockquote>{{ change.before.evidence }}</blockquote></div>
                  <ArrowRight :size="16" aria-hidden="true" />
                  <div><small>新事实</small><strong>{{ change.after.statement }}</strong><blockquote>{{ change.after.evidence }}</blockquote></div>
                </div>
              </li>
            </ol>
          </section>

          <section v-if="comparison.result.value.addedFacts.length" class="result-section">
            <h3><Plus :size="16" aria-hidden="true" />新增事实</h3>
            <ul>
              <li v-for="fact in comparison.result.value.addedFacts" :key="factKey(fact)">
                <span class="fact-type">{{ factTypeLabels[fact.type] }}</span>
                <strong>{{ fact.statement }}</strong>
                <blockquote>{{ fact.evidence }}</blockquote>
              </li>
            </ul>
          </section>

          <section v-if="comparison.result.value.removedFacts.length" class="result-section">
            <h3><Minus :size="16" aria-hidden="true" />删除事实</h3>
            <ul>
              <li v-for="fact in comparison.result.value.removedFacts" :key="factKey(fact)">
                <span class="fact-type">{{ factTypeLabels[fact.type] }}</span>
                <strong>{{ fact.statement }}</strong>
                <blockquote>{{ fact.evidence }}</blockquote>
              </li>
            </ul>
          </section>
        </div>
      </template>
    </template>
  </section>
</template>

<style scoped lang="scss">
.comparison { display: grid; gap: var(--space-lg); margin-top: var(--space-2xl); padding-top: var(--space-xl); border-top: var(--rule-thin) solid var(--color-rule-2); }
.comparison-heading { display: flex; align-items: baseline; justify-content: space-between; gap: var(--space-lg); }
.comparison-heading h2, .comparison-heading p { margin: 0; }
.comparison-heading h2 { font-size: var(--text-lg); }
.comparison-heading p, .comparison-heading > span { margin-top: var(--space-2xs); color: var(--color-muted); font-size: var(--text-xs); }
.comparison-controls { display: grid; grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr) auto; align-items: end; gap: var(--space-sm); }
.comparison-controls label { display: grid; gap: var(--space-xs); min-width: 0; }
.comparison-controls label > span { color: var(--color-muted); font-size: var(--text-xs); font-weight: 700; }
.comparison-controls select { width: 100%; min-height: 2.75rem; padding-inline: var(--space-sm); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); color: var(--color-ink); background: var(--color-surface); font-size: var(--text-sm); }
.comparison-controls button { display: inline-flex; align-items: center; justify-content: center; gap: var(--space-xs); min-height: 2.75rem; padding-inline: var(--space-md); border: 0; border-radius: var(--radius-control); color: var(--color-accent-ink); background: var(--color-accent); font-weight: 700; white-space: nowrap; }
.comparison-controls button:disabled { opacity: 0.55; cursor: not-allowed; }
.direction-icon { align-self: center; margin-top: var(--space-lg); color: var(--color-faint); }
.comparison-error, .same-state, .empty-state { margin: 0; padding: var(--space-sm) var(--space-md); font-size: var(--text-sm); }
.comparison-error { border: var(--rule-thin) solid var(--color-error-rule); color: var(--color-error); background: var(--color-error-soft); }
.same-state { border-left: var(--space-2xs) solid var(--color-success-rule); color: var(--color-success); background: var(--color-success-soft); }
.empty-state { padding-inline: 0; color: var(--color-muted); }
.comparison-summary { display: flex; flex-wrap: wrap; gap: var(--space-lg); padding-block: var(--space-sm); border-block: var(--rule-thin) solid var(--color-rule); }
.comparison-summary span { display: inline-flex; align-items: center; gap: var(--space-2xs); color: var(--color-muted); font-size: var(--text-sm); }
.comparison-summary .added { color: var(--color-success); }
.comparison-summary .removed { color: var(--color-error); }
.comparison-summary .changed { color: var(--color-accent-strong); }
.comparison-results { display: grid; gap: var(--space-xl); }
.result-section { display: grid; gap: var(--space-sm); }
.result-section h3 { display: flex; align-items: center; gap: var(--space-xs); margin: 0; font-size: var(--text-md); }
.result-section ol, .result-section ul { display: grid; gap: 0; margin: 0; padding: 0; list-style: none; }
.result-section li { display: grid; grid-template-columns: 4.5rem minmax(0, 1fr); gap: var(--space-sm); padding-block: var(--space-md); border-bottom: var(--rule-thin) solid var(--color-rule); }
.fact-type { width: fit-content; height: fit-content; padding: var(--space-3xs) var(--space-xs); border: var(--rule-thin) solid var(--color-rule-2); color: var(--color-muted); font-size: var(--text-xs); }
.result-section li > strong { align-self: start; font-size: var(--text-sm); }
.result-section li > blockquote { grid-column: 2; }
.result-section blockquote { margin: var(--space-xs) 0 0; color: var(--color-muted); font-size: var(--text-xs); overflow-wrap: anywhere; }
.fact-transition { display: grid; grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr); align-items: center; gap: var(--space-md); min-width: 0; }
.fact-transition div { display: grid; gap: var(--space-2xs); min-width: 0; }
.fact-transition small { color: var(--color-muted); font-size: var(--text-xs); }
.fact-transition strong { font-size: var(--text-sm); overflow-wrap: anywhere; }
.fact-transition > svg { color: var(--color-faint); }
@media (max-width: 48rem) {
  .comparison-controls { grid-template-columns: minmax(0, 1fr); }
  .direction-icon { display: none; }
  .comparison-controls button { width: 100%; }
  .fact-transition { grid-template-columns: minmax(0, 1fr); }
  .fact-transition > svg { transform: rotate(90deg); }
}
</style>
