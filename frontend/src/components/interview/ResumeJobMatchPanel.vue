<script setup lang="ts">
import {
  AlertTriangle,
  CheckCircle2,
  CircleHelp,
  FileSearch,
  Lightbulb,
  RefreshCw,
  ShieldCheck,
} from "@lucide/vue";
import { computed } from "vue";

import type {
  ResumeEvidenceAuditResult,
  ResumeJobMatchStatus,
} from "@/api/interview";
import { useResumeJobMatch } from "@/composables/useResumeJobMatch";

const props = defineProps<{
  targetRole: string;
  jobDescription: string;
  auditResult: ResumeEvidenceAuditResult | null;
}>();

const {
  result,
  loading,
  error,
  canMatch,
  isStale,
  match,
} = useResumeJobMatch(
  () => props.targetRole,
  () => props.jobDescription,
  () => props.auditResult,
);

const statusMeta: Record<ResumeJobMatchStatus, { label: string; tone: string }> = {
  MATCHED: { label: "已有证据", tone: "matched" },
  PARTIAL: { label: "部分覆盖", tone: "partial" },
  MISSING: { label: "尚无证据", tone: "missing" },
  NEEDS_VERIFICATION: { label: "需要核对", tone: "verify" },
};

const assessmentGroups = computed(() => {
  const assessments = result.value?.assessments ?? [];
  return (Object.keys(statusMeta) as ResumeJobMatchStatus[])
    .map((status) => ({
      status,
      ...statusMeta[status],
      items: assessments.filter((item) => item.status === status),
    }))
    .filter((group) => group.items.length > 0);
});

const actionLabel = computed(() => {
  if (loading.value) return "正在分析…";
  return result.value ? "重新分析" : "分析岗位匹配";
});

function iconFor(status: ResumeJobMatchStatus) {
  if (status === "MATCHED") return CheckCircle2;
  if (status === "NEEDS_VERIFICATION") return CircleHelp;
  if (status === "MISSING") return AlertTriangle;
  return Lightbulb;
}

function emitMatch(): void {
  void match();
}
</script>

<template>
  <section class="match-panel" aria-labelledby="job-match-title">
    <header class="match-heading">
      <div class="match-title">
        <span class="match-mark" aria-hidden="true"><ShieldCheck :size="18" /></span>
        <div>
          <h3 id="job-match-title">岗位匹配顾问</h3>
          <p>只比较岗位描述与已确认事实，不把未写内容判断为不会。</p>
        </div>
      </div>
      <button
        type="button"
        class="match-action"
        :disabled="!canMatch || loading"
        @click="emitMatch"
      >
        <RefreshCw v-if="result" :size="16" aria-hidden="true" />
        <FileSearch v-else :size="16" aria-hidden="true" />
        <span>{{ actionLabel }}</span>
      </button>
    </header>

    <p class="match-privacy">
      分析请求只发送岗位描述和审计确认的逐字证据；结果不保存原始简历。
    </p>

    <p v-if="!props.auditResult" class="match-empty">
      <ShieldCheck :size="17" aria-hidden="true" />
      <span>先完成简历证据检查，再分析岗位要求覆盖情况。</span>
    </p>
    <p v-else-if="!props.jobDescription.trim()" class="match-empty">
      <FileSearch :size="17" aria-hidden="true" />
      <span>补充岗位描述后，可以逐条核对岗位要求。</span>
    </p>

    <div v-if="loading" class="match-loading" aria-live="polite" aria-busy="true">
      正在比较岗位要求与已确认证据，通常需要约一分钟。
    </div>

    <p v-if="error" class="match-error" role="alert">
      <AlertTriangle :size="16" aria-hidden="true" />
      <span>{{ error }}</span>
    </p>

    <template v-if="result && !loading">
      <p v-if="isStale" class="match-stale" role="status">
        <RefreshCw :size="15" aria-hidden="true" />
        <span>目标岗位、岗位描述或简历审计结果已变化；请重新分析后再参考。</span>
      </p>
      <p class="match-summary">{{ result.summary }}</p>
      <div class="match-groups" aria-label="岗位要求匹配结果">
        <article v-for="group in assessmentGroups" :key="group.status" class="match-group">
          <header :class="['group-heading', group.tone]">
            <component :is="iconFor(group.status)" :size="16" aria-hidden="true" />
            <h4>{{ group.label }}</h4>
            <span>{{ group.items.length }} 项</span>
          </header>
          <ul>
            <li v-for="item in group.items" :key="`${item.status}-${item.requirementEvidence}`">
              <strong>{{ item.requirement }}</strong>
              <p>{{ item.explanation }}</p>
              <small>岗位原文：{{ item.requirementEvidence }}</small>
              <small v-if="item.resumeEvidence">简历证据：{{ item.resumeEvidence }}</small>
            </li>
          </ul>
        </article>
      </div>
    </template>
  </section>
