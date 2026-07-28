<script setup lang="ts">
import { computed, reactive, watch } from "vue";

import type { InterviewSessionStartPayload } from "@/api/interview";
import InterviewTextFileField from "@/components/interview/InterviewTextFileField.vue";

defineProps<{
  busy: boolean;
}>();

const emit = defineEmits<{
  start: [payload: InterviewSessionStartPayload];
}>();

interface InterviewSetupFormData {
  targetRole: string;
  resumeText: string;
  jobDescription: string;
  initialQuestionCount: number;
  maxQuestionCount: number;
}

const form = reactive<InterviewSetupFormData>({
  targetRole: "Java 后端开发工程师",
  resumeText: "",
  jobDescription: "",
  initialQuestionCount: 5,
  maxQuestionCount: 8,
});

const initialQuestionOptions = [3, 4, 5, 6, 7, 8];
const maxQuestionOptions = computed(() =>
  Array.from({ length: 13 - form.initialQuestionCount }, (_, index) => form.initialQuestionCount + index),
);

watch(
  () => form.initialQuestionCount,
  (count) => {
    if (form.maxQuestionCount < count) {
      form.maxQuestionCount = count;
    }
  },
);

function handleSubmit(): void {
  emit("start", {
    targetRole: form.targetRole.trim(),
    resumeText: form.resumeText.trim(),
    jobDescription: form.jobDescription?.trim() || undefined,
    initialQuestionCount: form.initialQuestionCount,
    maxQuestionCount: form.maxQuestionCount,
  });
}
</script>

<template>
  <form class="setup-card" @submit.prevent="handleSubmit">
    <header>
      <span>01 / 面试配置</span>
      <h2>用真实经历开始一场面试</h2>
      <p>系统先理解目标岗位和简历，再生成首轮题目。回答不足时可继续追问，但不会超过整场上限。</p>
    </header>

    <label>
      <span>目标岗位</span>
      <input
        v-model="form.targetRole"
        type="text"
        maxlength="100"
        required
        autocomplete="organization-title"
        placeholder="例如：Java AI 应用开发工程师"
      />
    </label>

    <InterviewTextFileField
      v-model="form.resumeText"
      input-id="resume"
      label="简历"
      :rows="10"
      :max-length="50000"
      placeholder="也可以直接粘贴简历中的技能、项目和工作经历。"
      :busy="busy"
    />

    <InterviewTextFileField
      v-model="form.jobDescription"
      input-id="job-description"
      label="岗位描述"
      optional
      :rows="5"
      :max-length="30000"
      placeholder="也可以直接粘贴 JD，让题目更贴近岗位要求。"
      :busy="busy"
    />

    <div class="count-grid">
      <label>
        <span>初始题数</span>
        <select v-model="form.initialQuestionCount">
          <option v-for="count in initialQuestionOptions" :key="count" :value="count">
            {{ count }} 道
          </option>
        </select>
      </label>
      <label>
        <span>整场上限</span>
        <select v-model="form.maxQuestionCount">
          <option v-for="count in maxQuestionOptions" :key="count" :value="count">
            {{ count }} 道
          </option>
        </select>
      </label>
    </div>

    <div class="form-footer">
      <p>默认 5 道首轮题，最多进行 8 道；不是固定只有 3 道。</p>
      <button type="submit" :disabled="busy">
        {{ busy ? "正在生成画像与题目…" : "开始模拟面试" }}
      </button>
    </div>
  </form>
</template>

<style scoped lang="scss">
.setup-card {
  display: grid;
  gap: var(--space-lg);
  padding: var(--space-xl);
  border: var(--rule-thin) solid var(--color-rule);
  border-radius: var(--radius-surface);
  background: var(--color-surface);
  box-shadow: var(--shadow-whisper);
}

header {
  display: grid;
  gap: var(--space-xs);
  padding-bottom: var(--space-md);
  border-bottom: var(--rule-thin) solid var(--color-rule);
}

header > span {
  color: var(--color-accent);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

h2,
p {
  margin: 0;
}

h2 {
  font-size: var(--text-xl);
  letter-spacing: -0.03em;
}

header p,
.form-footer p {
  color: var(--color-muted);
  line-height: 1.65;
}

label {
  display: grid;
  gap: var(--space-xs);
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  font-weight: 600;
}

input,
select {
  width: 100%;
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-ink);
  background: var(--color-paper);
}

input,
select {
  min-height: 3rem;
  padding-inline: var(--space-md);
}

.count-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-md);
}

.form-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-lg);
  padding-top: var(--space-md);
  border-top: var(--rule-thin) solid var(--color-rule);
}

.form-footer p {
  max-width: 38rem;
  font-size: var(--text-sm);
}

button {
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
  .setup-card {
    padding: var(--space-lg) var(--space-md);
  }

  .count-grid,
  .form-footer {
    grid-template-columns: minmax(0, 1fr);
  }

  .form-footer {
    display: grid;
  }

  button {
    width: 100%;
  }
}
</style>
