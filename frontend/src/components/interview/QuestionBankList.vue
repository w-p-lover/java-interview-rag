<script setup lang="ts">
import { Check, FileEdit, Pencil, Archive, Trash2 } from "@lucide/vue";
import type { BankQuestion, BankQuestionStatus } from "@/api/questionBank";

defineProps<{ questions: BankQuestion[]; selectedIds: string[]; eligibleIds: string[]; currentVersion: string; busy: boolean; loading: boolean }>();
const emit = defineEmits<{
  select: [id: string]; edit: [question: BankQuestion];
  status: [question: BankQuestion, status: Exclude<BankQuestionStatus, "STALE">]; remove: [question: BankQuestion];
}>();
const statusLabels: Record<BankQuestionStatus, string> = { DRAFT: "草稿", ACTIVE: "已审核", ARCHIVED: "已停用", STALE: "待复核" };
const difficultyLabels = { BASIC: "基础", INTERMEDIATE: "进阶", ADVANCED: "高级场景" };
const skillLabels = { JAVA_CORE: "Java 核心", SPRING: "Spring", DATABASE: "数据库", SYSTEM_DESIGN: "系统设计", RAG_AI: "RAG / AI", GENERAL: "综合" };
</script>

<template>
  <div class="bank-list" :aria-busy="loading">
    <div v-if="loading && !questions.length" class="list-skeleton" aria-label="正在读取题库"><div v-for="index in 3" :key="index" class="skeleton-row"></div></div>
    <p v-else-if="!questions.length" class="empty-state">当前条件下没有题目。</p>
    <ol v-else class="question-list">
      <li v-for="question in questions" :key="question.id" class="question-item">
        <div class="question-content">
          <label class="selection"><input type="checkbox" :checked="selectedIds.includes(question.id)" :disabled="busy || !eligibleIds.includes(question.id)" :aria-label="`选择题目：${question.question}`" @change="emit('select', question.id)" /><strong>{{ question.question }}</strong></label>
          <p class="topic-summary">{{ question.topicSummary }}</p>
          <div class="question-meta">
            <span class="status-label" :data-status="question.status">{{ statusLabels[question.status] }}</span>
            <span>{{ skillLabels[question.skill] }}</span><span>{{ difficultyLabels[question.difficulty] }}</span><span>{{ question.category }}</span><span>使用 {{ question.useCount }} 次</span><span>追问 {{ question.followUps.length }} 道</span>
          </div>
          <p class="version-meta">资料版本 {{ question.documentVersion }}<template v-if="question.lastUsedAt"> · 最近使用 {{ question.lastUsedAt.replace('T', ' ').slice(0, 16) }}</template></p>
        </div>
        <div class="question-actions">
          <button type="button" class="icon-action" :disabled="busy" aria-label="编辑题目" title="编辑题目" @click="emit('edit', question)"><Pencil :size="17" aria-hidden="true" /></button>
          <button v-if="question.status !== 'ACTIVE'" type="button" class="icon-action" :disabled="busy || question.documentVersion !== currentVersion" aria-label="审核启用" :title="question.documentVersion !== currentVersion ? '资料版本失效，不能启用' : '审核启用'" @click="emit('status', question, 'ACTIVE')"><Check :size="17" aria-hidden="true" /></button>
          <button v-if="question.status === 'ACTIVE'" type="button" class="icon-action" :disabled="busy" aria-label="撤销审核" title="撤销审核" @click="emit('status', question, 'DRAFT')"><FileEdit :size="17" aria-hidden="true" /></button>
          <button v-if="question.status !== 'ARCHIVED'" type="button" class="icon-action" :disabled="busy" aria-label="停用题目" title="停用题目" @click="emit('status', question, 'ARCHIVED')"><Archive :size="17" aria-hidden="true" /></button>
          <button type="button" class="icon-action delete-action" :disabled="busy" aria-label="删除题目" title="删除题目" @click="emit('remove', question)"><Trash2 :size="17" aria-hidden="true" /></button>
        </div>
      </li>
    </ol>
  </div>
</template>

<style scoped lang="scss">
.question-list { margin: 0; padding: 0; list-style: none; }
.question-item { display: flex; align-items: start; justify-content: space-between; gap: var(--space-lg); padding-block: var(--space-lg); border-bottom: var(--rule-thin) solid var(--color-rule); }
.question-content { min-width: 0; flex: 1; }
.selection { display: flex; align-items: start; gap: var(--space-sm); }
.selection input { width: 1.1rem; height: 1.1rem; flex-shrink: 0; margin: 0.3rem 0 0; accent-color: var(--color-accent); }
.selection strong { color: var(--color-ink); font-size: var(--text-base); overflow-wrap: anywhere; }
.topic-summary { max-width: 75ch; margin: var(--space-xs) 0 var(--space-sm); color: var(--color-ink-2); font-size: var(--text-sm); overflow-wrap: anywhere; }
.question-meta { display: flex; flex-wrap: wrap; gap: var(--space-sm); align-items: center; color: var(--color-muted); font-size: var(--text-xs); }
.status-label { padding: var(--space-3xs) var(--space-xs); border: var(--rule-thin) solid var(--color-rule-2); color: var(--color-ink-2); background: var(--color-paper-2); }
.status-label[data-status="ACTIVE"] { color: var(--color-success); border-color: var(--color-success-rule); background: var(--color-success-soft); }
.status-label[data-status="STALE"] { color: var(--color-warning); border-color: var(--color-warning-rule); background: var(--color-warning-soft); }
.version-meta { margin: var(--space-sm) 0 0; color: var(--color-muted); font-size: var(--text-xs); overflow-wrap: anywhere; }
.question-actions { display: flex; gap: var(--space-3xs); flex-shrink: 0; }
.icon-action { display: inline-grid; place-items: center; width: 2.5rem; height: 2.5rem; padding: 0; border: var(--rule-thin) solid transparent; border-radius: var(--radius-control); color: var(--color-ink-2); background: transparent; }
.icon-action:not(:disabled):hover { color: var(--color-accent); border-color: var(--color-accent); background: var(--color-accent-soft); }
.delete-action:not(:disabled):hover { color: var(--color-error); border-color: var(--color-error-rule); background: var(--color-error-soft); }
.empty-state { padding-block: var(--space-xl); color: var(--color-muted); }
.list-skeleton { display: grid; gap: var(--space-md); padding-block: var(--space-lg); }
.skeleton-row { height: 7rem; background: var(--color-paper-2); }
@media (max-width: 48rem) { .question-item { flex-direction: column; gap: var(--space-sm); } .question-actions { align-self: end; } }
</style>
