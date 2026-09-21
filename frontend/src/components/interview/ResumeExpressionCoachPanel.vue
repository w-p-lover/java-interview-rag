<script setup lang="ts">
import {
  AlertTriangle,
  Check,
  Copy,
  PenLine,
  RefreshCw,
  ShieldCheck,
} from "@lucide/vue";
import { computed, shallowRef } from "vue";

import type { ResumeEvidenceAuditResult } from "@/api/interview";
import { useResumeExpressionCoach } from "@/composables/useResumeExpressionCoach";

const props = defineProps<{
  targetRole: string;
  auditResult: ResumeEvidenceAuditResult | null;
  resumeVersionId: string;
}>();

const {
  result,
  loading,
  error,
  canCoach,
  isStale,
  coach,
} = useResumeExpressionCoach(
  () => props.targetRole,
  () => props.auditResult,
  () => props.resumeVersionId,
);

const copiedCandidateId = shallowRef("");
const candidates = computed(() => result.value?.candidates ?? []);

const actionLabel = computed(() => {
  if (loading.value) return "正在生成…";
  return result.value ? "重新生成候选" : "生成表达候选";
});

async function copyCandidate(id: string, text: string): Promise<void> {
  try {
    await navigator.clipboard.writeText(text);
    copiedCandidateId.value = id;
    window.setTimeout(() => {
      if (copiedCandidateId.value === id) {
        copiedCandidateId.value = "";
      }
    }, 1800);
  } catch {
    copiedCandidateId.value = "";
  }
}

function emitCoach(): void {
  void coach();
}
</script>

<template>
  <section class="expression-panel" aria-labelledby="resume-expression-title">
    <header class="expression-heading">
      <div class="expression-title">
        <span class="expression-mark" aria-hidden="true"><PenLine :size="18" /></span>
        <div>
          <h3 id="resume-expression-title">简历表达教练</h3>
          <p>只改写已确认事实，候选结果不会覆盖原简历。</p>
        </div>
      </div>
      <button
        type="button"
        class="expression-action"
        :disabled="!canCoach || loading"
        @click="emitCoach"
      >
        <RefreshCw v-if="result" :size="16" aria-hidden="true" />
        <PenLine v-else :size="16" aria-hidden="true" />
        <span>{{ actionLabel }}</span>
      </button>
    </header>

    <p class="expression-privacy">
      请求只发送目标岗位和审计确认的逐字证据；请在复制或投递前人工核对语义。
    </p>

    <p v-if="!props.auditResult" class="expression-empty">
      <ShieldCheck :size="17" aria-hidden="true" />
      <span>先完成简历证据检查，再生成表达候选。</span>
    </p>

    <div v-if="loading" class="expression-loading" aria-live="polite" aria-busy="true">
      正在根据已确认事实整理表达候选，通常需要约一分钟。
    </div>

    <p v-if="error" class="expression-error" role="alert">
      <AlertTriangle :size="16" aria-hidden="true" />
      <span>{{ error }}</span>
    </p>

    <template v-if="result && !loading">
      <p v-if="isStale" class="expression-stale" role="status">
        <RefreshCw :size="15" aria-hidden="true" />
        <span>这是该版本最近保存的结果，当前输入可能不同；请重新生成后更新。</span>
      </p>
      <div class="expression-candidates" aria-label="简历表达候选">
        <article v-for="(candidate, index) in candidates" :key="candidate.id" class="expression-candidate">
          <header class="candidate-heading">
            <h4>候选 {{ index + 1 }}</h4>
            <button
              type="button"
              class="copy-action"
              :aria-label="copiedCandidateId === candidate.id ? '已复制候选表达' : '复制候选表达'"
              :title="copiedCandidateId === candidate.id ? '已复制' : '复制候选表达'"
              @click="copyCandidate(candidate.id, candidate.text)"
            >
              <Check v-if="copiedCandidateId === candidate.id" :size="15" aria-hidden="true" />
              <Copy v-else :size="15" aria-hidden="true" />
              <span>{{ copiedCandidateId === candidate.id ? "已复制" : "复制" }}</span>
            </button>
          </header>
          <p class="candidate-text">{{ candidate.text }}</p>
          <p class="candidate-rationale">{{ candidate.rationale }}</p>
          <div class="candidate-evidence">
            <strong>依据事实</strong>
            <ul>
              <li v-for="(fact, factIndex) in candidate.supportingFacts" :key="`${candidate.id}-${factIndex}`">
                <span>[事实 {{ candidate.supportingFactNumbers[factIndex] }}]</span>
                {{ fact.evidence }}
              </li>
            </ul>
          </div>
          <div v-if="candidate.reviewPoints.length" class="candidate-review">
            <strong>人工核对</strong>
            <ul>
              <li v-for="point in candidate.reviewPoints" :key="point">{{ point }}</li>
            </ul>
          </div>
        </article>
      </div>
    </template>
  </section>
