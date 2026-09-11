<script setup lang="ts">
import {
  AlertTriangle,
  ArrowRight,
  BookOpenCheck,
  CalendarClock,
  CheckCircle2,
  Clock3,
  ListChecks,
  MessageCircleQuestion,
  RefreshCw,
} from "@lucide/vue";
import { computed, onMounted, shallowRef } from "vue";
import { useRouter } from "vue-router";

import WorkspaceHeader from "@/components/WorkspaceHeader.vue";
import {
  completeInterviewReviewTask,
  scheduleInterviewReviewTask,
} from "@/api/interview";
import {
  completeKnowledgeReviewTask,
  scheduleKnowledgeReviewTask,
} from "@/api/knowledge";
import { resolveApiError } from "@/api/request";
import {
  useUnifiedReviewCenter,
  type UnifiedReviewItem,
  type UnifiedReviewKind,
  type UnifiedReviewTiming,
} from "@/composables/useUnifiedReviewCenter";

type ReviewFilter = "ALL" | "DUE" | "UPCOMING" | UnifiedReviewKind;

const router = useRouter();
const activeFilter = shallowRef<ReviewFilter>("ALL");
const {
  items,
  summary,
  interviewError,
  knowledgeError,
  loading,
  sessionCount,
  load,
} = useUnifiedReviewCenter();

const filters = computed(() => [
  { id: "ALL" as const, label: "全部", count: summary.value.total },
  { id: "DUE" as const, label: "今日与逾期", count: summary.value.due },
  { id: "UPCOMING" as const, label: "后续计划", count: items.value.filter((item) => item.timing === "UPCOMING").length },
  { id: "INTERVIEW" as const, label: "面试复习", count: summary.value.interview },
  { id: "KNOWLEDGE" as const, label: "知识复习", count: summary.value.knowledge },
]);

const visibleItems = computed(() => items.value.filter((item) => {
  if (activeFilter.value === "ALL") {
    return true;
  }
  if (activeFilter.value === "DUE") {
    return item.timing !== "UPCOMING" && item.status === "PENDING";
  }
  if (activeFilter.value === "UPCOMING") {
    return item.timing === "UPCOMING";
  }
  return item.kind === activeFilter.value;
}));

const hasPartialFailure = computed(() => Boolean(interviewError.value || knowledgeError.value));
const actionLoadingKey = shallowRef("");
const actionError = shallowRef("");

function formatDate(date: string): string {
  return date.replaceAll("-", ".");
}

function timingLabel(timing: UnifiedReviewTiming, status: UnifiedReviewItem["status"]): string {
  if (status === "COMPLETED") {
    return "已完成";
  }
  if (timing === "OVERDUE") {
    return "已逾期";
  }
  return timing === "TODAY" ? "今天复习" : "后续计划";
}

function scoreLabel(item: UnifiedReviewItem): string {
  return item.latestOverallScore == null ? "尚未练习" : `最近 ${item.latestOverallScore} 分`;
}

function kindLabel(kind: UnifiedReviewKind): string {
  return kind === "INTERVIEW" ? "面试复习" : "知识复习";
}

async function openItem(item: UnifiedReviewItem): Promise<void> {
  if (item.kind === "INTERVIEW" && item.sessionId) {
    await router.push({ name: "interview", query: { sessionId: item.sessionId, taskId: item.taskId } });
    return;
  }
  await router.push({ name: "knowledge-answer", query: { reviewTaskId: item.taskId } });
}

async function completeItem(item: UnifiedReviewItem): Promise<void> {
  actionLoadingKey.value = item.key;
  actionError.value = "";
  try {
    if (item.kind === "INTERVIEW" && item.sessionId) {
      await completeInterviewReviewTask(item.sessionId, item.taskId);
    } else {
      await completeKnowledgeReviewTask(item.taskId);
    }
    await load();
  } catch (error) {
    actionError.value = resolveApiError(error, "完成复习任务失败");
  } finally {
    actionLoadingKey.value = "";
  }
}

async function scheduleItem(item: UnifiedReviewItem, event: Event): Promise<void> {
  const plannedReviewDate = (event.target as HTMLInputElement).value || null;
  actionLoadingKey.value = item.key;
  actionError.value = "";
  try {
    if (item.kind === "INTERVIEW" && item.sessionId) {
      await scheduleInterviewReviewTask(item.sessionId, item.taskId, plannedReviewDate);
    } else {
      await scheduleKnowledgeReviewTask(item.taskId, plannedReviewDate);
    }
    await load();
  } catch (error) {
    actionError.value = resolveApiError(error, "调整复习计划失败");
  } finally {
    actionLoadingKey.value = "";
  }
}

