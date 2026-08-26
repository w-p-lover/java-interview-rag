<script setup lang="ts">
import {
  AlertTriangle,
  ChevronDown,
  FileCheck2,
  FileWarning,
  Lightbulb,
  Quote,
  RefreshCw,
  ScanSearch,
  ShieldCheck,
} from "@lucide/vue";
import { computed } from "vue";

import type {
  ResumeEvidenceAuditResult,
  ResumeEvidenceConfirmedFact,
  ResumeEvidenceFactType,
} from "@/api/interview";

const props = defineProps<{
  result: ResumeEvidenceAuditResult | null;
  loading: boolean;
  error: string;
  canAudit: boolean;
  stale: boolean;
}>();

const emit = defineEmits<{
  audit: [];
}>();

interface FactGroup {
  type: ResumeEvidenceFactType;
  label: string;
  facts: ResumeEvidenceConfirmedFact[];
}

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
const factGroups = computed<FactGroup[]>(() => factTypeOrder
  .map((type) => ({
    type,
    label: factTypeLabels[type],
    facts: props.result?.confirmedFacts.filter((fact) => fact.type === type) ?? [],
  }))
  .filter((group) => group.facts.length > 0));
const checkedFactCount = computed(() => props.result?.confirmedFacts.length ?? 0);
const issueCount = computed(() => (props.result?.missingInformation.length ?? 0)
  + (props.result?.risks.length ?? 0));
const actionLabel = computed(() => {
  if (props.loading) {
    return "正在检查…";
  }
  return props.result ? "重新检查" : "检查简历证据";
});
</script>

<template>
  <section class="audit-panel" aria-labelledby="resume-audit-title">
    <header class="audit-heading">
      <div class="audit-title">
        <span class="audit-mark" aria-hidden="true">
          <ShieldCheck :size="18" />
        </span>
        <div>
          <h3 id="resume-audit-title">简历证据检查</h3>
          <p>区分已确认事实、待补信息和需要人工核对的风险。</p>
        </div>
      </div>

      <button
        type="button"
        class="audit-action"
        :disabled="!canAudit || loading"
        @click="emit('audit')"
      >
        <RefreshCw v-if="result" :size="16" aria-hidden="true" />
        <ScanSearch v-else :size="16" aria-hidden="true" />
        <span>{{ actionLabel }}</span>
      </button>
    </header>

    <p class="audit-privacy">
      检查结果仅保留在当前页面；未出现在事实列表中，不代表候选人不具备该能力。
    </p>

    <div v-if="loading" class="audit-loading" aria-live="polite" aria-busy="true">
      <span>正在逐项核对原文证据，通常需要约一分钟。</span>
      <div class="skeleton-line skeleton-line-wide"></div>
      <div class="skeleton-line"></div>
      <div class="skeleton-line skeleton-line-short"></div>
    </div>

    <p v-if="error" class="audit-error" role="alert">
      <AlertTriangle :size="16" aria-hidden="true" />
      <span>{{ error }}</span>
    </p>

    <template v-if="result && !loading">
      <p v-if="stale" class="audit-stale" role="status">
        <RefreshCw :size="15" aria-hidden="true" />
        <span>简历内容已修改，下面是上一次结果；请重新检查后再参考。</span>
      </p>

      <div class="audit-summary" aria-live="polite">
        <span>
          <FileCheck2 :size="16" aria-hidden="true" />
          已确认 <strong>{{ checkedFactCount }}</strong> 项事实
        </span>
        <span>
          <FileWarning :size="16" aria-hidden="true" />
          <strong>{{ issueCount }}</strong> 项待补或待核对
        </span>
      </div>

      <div class="fact-groups" aria-label="已确认事实">
        <details
          v-for="(group, index) in factGroups"
          :key="group.type"
          class="fact-group"
          :open="index === 0"
        >
          <summary>
            <span>{{ group.label }}</span>
            <small>{{ group.facts.length }} 项</small>
            <ChevronDown :size="15" aria-hidden="true" />
          </summary>
          <ul>
            <li v-for="fact in group.facts" :key="`${fact.type}-${fact.statement}-${fact.evidence}`">
              <strong>{{ fact.statement }}</strong>
              <span>
                <Quote :size="13" aria-hidden="true" />
                {{ fact.evidence }}
              </span>
            </li>
          </ul>
        </details>
      </div>

      <div class="audit-guidance">
        <section aria-labelledby="missing-title">
          <h4 id="missing-title">
            <FileWarning :size="16" aria-hidden="true" />
            待补信息
          </h4>
          <ul v-if="result.missingInformation.length">
            <li v-for="item in result.missingInformation" :key="item">{{ item }}</li>
          </ul>
          <p v-else>当前没有识别到明确缺失项。</p>
        </section>

        <section aria-labelledby="risk-title">
          <h4 id="risk-title">
            <AlertTriangle :size="16" aria-hidden="true" />
            人工核对
          </h4>
          <ul v-if="result.risks.length">
            <li v-for="item in result.risks" :key="item">{{ item }}</li>
          </ul>
          <p v-else>当前没有识别到需要人工核对的风险。</p>
        </section>

        <section aria-labelledby="suggestion-title">
          <h4 id="suggestion-title">
            <Lightbulb :size="16" aria-hidden="true" />
            修改建议
          </h4>
          <ul v-if="result.suggestions.length">
            <li v-for="item in result.suggestions" :key="item">{{ item }}</li>
          </ul>
          <p v-else>当前没有额外修改建议。</p>
        </section>
      </div>
    </template>

    <div v-else-if="!loading" class="audit-empty">
      <FileCheck2 :size="18" aria-hidden="true" />
      <span>上传或粘贴简历后，可先检查事实是否都有原文依据。</span>
    </div>
  </section>
