<script setup lang="ts">
import type { InterviewReviewTask } from "@/api/interview";

defineProps<{
  tasks: InterviewReviewTask[];
  loading: boolean;
  error: string;
  generated: boolean;
}>();

const emit = defineEmits<{
  retry: [];
}>();

function formatSimilarity(similarity: number | null): string {
  return similarity === null ? "未记录" : `${Math.round(similarity * 100)}%`;
}

function formatQuestionNumber(questionIndex: number): string {
  return String(questionIndex + 1).padStart(2, "0");
}
</script>

<template>
  <section class="review-card" aria-labelledby="review-task-title">
    <header class="review-header">
      <div>
        <span>复盘清单 / {{ tasks.length }} ITEMS</span>
        <h2 id="review-task-title">把本场遗漏变成下一次的准备</h2>
      </div>
      <p>每条任务都来自已保存的逐题评价；有可靠资料时，同时保留可追溯的知识来源。</p>
    </header>

    <div v-if="loading" class="review-state" aria-live="polite">
      正在整理本场复习任务…
    </div>

    <div v-else-if="error" class="review-state review-error" role="alert">
      <div>
        <strong>复习任务暂未就绪</strong>
        <p>{{ error }}</p>
      </div>
      <button type="button" @click="emit('retry')">重新生成复习任务</button>
    </div>

    <ol v-else-if="tasks.length" class="review-list">
      <li v-for="task in tasks" :key="task.id" class="review-item">
        <div class="review-marker" aria-hidden="true">
          <span>Q{{ formatQuestionNumber(task.questionIndex) }}</span>
        </div>

        <div class="review-body">
          <div class="review-question">
            <span>{{ task.objective }}</span>
            <h3>{{ task.question }}</h3>
          </div>

          <div class="review-gaps">
            <strong>这次遗漏</strong>
            <ul>
              <li v-for="gap in task.gaps" :key="gap">{{ gap }}</li>
            </ul>
          </div>

          <div class="review-suggestion">
            <strong>下次回答前</strong>
            <p>{{ task.improvementSuggestion }}</p>
          </div>
        </div>

        <aside class="source-ledger" :class="{ grounded: task.knowledgeGrounded }">
          <strong>{{ task.knowledgeGrounded ? "知识依据" : "来源状态" }}</strong>
          <template v-if="task.knowledgeSources.length">
            <div
              v-for="source in task.knowledgeSources"
              :key="`${source.source}:${source.documentVersion}:${source.chunkOrder}`"
            >
              <span>{{ source.source }}</span>
              <p>{{ source.headingPath.length ? source.headingPath.join(" / ") : "未记录标题路径" }}</p>
              <small>Chunk {{ source.chunkOrder }} · 相似度 {{ formatSimilarity(source.similarity) }}</small>
            </div>
          </template>
          <p v-else>本题没有可靠知识来源，先根据遗漏和改进建议复习。</p>
        </aside>
      </li>
    </ol>

    <div v-else class="review-state review-empty">
      <div>
        <strong>{{ generated ? "本场没有需要沉淀的遗漏" : "当前没有已保存的复习任务" }}</strong>
        <p>
          {{
            generated
              ? "逐题评价没有返回遗漏项，可以直接开始下一场训练。"
              : "可以重新检查这场面试；生成过程不会再次调用模型。"
          }}
        </p>
      </div>
      <button v-if="!generated" type="button" @click="emit('retry')">检查复习任务</button>
    </div>
  </section>
</template>

<style scoped lang="scss">
.review-card {
  display: grid;
  gap: var(--space-xl);
  padding: var(--space-xl);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-surface);
  background: var(--color-surface);
  box-shadow: var(--shadow-whisper);
}

.review-header {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(16rem, 0.55fr);
  align-items: end;
  gap: var(--space-xl);
  padding-bottom: var(--space-lg);
  border-bottom: var(--rule-thin) solid var(--color-rule);
}

.review-header > div,
.review-question,
.review-gaps,
.review-suggestion,
.source-ledger,
.source-ledger div,
.review-state > div {
  display: grid;
  gap: var(--space-xs);
}