function refresh(): void {
  void load();
}

onMounted(load);
</script>

<template>
  <WorkspaceHeader mode="review" />

  <main id="main-content" class="review-center" tabindex="-1">
    <header class="review-heading">
      <div>
        <span class="review-kicker">Review center</span>
        <h1>统一复习中心</h1>
        <p>合并最近面试和知识问答中的待复习内容，按日期直接进入下一次训练。</p>
      </div>
      <button type="button" class="refresh-action" :disabled="loading" @click="refresh">
        <RefreshCw :size="17" aria-hidden="true" />
        <span>{{ loading ? "正在刷新…" : "刷新路径" }}</span>
      </button>
    </header>

    <section id="review-overview" class="review-overview" aria-label="复习概览" tabindex="-1">
      <article>
        <ListChecks :size="19" aria-hidden="true" />
        <span>全部推荐</span>
        <strong>{{ summary.total }}</strong>
      </article>
      <article :class="{ urgent: summary.due > 0 }">
        <CalendarClock :size="19" aria-hidden="true" />
        <span>今日与逾期</span>
        <strong>{{ summary.due }}</strong>
      </article>
      <article :class="{ urgent: summary.overdue > 0 }">
        <Clock3 :size="19" aria-hidden="true" />
        <span>已经逾期</span>
        <strong>{{ summary.overdue }}</strong>
      </article>
      <article>
        <CheckCircle2 :size="19" aria-hidden="true" />
        <span>已有练习记录</span>
        <strong>{{ summary.practiced }}</strong>
      </article>
    </section>

    <section id="review-filters" class="review-controls" aria-labelledby="review-filter-title" tabindex="-1">
      <div>
        <h2 id="review-filter-title">复习队列</h2>
        <p>面试路径读取浏览器最近 {{ sessionCount }} 场会话；知识路径读取当前知识复习任务。</p>
      </div>
      <div class="review-filters" role="group" aria-label="筛选复习任务">
        <button
          v-for="filter in filters"
          :key="filter.id"
          type="button"
          :class="{ active: activeFilter === filter.id }"
          :aria-pressed="activeFilter === filter.id"
          @click="activeFilter = filter.id"
        >
          {{ filter.label }}
          <span>{{ filter.count }}</span>
        </button>
      </div>
    </section>

    <div v-if="hasPartialFailure" class="partial-errors" aria-label="部分路径加载状态">
      <p v-if="interviewError" role="alert">
        <AlertTriangle :size="16" aria-hidden="true" />
        <span>{{ interviewError }}；知识复习仍可继续使用。</span>
      </p>
      <p v-if="knowledgeError" role="alert">
        <AlertTriangle :size="16" aria-hidden="true" />
        <span>{{ knowledgeError }}；面试复习仍可继续使用。</span>
      </p>
    </div>

    <p v-if="actionError" class="action-error" role="alert">{{ actionError }}</p>

    <section id="review-list" class="review-list" aria-live="polite" tabindex="-1">
      <div v-if="loading && !items.length" class="review-state" aria-busy="true">
        正在整理复习路径…
      </div>

      <div v-else-if="!visibleItems.length" class="review-empty">
        <ListChecks :size="28" aria-hidden="true" />
        <div>
          <h2>{{ summary.total ? "当前筛选没有任务" : "还没有可复习内容" }}</h2>
          <p v-if="!summary.total && !sessionCount">
            完成模拟面试或在知识问答中确认创建复习任务后，这里会自动汇总下一次练习。
          </p>
          <p v-else>可以切换筛选条件，或刷新获取最新学习路径。</p>
        </div>
      </div>

      <ol v-else>
        <li v-for="item in visibleItems" :key="item.key">
          <div class="review-kind" :class="item.kind.toLowerCase()">
            <BookOpenCheck v-if="item.kind === 'INTERVIEW'" :size="18" aria-hidden="true" />
            <MessageCircleQuestion v-else :size="18" aria-hidden="true" />
          </div>
          <div class="review-copy">
            <div class="review-item-heading">
              <div>
                <span>{{ kindLabel(item.kind) }}</span>
                <strong>{{ item.title }}</strong>
              </div>
              <span class="timing-badge" :class="[item.timing.toLowerCase(), { completed: item.status === 'COMPLETED' }]">
                {{ timingLabel(item.timing, item.status) }}
              </span>
            </div>
            <p>{{ item.context }}</p>
            <small>{{ item.reason }}</small>
            <div class="review-meta">
              <span>{{ formatDate(item.recommendedReviewDate) }}</span>
              <span>间隔 {{ item.intervalDays }} 天</span>
              <span>{{ scoreLabel(item) }}</span>
              <span v-if="item.bestOverallScore != null">最佳 {{ item.bestOverallScore }} 分</span>
            </div>
          </div>
          <div class="review-actions">
            <label v-if="item.status === 'PENDING'" class="schedule-action">
              <span class="sr-only">计划复习日期</span>
              <input type="date" :value="item.plannedReviewDate ?? ''" :disabled="actionLoadingKey === item.key" @change="scheduleItem(item, $event)" />
            </label>
            <button v-if="item.status === 'PENDING'" type="button" class="complete-action" :disabled="actionLoadingKey === item.key" @click="completeItem(item)">
              {{ actionLoadingKey === item.key ? "处理中…" : "标记完成" }}
            </button>
            <button type="button" class="open-action" @click="openItem(item)">
              <span>进入训练</span>
              <ArrowRight :size="16" aria-hidden="true" />
            </button>
          </div>
        </li>
      </ol>
    </section>
  </main>