</template>

<style scoped lang="scss">
.match-panel {
  display: grid;
  gap: var(--space-md);
  min-width: 0;
  padding-top: var(--space-lg);
  border-top: var(--rule-thin) solid var(--color-rule);
}

.match-heading,
.match-title,
.match-action,
.match-error,
.match-stale,
.match-empty,
.group-heading {
  display: flex;
  align-items: center;
}

.match-heading {
  justify-content: space-between;
  gap: var(--space-md);
}

.match-title {
  align-items: flex-start;
  gap: var(--space-sm);
  min-width: 0;
}

.match-mark {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 2.25rem;
  height: 2.25rem;
  border-radius: var(--radius-control);
  color: var(--color-accent);
  background: var(--color-accent-soft);
}

.match-title div {
  display: grid;
  gap: var(--space-2xs);
}

.match-title h3,
.match-title p,
.match-privacy,
.match-summary,
.match-error,
.match-stale,
.match-empty,
.match-loading,
.match-group h4,
.match-group p,
.match-group small {
  margin: 0;
}

.match-title h3 {
  color: var(--color-ink);
  font-size: var(--text-md);
}

.match-title p,
.match-privacy,
.match-group small,
.match-empty,
.match-loading {
  color: var(--color-muted);
  font-size: var(--text-xs);
  line-height: 1.55;
}

.match-action {
  flex: 0 0 auto;
  justify-content: center;
  gap: var(--space-xs);
  min-height: 2.5rem;
  padding-inline: var(--space-md);
  border: var(--rule-thin) solid var(--color-accent);
  border-radius: var(--radius-control);
  color: var(--color-accent);
  background: transparent;
  cursor: pointer;
  font-size: var(--text-sm);
  font-weight: 700;
}

.match-action:disabled {
  border-color: var(--color-rule-2);
  color: var(--color-faint);
  cursor: not-allowed;
}

.match-action:focus-visible {
  outline: var(--rule-focus) solid var(--color-focus);
  outline-offset: var(--rule-thin);
}

.match-empty,
.match-loading,
.match-error,
.match-stale {
  align-items: flex-start;
  gap: var(--space-xs);
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-control);
}

.match-empty,
.match-loading {
  background: var(--color-paper-2);
}

.match-error {
  color: var(--color-error);
  background: var(--color-error-soft);
}

.match-stale {
  color: var(--color-warning);
  background: var(--color-warning-soft);
  font-size: var(--text-sm);
}

.match-summary {
  color: var(--color-ink);
  font-size: var(--text-sm);
  line-height: 1.6;
}

.match-groups {
  display: grid;
  gap: var(--space-md);
}

.match-group {
  border-top: var(--rule-thin) solid var(--color-rule-2);
}

.group-heading {
  gap: var(--space-xs);
  padding-block: var(--space-sm);
}

.group-heading h4 {
  color: var(--color-ink);
  font-size: var(--text-sm);
}

.group-heading span {
  margin-left: auto;
  color: var(--color-muted);
  font-size: var(--text-xs);
}

.group-heading.matched { color: var(--color-success); }
.group-heading.partial { color: var(--color-accent); }
.group-heading.missing { color: var(--color-error); }
.group-heading.verify { color: var(--color-warning); }

.match-group ul {
  display: grid;
  gap: var(--space-sm);
  margin: 0;
  padding: 0 0 var(--space-sm);
  list-style: none;
}

.match-group li {
  display: grid;
  gap: var(--space-2xs);
  padding: var(--space-sm) var(--space-md);
  border-left: 0.2rem solid var(--color-rule-2);
  background: var(--color-paper-2);
}

.match-group li strong {
  color: var(--color-ink);
  font-size: var(--text-sm);
  line-height: 1.45;
}

.match-group li p {
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  line-height: 1.5;
}

@media (max-width: 48rem) {
  .match-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .match-action {
    width: 100%;
  }
}
</style>
