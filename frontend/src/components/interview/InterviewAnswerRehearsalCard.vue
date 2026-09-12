<script setup lang="ts">
import { AlertTriangle, Check, ClipboardCheck, Copy, ListOrdered } from "@lucide/vue";
import { computed, shallowRef } from "vue";

import type { InterviewQuestion, InterviewSessionAnswer } from "@/api/interview";

const props = defineProps<{
  question: InterviewQuestion;
  answer: InterviewSessionAnswer;
  reviewTaskId?: string;
}>();

const emit = defineEmits<{
  practice: [taskId: string];
}>();

const copyState = shallowRef<"idle" | "copied" | "failed">("idle");

const rehearsalSteps = computed(() => [
  {
    title: "先给结论",
    detail: `先用一句话回答“${props.question.question}”，让面试官先知道你的判断。`,
  },
  {
    title: "再讲原理或实现",
    detail: `围绕题目目标“${props.question.objective}”，补充关键机制、实现步骤或判断依据。`,
  },
  {
    title: "最后讲边界与取舍",
    detail: "补充一个失败边界、风险或技术取舍，说明这个结论在什么情况下不成立。",
  },
]);

const mustCoverPoints = computed(() => props.answer.evaluation.gaps.length
  ? props.answer.evaluation.gaps
  : ["本次评分没有记录明确遗漏；复述时仍要保留一个边界、风险或取舍。"]);

const selfChecks = computed(() => [
  "我能在 30 秒内先说清结论，而不是从背景铺陈开始。",
  `我能解释题目目标“${props.question.objective}”，而不只是复述术语。`,
  "我能补充一个边界、风险或取舍，并说明为什么这样设计。",
]);

const knowledgeSources = computed(() => props.answer.evaluation.knowledgeEvidence);

const copyText = computed(() => {
  const lines = [
    `# 回答复述卡：${props.question.question}`,
    "",
    `## 题目目标\n${props.question.objective}`,
    "",
    "## 建议口述顺序",
    ...rehearsalSteps.value.map((step, index) => `${index + 1}. **${step.title}**：${step.detail}`),
    "",
    "## 本次必须补充",
    ...mustCoverPoints.value.map((point) => `- ${point}`),
    "",
    `## 评分改进建议\n${props.answer.evaluation.improvementSuggestion}`,
    "",
    "## 口述自检",
    ...selfChecks.value.map((check) => `- [ ] ${check}`),
  ];
  if (knowledgeSources.value.length) {
    lines.push("", "## 已验真知识依据");
    lines.push(...knowledgeSources.value.map((source) => {
      const heading = source.headingPath.join(" / ");
      const label = heading ? `${source.source} · ${heading}` : source.source;
      return `- ${label}：${source.quote}`;
    }));
  }
  return lines.join("\n");
});

async function copyRehearsalCard(): Promise<void> {
  try {
    await navigator.clipboard.writeText(copyText.value);
    copyState.value = "copied";
    window.setTimeout(() => {
      if (copyState.value === "copied") copyState.value = "idle";
    }, 1800);
  } catch {
    copyState.value = "failed";
  }
}
</script>

<template>
  <section class="rehearsal-card" aria-labelledby="rehearsal-card-title">
    <header class="rehearsal-heading">
      <div class="rehearsal-title">
        <span class="rehearsal-mark" aria-hidden="true"><ClipboardCheck :size="18" /></span>
        <div>
          <h2 id="rehearsal-card-title">回答复述卡</h2>
          <p>把本次评分整理成下一次可以直接口述的结构，不是标准答案。</p>
        </div>
      </div>
      <button
        type="button"
        class="copy-action"
        :class="{ 'copy-action--failed': copyState === 'failed' }"
        @click="copyRehearsalCard"
      >
        <Check v-if="copyState === 'copied'" :size="15" aria-hidden="true" />
        <AlertTriangle v-else-if="copyState === 'failed'" :size="15" aria-hidden="true" />
        <Copy v-else :size="15" aria-hidden="true" />
        <span>{{ copyState === "copied" ? "已复制" : copyState === "failed" ? "复制失败" : "复制复述卡" }}</span>
      </button>
      <button
        v-if="props.reviewTaskId"
        type="button"
        class="practice-action"
        @click="emit('practice', props.reviewTaskId)"
      >
        <ListOrdered :size="15" aria-hidden="true" />
        <span>进入同题再练</span>
      </button>
    </header>

    <p v-if="copyState === 'failed'" class="copy-hint" role="status">
      复制权限不可用，请手动选择卡片内容。
    </p>

    <div class="rehearsal-layout">
      <section class="rehearsal-section">
        <h3><ListOrdered :size="16" aria-hidden="true" />建议口述顺序</h3>
        <ol class="step-list">
          <li v-for="step in rehearsalSteps" :key="step.title">
            <strong>{{ step.title }}</strong>
            <p>{{ step.detail }}</p>
          </li>
        </ol>
      </section>

      <section class="rehearsal-section rehearsal-section--must-cover">
        <h3><AlertTriangle :size="16" aria-hidden="true" />本次必须补充</h3>
        <ul>
          <li v-for="point in mustCoverPoints" :key="point">{{ point }}</li>
        </ul>
      </section>

      <section class="rehearsal-section">
        <h3><ClipboardCheck :size="16" aria-hidden="true" />口述自检</h3>
        <ul class="check-list">
          <li v-for="check in selfChecks" :key="check">{{ check }}</li>
        </ul>
      </section>

      <section v-if="knowledgeSources.length" class="rehearsal-section rehearsal-section--evidence">
        <h3><ClipboardCheck :size="16" aria-hidden="true" />已验真知识依据</h3>
        <ul>
          <li v-for="source in knowledgeSources" :key="`${source.source}-${source.chunkOrder}-${source.quote}`">
            <strong>{{ source.source }}<span v-if="source.headingPath.length"> · {{ source.headingPath.join(" / ") }}</span></strong>
            <p>{{ source.quote }}</p>
          </li>
        </ul>
      </section>
    </div>

    <section class="improvement-note">
      <strong>评分改进建议</strong>
      <p>{{ answer.evaluation.improvementSuggestion }}</p>
    </section>
  </section>