.review-header span,
.review-question span {
  color: var(--color-accent);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

h2,
h3,
p {
  margin: 0;
}

h2 {
  font-size: var(--text-xl);
  letter-spacing: -0.03em;
}

.review-header > p {
  color: var(--color-muted);
  font-size: var(--text-sm);
  line-height: 1.65;
}

.review-list {
  display: grid;
  gap: 0;
  margin: 0;
  padding: 0;
  list-style: none;
}

.review-item {
  display: grid;
  grid-template-columns: 4rem minmax(0, 1fr) minmax(16rem, 0.58fr);
  gap: var(--space-lg);
  padding-block: var(--space-lg);
  border-bottom: var(--rule-thin) solid var(--color-rule);
}

.review-item:first-child {
  padding-top: 0;
}

.review-item:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.review-marker {
  position: relative;
  display: grid;
  justify-items: center;
}

.review-marker::after {
  position: absolute;
  top: 2.75rem;
  bottom: calc(var(--space-lg) * -1);
  width: var(--rule-thin);
  background: var(--color-rule-2);
  content: "";
}

.review-item:last-child .review-marker::after {
  display: none;
}

.review-marker span {
  display: grid;
  place-items: center;
  width: 2.75rem;
  height: 2.75rem;
  border: var(--rule-thin) solid var(--color-accent);
  border-radius: var(--radius-control);
  color: var(--color-accent-strong);
  background: var(--color-accent-soft);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  font-weight: 700;
}

.review-body {
  display: grid;
  gap: var(--space-md);
  min-width: 0;
}

.review-question h3 {
  color: var(--color-ink);
  font-size: var(--text-lg);
  letter-spacing: -0.025em;
  line-height: 1.35;
}

.review-gaps strong,
.review-suggestion strong,
.source-ledger > strong,
.review-state strong {
  color: var(--color-ink-2);
  font-size: var(--text-sm);
}

.review-gaps ul {
  display: grid;
  gap: var(--space-xs);
  margin: 0;
  padding-left: var(--space-lg);
  color: var(--color-muted);
  font-size: var(--text-sm);
}

.review-suggestion {
  padding: var(--space-md);
  border-left: 0.1875rem solid var(--color-accent);
  background: var(--color-accent-soft);
}

.review-suggestion p,
.source-ledger p,
.review-state p {
  color: var(--color-muted);
  font-size: var(--text-sm);
  line-height: 1.65;
}

.source-ledger {
  align-content: start;
  padding: var(--space-md);
  border: var(--rule-thin) solid var(--color-warning-rule);
  border-radius: var(--radius-control);
  background: var(--color-warning-soft);
}

.source-ledger.grounded {
  border-color: var(--color-success-rule);
  background: var(--color-success-soft);
}

.source-ledger div + div {
  padding-top: var(--space-sm);
  border-top: var(--rule-thin) solid var(--color-success-rule);
}

.source-ledger span,
.source-ledger small {
  overflow-wrap: anywhere;
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.source-ledger span {
  color: var(--color-ink-2);
  font-weight: 700;
}

.source-ledger small {
  color: var(--color-muted);
}

.review-state {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-lg);
  min-height: 6rem;
  padding: var(--space-lg);
  border: var(--rule-thin) dashed var(--color-rule-2);
  border-radius: var(--radius-control);
  background: var(--color-paper-2);
}

.review-error {
  border-color: var(--color-error-rule);
  background: var(--color-error-soft);
}

button {
  min-height: 2.75rem;
  padding-inline: var(--space-md);
  border: var(--rule-thin) solid var(--color-accent);
  border-radius: var(--radius-control);
  color: var(--color-accent-strong);
  background: var(--color-surface);
  font-weight: 700;
  white-space: nowrap;
}

@media (max-width: 62rem) {
  .review-header,
  .review-item {
    grid-template-columns: minmax(0, 1fr);
  }

  .review-marker {
    justify-items: start;
  }

  .review-marker::after {
    display: none;
  }
}

@media (max-width: 40rem) {
  .review-card {
    padding: var(--space-lg) var(--space-md);
  }

  .review-header,
  .review-item {
    gap: var(--space-md);
  }

  .review-state {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
  }

  button {
    width: 100%;
  }
}
</style>