</template>

<style scoped lang="scss">
.review-center {
  display: grid;
  gap: var(--space-xl);
  margin-left: var(--workspace-rail);
  padding: var(--space-xl) clamp(var(--space-lg), 4vw, var(--space-3xl)) var(--space-2xl);
}

.review-heading,
.review-controls,
.review-item-heading,
.review-meta,
.refresh-action,
.partial-errors p,
.review-empty,
.review-actions {
  display: flex;
  align-items: center;
}

.review-heading {
  justify-content: space-between;
  gap: var(--space-xl);
  padding-bottom: var(--space-lg);
  border-bottom: var(--rule-thin) solid var(--color-rule-2);
}

.review-heading > div,
.review-controls > div:first-child,
.review-empty > div {
  display: grid;
  gap: var(--space-xs);
}

.review-kicker {
  color: var(--color-accent);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  font-weight: 700;
  text-transform: uppercase;
}

.review-heading h1,
.review-heading p,
.review-controls h2,
.review-controls p,
.review-empty h2,
.review-empty p,
.partial-errors p,
.review-copy p,
.review-copy small {
  margin: 0;
}

.review-heading h1 {
  color: var(--color-ink);
  font-family: var(--font-display);
  font-size: var(--text-2xl);
  line-height: 1.1;
}

.review-heading p,
.review-controls p,
.review-empty p {
  color: var(--color-muted);
  font-size: var(--text-sm);
  line-height: 1.55;
}

.refresh-action,
.open-action {
  justify-content: center;
  gap: var(--space-xs);
  min-height: 2.75rem;
  padding-inline: var(--space-md);
  border: var(--rule-thin) solid var(--color-accent);
  border-radius: var(--radius-control);
  color: var(--color-accent-strong);
  background: var(--color-surface);
  font-weight: 700;
}

.review-actions {
  justify-content: end;
  flex-wrap: wrap;
  gap: var(--space-xs);
  min-width: max-content;
}

.review-actions input,
.complete-action {
  min-height: 2.25rem;
  padding-inline: var(--space-sm);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-ink-2);
  background: var(--color-surface);
  font: inherit;
  font-size: var(--text-xs);
}

.complete-action {
  border-color: var(--color-accent);
  color: var(--color-accent-strong);
  font-weight: 700;
  cursor: pointer;
}

.complete-action:disabled,
.review-actions input:disabled {
  cursor: wait;
  opacity: 0.6;
}

.action-error {
  margin: 0;
  padding: var(--space-sm) var(--space-md);
  border-left: 3px solid var(--color-danger);
  color: var(--color-danger-strong);
  background: var(--color-danger-soft);
}

.refresh-action:disabled {
  border-color: var(--color-rule-2);
  color: var(--color-faint);
}

.review-overview {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-sm);
}

.review-overview article {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: var(--space-xs) var(--space-sm);
  padding: var(--space-lg);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-surface);
  color: var(--color-accent);
  background: var(--color-surface);
}

.review-overview article.urgent {
  border-color: var(--color-warning);
  color: var(--color-warning);
  background: var(--color-warning-soft);
}

.review-overview article span {
  align-self: center;
  color: var(--color-muted);
  font-size: var(--text-xs);
}

.review-overview article strong {
  grid-column: 1 / -1;
  color: var(--color-ink);
  font-family: var(--font-display);
  font-size: var(--text-2xl);
}

.review-controls {
  justify-content: space-between;
  gap: var(--space-lg);
}

.review-controls h2 {
  color: var(--color-ink);
  font-size: var(--text-lg);
}

.review-filters {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: var(--space-xs);
}

.review-filters button {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  min-height: 2.5rem;
  padding-inline: var(--space-sm);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-muted);
  background: var(--color-surface);
  font-size: var(--text-xs);
  font-weight: 700;
}