</template>

<style scoped lang="scss">
.expression-panel {
  display: grid;
  gap: var(--space-md);
  min-width: 0;
  padding-top: var(--space-lg);
  border-top: var(--rule-thin) solid var(--color-rule);
}

.expression-heading,
.expression-title,
.expression-action,
.candidate-heading,
.expression-error,
.expression-stale,
.expression-empty {
  display: flex;
  align-items: center;
}

.expression-heading {
  justify-content: space-between;
  gap: var(--space-md);
}

.expression-title {
  align-items: flex-start;
  gap: var(--space-sm);
  min-width: 0;
}

.expression-mark {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 2.25rem;
  height: 2.25rem;
  border-radius: var(--radius-control);
  color: var(--color-accent);
  background: var(--color-accent-soft);
}

.expression-title div {
  display: grid;
  gap: var(--space-2xs);
}

.expression-title h3,
.expression-title p,
.expression-privacy,
.expression-error,
.expression-stale,
.expression-empty,
.expression-loading,
.candidate-heading h4,
.candidate-text,
.candidate-rationale,
.candidate-evidence strong,
.candidate-review strong {
  margin: 0;
}

.expression-title h3 {
  color: var(--color-ink);
  font-size: var(--text-md);
}

.expression-title p,
.expression-privacy,
.expression-empty,
.expression-loading {
  color: var(--color-muted);
  font-size: var(--text-xs);
  line-height: 1.55;
}

.expression-action,
.copy-action {
  justify-content: center;
  gap: var(--space-xs);
  border: var(--rule-thin) solid var(--color-accent);
  border-radius: var(--radius-control);
  color: var(--color-accent);
  background: transparent;
  cursor: pointer;
  font-weight: 700;
}

.expression-action {
  flex: 0 0 auto;
  min-height: 2.5rem;
  padding-inline: var(--space-md);
  font-size: var(--text-sm);
}

.copy-action {
  display: inline-flex;
  min-height: 2rem;
  padding: 0 var(--space-sm);
  font-size: var(--text-xs);
}

.expression-action:disabled {
  border-color: var(--color-rule-2);
  color: var(--color-faint);
  cursor: not-allowed;
}

.expression-action:focus-visible,
.copy-action:focus-visible {
  outline: var(--rule-focus) solid var(--color-focus);
  outline-offset: var(--rule-thin);
}

.expression-empty,
.expression-loading,
.expression-error,
.expression-stale {
  align-items: flex-start;
  gap: var(--space-xs);
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-control);
}

.expression-empty,
.expression-loading {
  background: var(--color-paper-2);
}

.expression-error {
  color: var(--color-error);
  background: var(--color-error-soft);
}

.expression-stale {
  color: var(--color-warning);
  background: var(--color-warning-soft);
  font-size: var(--text-sm);
}

.expression-candidates {
  display: grid;
  gap: var(--space-md);
}

.expression-candidate {
  display: grid;
  gap: var(--space-sm);
  padding: var(--space-md);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  background: var(--color-paper-2);
}

.candidate-heading {
  justify-content: space-between;
  gap: var(--space-sm);
}

.candidate-heading h4 {
  color: var(--color-ink);
  font-size: var(--text-sm);
}

.candidate-text {
  color: var(--color-ink);
  font-size: var(--text-md);
  line-height: 1.6;
}

.candidate-rationale,
.candidate-evidence,
.candidate-review {
  color: var(--color-ink-2);
  font-size: var(--text-xs);
  line-height: 1.55;
}

.candidate-evidence ul,
.candidate-review ul {
  display: grid;
  gap: var(--space-2xs);
  margin: var(--space-2xs) 0 0;
  padding-left: 1.15rem;
}

.candidate-evidence li span {
  color: var(--color-accent-strong);
  font-weight: 700;
}

.candidate-review {
  color: var(--color-warning-strong, var(--color-warning));
}

@media (max-width: 48rem) {
  .expression-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .expression-action {
    width: 100%;
  }
}
</style>
