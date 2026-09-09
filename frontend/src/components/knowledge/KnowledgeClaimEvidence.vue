<script setup lang="ts">
import { computed } from "vue";

import type {
  KnowledgeAnswerClaimEvidence,
  KnowledgeAnswerClaimStatus,
} from "@/api/knowledge";

const props = defineProps<{
  claims: readonly KnowledgeAnswerClaimEvidence[];
  reviewTaskIds?: Readonly<Record<string, string>>;
}>();

const emit = defineEmits<{ addReview: [claim: KnowledgeAnswerClaimEvidence] }>();

const statusMeta: Record<KnowledgeAnswerClaimStatus, { label: string; tone: string }> = {
  SUPPORTED: { label: "资料支持", tone: "supported" },
  EVIDENCE_INSUFFICIENT: { label: "证据不足", tone: "insufficient" },
  EVIDENCE_CONFLICTING: { label: "证据冲突", tone: "conflicting" },
  CITATION_INVALID: { label: "引用无效", tone: "invalid" },
};

const supportedCount = computed(() => props.claims.filter((claim) => claim.status === "SUPPORTED").length);
const issueCount = computed(() => props.claims.length - supportedCount.value);

function statusLabel(status: KnowledgeAnswerClaimStatus): string {
  return statusMeta[status].label;
}

function statusTone(status: KnowledgeAnswerClaimStatus): string {
  return statusMeta[status].tone;
}

function coverageLabel(coverage: number): string {
  return `${Math.round(coverage * 100)}% 锚点覆盖`;
}

function sourceTarget(sourceNumber: number): string {
  return `#knowledge-source-${sourceNumber}`;
}
</script>

<template>
  <details v-if="props.claims.length" class="claim-evidence">
    <summary>
      <span>证据核对</span>
      <small>{{ supportedCount }}/{{ props.claims.length }} 条有资料支持<span v-if="issueCount">，{{ issueCount }} 条需留意</span></small>
    </summary>
    <ul>
      <li v-for="(claim, index) in props.claims" :key="`${claim.status}-${index}-${claim.claim}`">
        <div class="claim-heading">
          <strong :class="statusTone(claim.status)">{{ statusLabel(claim.status) }}</strong>
          <span>{{ coverageLabel(claim.anchorCoverage) }}</span>
        </div>
        <p>{{ claim.claim }}</p>
        <button
          v-if="claim.status !== 'SUPPORTED'"
          type="button"
          :disabled="Boolean(props.reviewTaskIds?.[claim.claim])"
          @click="emit('addReview', claim)"
        >{{ props.reviewTaskIds?.[claim.claim] ? "已加入复习" : "加入复习" }}</button>
        <button
          v-if="claim.status !== 'SUPPORTED'"
          type="button"
          :disabled="Boolean(props.reviewTaskIds?.[claim.claim])"
          @click="emit('addReview', claim)"
        >{{ props.reviewTaskIds?.[claim.claim] ? "已加入复习" : "加入复习" }}</button>
        <small v-if="claim.sourceNumbers.length" class="claim-sources">
          <span>引用资料：</span>
          <a
            v-for="sourceNumber in claim.sourceNumbers"
            :key="sourceNumber"
            :href="sourceTarget(sourceNumber)"
          >[资料 {{ sourceNumber }}]</a>
        </small>
      </li>
    </ul>
  </details>
</template>

<style scoped lang="scss">
.claim-evidence {
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  background: var(--color-paper-2);
}

.claim-evidence summary {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-md);
  padding: var(--space-sm) var(--space-md);
  color: var(--color-ink);
  cursor: pointer;
  font-size: var(--text-sm);
  font-weight: 700;
}

.claim-evidence summary small,
.claim-evidence li small {
  color: var(--color-muted);
  font-size: var(--text-xs);
  font-weight: 400;
}

.claim-sources {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2xs);
}

.claim-sources a {
  color: var(--color-accent-strong);
  font-weight: 700;
  text-decoration: underline;
  text-underline-offset: 0.18em;
}

.claim-sources a:focus-visible {
  border-radius: var(--radius-control);
  outline: var(--rule-focus) solid var(--color-focus);
  outline-offset: var(--rule-thin);
}

.claim-evidence ul {
  display: grid;
  gap: var(--space-sm);
  margin: 0;
  padding: 0 var(--space-md) var(--space-md);
  list-style: none;
}

.claim-evidence li {
  display: grid;
  gap: var(--space-2xs);
  padding-top: var(--space-sm);
  border-top: var(--rule-thin) solid var(--color-rule-2);
}

.claim-heading {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-sm);
}

.claim-heading strong {
  font-size: var(--text-xs);
}

.claim-heading span {
  color: var(--color-muted);
  font-size: var(--text-xs);
}

.claim-evidence p {
  margin: 0;
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  line-height: 1.55;
}

.claim-evidence button {
  justify-self: start;
  min-height: 2rem;
  padding-inline: var(--space-sm);
  border: var(--rule-thin) solid var(--color-accent);
  border-radius: var(--radius-control);
  color: var(--color-accent-strong);
  background: var(--color-surface);
  font-size: var(--text-xs);
  font-weight: 700;
}

.supported { color: var(--color-success); }
.insufficient { color: var(--color-warning); }
.conflicting,
.invalid { color: var(--color-error); }

@media (max-width: 40rem) {
  .claim-evidence summary,
  .claim-heading {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