.review-filters button.active {
  border-color: var(--color-accent);
  color: var(--color-accent-strong);
  background: var(--color-accent-soft);
}

.review-filters button span {
  display: grid;
  min-width: 1.25rem;
  height: 1.25rem;
  place-items: center;
  border-radius: var(--radius-round);
  background: var(--color-paper-3);
  font-family: var(--font-mono);
}

.partial-errors {
  display: grid;
  gap: var(--space-xs);
}

.partial-errors p {
  align-items: flex-start;
  gap: var(--space-xs);
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-control);
  color: var(--color-warning);
  background: var(--color-warning-soft);
  font-size: var(--text-sm);
}

.review-list {
  min-width: 0;
}

.review-list ol {
  display: grid;
  gap: var(--space-sm);
  margin: 0;
  padding: 0;
  list-style: none;
}

.review-list li {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-md);
  padding: var(--space-lg);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-surface);
  background: var(--color-surface);
  box-shadow: var(--shadow-whisper);
}

.review-kind {
  display: grid;
  width: 2.75rem;
  height: 2.75rem;
  place-items: center;
  border-radius: var(--radius-control);
  color: var(--color-accent);
  background: var(--color-accent-soft);
}

.review-kind.knowledge {
  color: var(--color-success);
  background: var(--color-success-soft);
}

.review-copy {
  display: grid;
  gap: var(--space-xs);
  min-width: 0;
}

.review-item-heading {
  justify-content: space-between;
  gap: var(--space-md);
}

.review-item-heading > div {
  display: grid;
  gap: var(--space-2xs);
  min-width: 0;
}

.review-item-heading > div span,
.review-copy small,
.review-meta {
  color: var(--color-muted);
  font-size: var(--text-xs);
}

.review-item-heading strong {
  color: var(--color-ink);
  font-size: var(--text-md);
  overflow-wrap: anywhere;
}

.timing-badge {
  flex: 0 0 auto;
  padding: var(--space-2xs) var(--space-xs);
  border-radius: var(--radius-round);
  color: var(--color-muted);
  background: var(--color-paper-3);
  font-size: var(--text-xs);
  font-weight: 700;
}

.timing-badge.overdue,
.timing-badge.today {
  color: var(--color-warning);
  background: var(--color-warning-soft);
}

.timing-badge.completed {
  color: var(--color-success);
  background: var(--color-success-soft);
}

.review-copy p {
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  line-height: 1.55;
}

.review-copy small {
  line-height: 1.5;
}

.review-meta {
  flex-wrap: wrap;
  gap: var(--space-xs) var(--space-md);
}

.review-meta span + span::before {
  margin-right: var(--space-xs);
  color: var(--color-rule-2);
  content: "/";
}

.open-action {
  display: inline-flex;
  flex: 0 0 auto;
}

.review-state,
.review-empty {
  min-height: 16rem;
  justify-content: center;
  gap: var(--space-md);
  padding: var(--space-xl);
  border: var(--rule-thin) dashed var(--color-rule-2);
  border-radius: var(--radius-surface);
  color: var(--color-muted);
  background: var(--color-paper-2);
}

.review-state {
  display: grid;
  place-items: center;
}

.review-empty h2 {
  color: var(--color-ink);
  font-size: var(--text-lg);
}

@media (hover: hover) and (pointer: fine) {
  .refresh-action:not(:disabled):hover,
  .open-action:hover {
    color: var(--color-accent-ink);
    background: var(--color-accent);
  }

  .review-filters button:hover:not(.active) {
    color: var(--color-ink-2);
    background: var(--color-paper-2);
  }
}

@media (max-width: 68rem) {
  .review-center {
    margin-left: 0;
  }
}

@media (max-width: 58rem) {
  .review-overview {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .review-heading,
  .review-controls {
    align-items: stretch;
    flex-direction: column;
  }

  .review-filters {
    justify-content: flex-start;
  }
}

@media (max-width: 40rem) {
  .review-center {
    width: calc(100% - (var(--space-md) * 2));
    margin-inline: auto;
    padding: var(--space-lg) 0 var(--space-xl);
  }

  .review-overview {
    grid-template-columns: minmax(0, 1fr);
  }

  .refresh-action {
    width: 100%;
  }

  .review-filters {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .review-filters button {
    justify-content: space-between;
  }

  .review-list li {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .review-item-heading {
    align-items: flex-start;
    flex-direction: column;
  }

  .review-actions {
    justify-content: start;
    min-width: 0;
  }

  .open-action {
    grid-column: 1 / -1;
    width: 100%;
  }
}
</style>