</template>

<style scoped lang="scss">
.rehearsal-card {
  display: grid;
  grid-column: 1 / -1;
  gap: var(--space-md);
  min-width: 0;
  padding: var(--space-xl);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-surface);
  color: var(--color-ink);
  background: var(--color-surface);
  box-shadow: var(--shadow-whisper);
}

.rehearsal-heading,
.rehearsal-title,
.copy-action,
.practice-action,
.rehearsal-section h3 {
  display: flex;
  align-items: center;
}

.rehearsal-heading {
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--space-md);
}

.rehearsal-title {
  align-items: flex-start;
  gap: var(--space-sm);
}

.rehearsal-mark {
  display: grid;
  flex: 0 0 auto;
  width: 2.25rem;
  height: 2.25rem;
  place-items: center;
  border-radius: var(--radius-control);
  color: var(--color-accent);
  background: var(--color-accent-soft);
}

h2,
h3,
p,
ol,
ul {
  margin: 0;
}

.rehearsal-title div {
  display: grid;
  gap: var(--space-2xs);
}

.rehearsal-title h2 {
  font-size: var(--text-lg);
}

.rehearsal-title p,
.copy-hint,
.step-list p,
.improvement-note p,
.rehearsal-section li,
.rehearsal-section--evidence p {
  color: var(--color-muted);
  font-size: var(--text-sm);
  line-height: 1.6;
}

.copy-action {
  justify-content: center;
  flex: 0 0 auto;
  gap: var(--space-xs);
  min-height: 2.25rem;
  padding: 0 var(--space-sm);
  border: var(--rule-thin) solid var(--color-accent);
  border-radius: var(--radius-control);
  color: var(--color-accent);
  background: transparent;
  cursor: pointer;
  font-size: var(--text-xs);
  font-weight: 700;
}

.practice-action {
  justify-content: center;
  flex: 0 0 auto;
  gap: var(--space-xs);
  min-height: 2.25rem;
  padding: 0 var(--space-sm);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-accent-strong);
  background: var(--color-surface);
  cursor: pointer;
  font-size: var(--text-xs);
  font-weight: 700;
}

.copy-action--failed {
  border-color: var(--color-error-rule);
  color: var(--color-error);
}

.copy-action:focus-visible {
  outline: var(--rule-focus) solid var(--color-focus);
  outline-offset: var(--rule-thin);
}

.practice-action:focus-visible {
  outline: var(--rule-focus) solid var(--color-focus);
  outline-offset: var(--rule-thin);
}

.copy-hint {
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-control);
  color: var(--color-error);
  background: var(--color-error-soft);
}

.rehearsal-layout {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-lg);
}

.rehearsal-section {
  display: grid;
  align-content: start;
  gap: var(--space-sm);
  min-width: 0;
}

.rehearsal-section h3 {
  gap: var(--space-xs);
  color: var(--color-ink);
  font-size: var(--text-sm);
}

.rehearsal-section h3 svg {
  color: var(--color-accent);
}

.step-list,
.rehearsal-section ul {
  display: grid;
  gap: var(--space-sm);
  padding-left: var(--space-lg);
}

.step-list li,
.rehearsal-section li {
  padding-left: var(--space-2xs);
}

.step-list strong,
.rehearsal-section--evidence strong,
.improvement-note strong {
  color: var(--color-ink);
  font-size: var(--text-sm);
}

.step-list p,
.rehearsal-section--evidence p {
  margin-top: var(--space-2xs);
}

.rehearsal-section--must-cover {
  padding: var(--space-md);
  border-radius: var(--radius-control);
  background: var(--color-warning-soft);
}

.rehearsal-section--must-cover h3 svg {
  color: var(--color-warning);
}

.rehearsal-section--evidence {
  grid-column: 1 / -1;
  padding-top: var(--space-md);
  border-top: var(--rule-thin) solid var(--color-rule);
}

.check-list {
  list-style: "☐  ";
}

.improvement-note {
  display: grid;
  gap: var(--space-xs);
  padding-top: var(--space-md);
  border-top: var(--rule-thin) solid var(--color-rule);
}

@media (max-width: 48rem) {
  .rehearsal-heading {
    align-items: stretch;
    flex-direction: column;
  }

  .copy-action {
    width: 100%;
  }

  .practice-action {
    width: 100%;
  }

  .rehearsal-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .rehearsal-section--evidence {
    grid-column: auto;
  }
}

@media (max-width: 40rem) {
  .rehearsal-card {
    padding: var(--space-lg) var(--space-md);
  }
}
</style>