</template>

<style scoped lang="scss">
.audit-panel {
  display: grid;
  gap: var(--space-md);
  min-width: 0;
  padding-top: var(--space-lg);
  border-top: var(--rule-thin) solid var(--color-rule);
}

.audit-heading,
.audit-title,
.audit-action,
.audit-summary span,
.audit-error,
.audit-stale,
.audit-empty,
.fact-group summary,
.fact-group li span,
.audit-guidance h4 {
  display: flex;
  align-items: center;
}

.audit-heading {
  justify-content: space-between;
  gap: var(--space-md);
}

.audit-title {
  min-width: 0;
  align-items: flex-start;
  gap: var(--space-sm);
}

.audit-mark {
  display: grid;
  flex: 0 0 auto;
  place-items: center;
  width: 2.25rem;
  height: 2.25rem;
  border-radius: var(--radius-control);
  color: var(--color-success);
  background: var(--color-success-soft);
}

.audit-title div {
  display: grid;
  gap: var(--space-2xs);
}

.audit-title h3,
.audit-title p,
.audit-privacy,
.audit-error,
.audit-stale,
.audit-guidance h4,
.audit-guidance p,
.audit-empty {
  margin: 0;
}

.audit-title h3 {
  color: var(--color-ink);
  font-size: var(--text-md);
}

.audit-title p,
.audit-privacy,
.audit-guidance p,
.audit-empty {
  color: var(--color-muted);
  font-size: var(--text-xs);
  line-height: 1.55;
}

.audit-privacy {
  max-width: 72ch;
}

.audit-action {
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
  transition: color var(--dur-short) var(--ease-out), background-color var(--dur-short) var(--ease-out);
}

.audit-action:disabled {
  border-color: var(--color-rule-2);
  color: var(--color-faint);
  cursor: not-allowed;
}

.audit-action:focus-visible {
  outline: var(--rule-focus) solid var(--color-focus);
  outline-offset: var(--rule-thin);
}

.audit-loading {
  display: grid;
  gap: var(--space-xs);
  padding: var(--space-md);
  border-radius: var(--radius-control);
  color: var(--color-muted);
  background: var(--color-paper-2);
  font-size: var(--text-sm);
}

.skeleton-line {
  width: 72%;
  height: 0.55rem;
  border-radius: var(--radius-control);
  background: var(--color-paper-3);
  animation: audit-pulse 1.2s ease-in-out infinite alternate;
}

