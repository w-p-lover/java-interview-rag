<script setup lang="ts">
import { onMounted, shallowRef } from "vue";
import WorkspaceHeader from "@/components/WorkspaceHeader.vue";
import { getKnowledgeAnswerFeedback, reviewKnowledgeAnswerFeedback, type KnowledgeAnswerFeedback } from "@/api/knowledge";
import { resolveApiError } from "@/api/request";

const items = shallowRef<KnowledgeAnswerFeedback[]>([]);
const loading = shallowRef(false);
const error = shallowRef("");
const selectedStatus = shallowRef("PENDING");
const notes = shallowRef<Record<number, string>>({});

async function load(): Promise<void> {
  loading.value = true;
  error.value = "";
  try { items.value = await getKnowledgeAnswerFeedback(50, selectedStatus.value); }
  catch (requestError) { error.value = resolveApiError(requestError, "读取反馈失败"); }
  finally { loading.value = false; }
}

async function review(item: KnowledgeAnswerFeedback, status: "EVALUATION" | "IGNORED"): Promise<void> {
  try {
    const updated = await reviewKnowledgeAnswerFeedback(item.id, status, notes.value[item.id] || "");
    items.value = items.value.map(current => current.id === updated.id ? updated : current);
  } catch (requestError) { error.value = resolveApiError(requestError, "更新反馈审阅状态失败"); }
}

onMounted(() => void load());
</script>

<template>
  <WorkspaceHeader mode="observability" />
  <main class="feedback-workspace" tabindex="-1">
    <header class="feedback-heading">
      <div><span>RAG 质量闭环</span><h1>回答反馈审阅</h1></div>
      <select v-model="selectedStatus" aria-label="反馈审阅状态" @change="load">
        <option value="PENDING">待审阅</option><option value="EVALUATION">已纳入评估</option><option value="IGNORED">已忽略</option>
      </select>
    </header>
    <p v-if="error" class="error-banner" role="alert">{{ error }}</p>
    <div v-if="loading" class="empty-state">正在读取低敏反馈…</div>
    <div v-else-if="items.length" class="feedback-list">
      <article v-for="item in items" :key="item.id" class="feedback-card">
        <header><strong :class="item.feedback === 'NOT_HELPFUL' ? 'danger' : 'helpful'">{{ item.feedback === "NOT_HELPFUL" ? "没帮助" : "有帮助" }}</strong><span>{{ item.retrievalStrategy }} · {{ item.grounded ? "grounded" : "not grounded" }}</span></header>
        <p>问题标识：<code>{{ item.questionHash }}</code></p>
        <textarea v-model="notes[item.id]" maxlength="500" rows="2" placeholder="记录人工判断或后续评估方向"></textarea>
        <div v-if="item.reviewStatus === 'PENDING'" class="actions"><button type="button" @click="review(item, 'EVALUATION')">纳入评估</button><button type="button" class="secondary" @click="review(item, 'IGNORED')">忽略</button></div>
        <small v-else>已处理：{{ item.reviewStatus === "EVALUATION" ? "纳入评估" : "忽略" }}{{ item.reviewNote ? ` · ${item.reviewNote}` : "" }}</small>
      </article>
    </div>
    <div v-else class="empty-state">当前筛选没有反馈。</div>
  </main>
</template>

<style scoped>
.feedback-workspace { display: grid; gap: var(--space-xl); margin-left: var(--workspace-rail); padding: var(--space-xl) clamp(var(--space-lg), 4vw, var(--space-3xl)); }
.feedback-heading { display: flex; align-items: end; justify-content: space-between; gap: var(--space-lg); padding-bottom: var(--space-lg); border-bottom: var(--rule-thin) solid var(--color-rule-2); }
.feedback-heading div { display: grid; gap: var(--space-2xs); }.feedback-heading span, .feedback-card span, .feedback-card small { color: var(--color-muted); font-size: var(--text-sm); }.feedback-heading h1 { margin: 0; color: var(--color-ink); font-family: var(--font-display); font-size: var(--text-2xl); }
select, textarea { border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); color: var(--color-ink-2); background: var(--color-surface); font: inherit; }
select { min-height: 2.5rem; padding-inline: var(--space-sm); } .feedback-list { display: grid; gap: var(--space-md); max-width: 56rem; }.feedback-card { display: grid; gap: var(--space-sm); padding: var(--space-lg); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-surface); background: var(--color-paper-2); }.feedback-card header { display: flex; align-items: center; justify-content: space-between; gap: var(--space-sm); }.feedback-card p { margin: 0; color: var(--color-muted); font-size: var(--text-sm); }.feedback-card code { font-family: var(--font-mono); }.feedback-card textarea { width: 100%; padding: var(--space-sm); resize: vertical; }.actions { display: flex; gap: var(--space-sm); }.actions button { min-height: 2.5rem; padding-inline: var(--space-md); border: var(--rule-thin) solid var(--color-accent); border-radius: var(--radius-control); color: var(--color-accent-ink); background: var(--color-accent); font: inherit; font-weight: 700; }.actions .secondary { border-color: var(--color-rule-2); color: var(--color-ink-2); background: transparent; }.danger { color: var(--color-error); }.helpful { color: var(--color-success); }.error-banner { padding: var(--space-sm) var(--space-md); color: var(--color-error); background: var(--color-error-soft); }.empty-state { min-height: 18rem; display: grid; place-items: center; color: var(--color-muted); }
@media (max-width: 68rem) { .feedback-workspace { margin-left: 0; } } @media (max-width: 42rem) { .feedback-heading { align-items: start; flex-direction: column; } }
</style>
