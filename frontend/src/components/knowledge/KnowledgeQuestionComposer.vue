<script setup lang="ts">
import { computed } from "vue";
import { LoaderCircle, Search, X } from "@lucide/vue";

const props = defineProps<{
  busy: boolean;
  hasResult: boolean;
  maxLength: number;
}>();

const emit = defineEmits<{
  submit: [];
  clear: [];
}>();

const question = defineModel<string>({ required: true });
const questionLength = computed(() => question.value.length);
const canSubmit = computed(() => question.value.trim().length > 0
  && questionLength.value <= props.maxLength
  && !props.busy);

function handleKeydown(event: KeyboardEvent): void {
  if ((event.ctrlKey || event.metaKey) && event.key === "Enter" && canSubmit.value) {
    event.preventDefault();
    emit("submit");
  }
}
</script>

<template>
  <section id="knowledge-question" class="question-composer" aria-labelledby="question-title">
    <header>
      <div>
        <h2 id="question-title">你的问题</h2>
        <p>Java、项目实践或已导入资料中的具体问题</p>
      </div>
      <button
        v-if="question || hasResult"
        class="clear-button"
        type="button"
        aria-label="清空当前问答"
        title="清空当前问答"
        :disabled="busy"
        @click="emit('clear')"
      >
        <X :size="17" aria-hidden="true" />
      </button>
    </header>

    <label for="knowledge-question-input">问题</label>
    <textarea
      id="knowledge-question-input"
      v-model="question"
      rows="8"
      :maxlength="maxLength"
      placeholder="例如：HashMap 在什么条件下会从链表转换为红黑树？"
      :disabled="busy"
      @keydown="handleKeydown"
    ></textarea>

    <footer>
      <span :class="{ 'is-limit': questionLength >= maxLength }">
        {{ questionLength }} / {{ maxLength }}
      </span>
      <button
        class="submit-button"
        type="button"
        :disabled="!canSubmit"
        @click="emit('submit')"
      >
        <LoaderCircle v-if="busy" class="loading-icon" :size="17" aria-hidden="true" />
        <Search v-else :size="17" aria-hidden="true" />
        <span>{{ busy ? "正在检索" : "查找答案" }}</span>
      </button>
    </footer>
  </section>
</template>

<style scoped lang="scss">
.question-composer {
  display: grid;
  gap: var(--space-md);
  padding: var(--space-lg);
  border-radius: var(--radius-surface);
  background: var(--color-paper-2);
}

header,
footer,
.submit-button,
.clear-button {
  display: flex;
  align-items: center;
}

header,
footer {
  justify-content: space-between;
  gap: var(--space-md);
}

header > div {
  display: grid;
  gap: var(--space-2xs);
}

h2,
p {
  margin: 0;
}

h2 {
  color: var(--color-ink);
  font-size: var(--text-lg);
}

p,
footer > span {
  color: var(--color-muted);
  font-size: var(--text-sm);
}

label {
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  font-weight: 700;
}

textarea {
  width: 100%;
  min-height: 13rem;
  resize: vertical;
  padding: var(--space-md);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  outline: 0;
  color: var(--color-ink);
  background: var(--color-surface);
  font: inherit;
  line-height: 1.65;
  transition: border-color var(--dur-short) var(--ease-out), box-shadow var(--dur-short) var(--ease-out);
}

textarea::placeholder {
  color: var(--color-muted);
}

textarea:focus-visible {
  border-color: var(--color-focus);
  box-shadow: 0 0 0 var(--rule-focus) var(--color-accent-soft);
}

textarea:disabled {
  cursor: wait;
  opacity: 0.72;
}

.clear-button,
.submit-button {
  justify-content: center;
  border-radius: var(--radius-control);
}

.clear-button {
  width: 2.5rem;
  height: 2.5rem;
  border: var(--rule-thin) solid transparent;
  color: var(--color-muted);
  background: transparent;
}

.submit-button {
  gap: var(--space-xs);
  min-height: 2.75rem;
  padding-inline: var(--space-md);
  border: var(--rule-thin) solid var(--color-accent-strong);
  color: var(--color-accent-ink);
  background: var(--color-accent-strong);
  font-weight: 700;
}

.submit-button:disabled {
  cursor: not-allowed;
  border-color: var(--color-rule-2);
  color: var(--color-faint);
  background: var(--color-paper-3);
}

.is-limit {
  color: var(--color-warning);
  font-weight: 700;
}

.loading-icon {
  animation: loading-spin 800ms linear infinite;
}

@keyframes loading-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (hover: hover) and (pointer: fine) {
  .clear-button:hover:not(:disabled) {
    color: var(--color-ink);
    background: var(--color-paper-3);
  }

  .submit-button:hover:not(:disabled) {
    background: var(--color-accent);
  }
}

@media (prefers-reduced-motion: reduce) {
  .loading-icon {
    animation: none;
  }
}
</style>