.skeleton-line-wide {
  width: 92%;
}

.skeleton-line-short {
  width: 48%;
}

.audit-error,
.audit-stale,
.audit-empty {
  align-items: flex-start;
  gap: var(--space-xs);
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-control);
}

.audit-error {
  color: var(--color-error);
  background: var(--color-error-soft);
}

.audit-stale {
  color: var(--color-warning);
  background: var(--color-warning-soft);
  font-size: var(--text-sm);
}

.audit-summary {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm) var(--space-lg);
  padding-block: var(--space-xs);
}

.audit-summary span {
  gap: var(--space-xs);
  color: var(--color-muted);
  font-size: var(--text-sm);
}

.audit-summary strong {
  color: var(--color-ink);
}

.fact-groups {
  border-block: var(--rule-thin) solid var(--color-rule);
}

.fact-group + .fact-group {
  border-top: var(--rule-thin) solid var(--color-rule);
}

.fact-group summary {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  gap: var(--space-sm);
  min-height: 2.75rem;
  color: var(--color-ink);
  cursor: pointer;
  font-size: var(--text-sm);
  font-weight: 700;
  list-style: none;
}

.fact-group summary::-webkit-details-marker {
  display: none;
}

.fact-group summary small {
  color: var(--color-muted);
  font-weight: 500;
}

.fact-group summary svg {
  color: var(--color-muted);
  transition: transform var(--dur-short) var(--ease-out);
}

.fact-group[open] summary svg {
  transform: rotate(180deg);
}

.fact-group ul,
.audit-guidance ul {
  margin: 0;
  padding: 0;
  list-style: none;
}

.fact-group ul {
  padding-bottom: var(--space-sm);
}

.fact-group li {
  display: grid;
  grid-template-columns: minmax(10rem, 0.75fr) minmax(0, 1.25fr);
  gap: var(--space-md);
  padding-block: var(--space-sm);
  border-top: var(--rule-thin) solid var(--color-rule);
}

.fact-group li strong,
.fact-group li span {
  min-width: 0;
  overflow-wrap: anywhere;
  font-size: var(--text-sm);
  line-height: 1.55;
}

.fact-group li strong {
  color: var(--color-ink);
}

.fact-group li span {
  align-items: flex-start;
  gap: var(--space-xs);
  color: var(--color-muted);
}

.fact-group li span svg {
  flex: 0 0 auto;
  margin-top: 0.25em;
  color: var(--color-accent);
}

.audit-guidance {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-lg);
}

.audit-guidance section {
  min-width: 0;
  padding-top: var(--space-sm);
  border-top: var(--rule-thin) solid var(--color-rule-2);
}

.audit-guidance h4 {
  gap: var(--space-xs);
  color: var(--color-ink);
  font-size: var(--text-sm);
}

.audit-guidance li {
  position: relative;
  margin-top: var(--space-xs);
  padding-left: var(--space-md);
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  line-height: 1.55;
}

.audit-guidance li::before {
  position: absolute;
  top: 0.65em;
  left: 0;
  width: 0.3rem;
  height: 0.3rem;
  border-radius: var(--radius-round);
  background: var(--color-accent);
  content: "";
}

.audit-guidance p {
  margin-top: var(--space-xs);
}

.audit-empty {
  background: var(--color-paper-2);
}

@keyframes audit-pulse {
  from {
    opacity: 0.48;
  }
  to {
    opacity: 1;
  }
}

@media (hover: hover) and (pointer: fine) {
  .audit-action:not(:disabled):hover {
    color: var(--color-accent-ink);
    background: var(--color-accent);
  }
}

@media (max-width: 48rem) {
  .audit-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .audit-action {
    width: 100%;
  }

  .audit-guidance,
  .fact-group li {
    grid-template-columns: minmax(0, 1fr);
  }

  .fact-group li {
    gap: var(--space-xs);
  }
}

@media (prefers-reduced-motion: reduce) {
  .skeleton-line {
    animation: none;
  }

  .audit-action,
  .fact-group summary svg {
    transition: none;
  }
}
</style>
