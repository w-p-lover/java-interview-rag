<script setup lang="ts">
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
        <span>当前题目</span>
        <strong>{{ questionIndex + 1 }} / {{ questionCount }}</strong>
      </div>
      <p>整场最多 {{ maxQuestionCount }} 道，追问会追加到当前队列末尾。</p>
    </header>

    <div class="question-copy">
      <p class="objective">考察目标 · {{ question.objective }}</p>
      <h2 id="current-question-title">{{ question.question }}</h2>
      <p :class="['grounding', { grounded: question.knowledgeGrounded }]">
        {{ question.knowledgeGrounded ? "已绑定知识库资料" : "暂无可靠知识资料，评分会降低事实确认强度" }}
      </p>
    </div>

    <details v-if="question.knowledgeSources.length" class="sources">
      <summary>查看 {{ question.knowledgeSources.length }} 条评分依据</summary>
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
        placeholder="像真实面试一样直接作答。建议先说结论，再说明原理、实现和异常边界。"
      ></textarea>
      <div class="answer-footer">
        <span>{{ answer.trim().length }} / 10000</span>
        <button type="submit" :disabled="busy || !answer.trim()">
          {{ busy ? "正在评分并判断追问…" : "提交本题回答" }}
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
  box-shadow: var(--shadow-whisper);
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
.objective,
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
  color: var(--color-accent);
}

header p,
.question-copy p,
h2 {
  margin: 0;
}

header p {
  font-size: var(--text-xs);
  text-align: right;
}

.question-copy {
  display: grid;
  gap: var(--space-sm);
}

.objective {
  color: var(--color-accent);
}

h2 {
  max-width: 30ch;
  font-size: var(--text-xl);
  letter-spacing: -0.035em;
  line-height: 1.3;
  text-wrap: balance;
}

.grounding {
  color: var(--color-warning);
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
  min-height: 3rem;
  padding-inline: var(--space-lg);
  border: 0;
  border-radius: var(--radius-control);
  color: var(--color-accent-ink);
  background: var(--color-accent);
  font-weight: 700;
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
