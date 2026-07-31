<script setup lang="ts">
import { BookOpenCheck, Info, Send } from "@lucide/vue";
import { ref, watch } from "vue";

import type { InterviewQuestion } from "@/api/interview";

const props = defineProps<{
  question: InterviewQuestion;
  questionIndex: number;
  questionCount: number;
  maxQuestionCount: number;
  busy: boolean;
}>();

const emit = defineEmits<{
  submit: [answer: string];
}>();

const answer = ref("");

watch(
  () => props.questionIndex,
  () => {
    answer.value = "";
  },
);

function handleSubmit(): void {
  const normalized = answer.value.trim();
  if (normalized) {
    emit("submit", normalized);
  }
}
</script>

<template>
  <section class="question-card" aria-labelledby="current-question-title">
    <header>
      <div>
        <span>第 {{ questionIndex + 1 }} 题</span>
        <strong>共 {{ questionCount }} 题</strong>
      </div>
      <p>{{ question.objective }}</p>
    </header>

    <div class="question-copy">
      <h2 id="current-question-title">{{ question.question }}</h2>
      <p :class="['grounding', { grounded: question.knowledgeGrounded }]">
        <BookOpenCheck v-if="question.knowledgeGrounded" :size="15" aria-hidden="true" />
        <Info v-else :size="15" aria-hidden="true" />
        {{ question.knowledgeGrounded ? "有资料参考" : "仅根据回答评价" }}
      </p>
    </div>

    <details v-if="question.knowledgeSources.length" class="sources">
      <summary>查看参考资料（{{ question.knowledgeSources.length }}）</summary>
      <ul>
        <li v-for="source in question.knowledgeSources" :key="`${source.source}-${source.chunkOrder}`">
          <strong>{{ source.source }}</strong>
          <span>{{ source.headingPath.join(" / ") || "无标题路径" }}</span>
        </li>
      </ul>
    </details>

    <form @submit.prevent="handleSubmit">
      <label for="interview-answer">你的回答</label>
      <textarea
        id="interview-answer"
        v-model="answer"
        rows="11"
        maxlength="10000"
        :disabled="busy"
        placeholder="先说结论，再说明原理、实现和边界。"
      ></textarea>
      <div class="answer-footer">
        <span>{{ answer.trim().length }} / 10000</span>
        <button type="submit" :disabled="busy || !answer.trim()">
          <Send :size="16" aria-hidden="true" />
          <span>{{ busy ? "正在提交…" : "提交回答" }}</span>
        </button>
      </div>
    </form>
  </section>
</template>

<style scoped lang="scss">
.question-card {
  display: grid;
  gap: var(--space-lg);
  min-width: 0;
  padding: var(--space-xl);
  border: var(--rule-thin) solid var(--color-rule);
  border-radius: var(--radius-surface);
  background: var(--color-surface);
}

header,
header > div,
.answer-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
}

header {
  padding-bottom: var(--space-md);
  border-bottom: var(--rule-thin) solid var(--color-rule);
}

header span,
header strong,
.answer-footer span {
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

header span,
header p,
.answer-footer span {
  color: var(--color-muted);
}

header strong {
  color: var(--color-muted);
}

header p,
.question-copy p,
h2 {
  margin: 0;
}

header p {
  max-width: 28rem;
  font-size: var(--text-xs);
  text-align: right;
}

.question-copy {
  display: grid;
  gap: var(--space-sm);
}

h2 {
  max-width: 34ch;
  font-size: var(--text-xl);
  line-height: 1.3;
  text-wrap: balance;
}

.grounding {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  width: fit-content;
  color: var(--color-info);
  font-size: var(--text-sm);
}

.grounding.grounded {
  color: var(--color-success);
}

.sources {
  padding: var(--space-md);
  border: var(--rule-thin) solid var(--color-rule);
  border-radius: var(--radius-control);
  background: var(--color-paper-2);
}

.sources summary {
  color: var(--color-ink-2);
  cursor: pointer;
  font-size: var(--text-sm);
  font-weight: 600;
}

.sources ul {
  display: grid;
  gap: var(--space-sm);
  margin: var(--space-md) 0 0;
  padding: 0;
  list-style: none;
}

.sources li {
  display: grid;
  gap: var(--space-2xs);
  color: var(--color-muted);
  font-size: var(--text-xs);
}

.sources li strong {
  color: var(--color-ink-2);
}

form,
form label {
  display: grid;
  gap: var(--space-xs);
}

form label {
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  font-weight: 700;
}

textarea {
  width: 100%;
  min-height: 15rem;
  padding: var(--space-md);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-ink);
  background: var(--color-paper);
  line-height: 1.7;
  resize: vertical;
}

.answer-footer {
  align-items: end;
}

button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-xs);
  min-height: 3rem;
  padding-inline: var(--space-lg);
  border: 0;
  border-radius: var(--radius-control);
  color: var(--color-accent-ink);
  background: var(--color-accent);
  font-weight: 700;
  white-space: nowrap;
}

@media (max-width: 40rem) {
  .question-card {
    padding: var(--space-lg) var(--space-md);
  }

  header {
    display: grid;
  }

  header p {
    text-align: left;
  }

  .answer-footer {
    display: grid;
  }

  button {
    width: 100%;
  }
}
</style>
