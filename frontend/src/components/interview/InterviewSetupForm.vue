<script setup lang="ts">
import { BriefcaseBusiness, Play, SlidersHorizontal } from "@lucide/vue";
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
const canStart = computed(() => Boolean(form.targetRole.trim() && form.resumeText.trim()));
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
  if (!canStart.value) {
    return;
  }

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
  <form class="setup" @submit.prevent="handleSubmit">
    <header class="setup-heading">
      <h1>开始一场模拟面试</h1>
      <p>上传简历，选择目标岗位。题目会根据你的经历和岗位要求生成。</p>
    </header>

    <div class="setup-layout">
      <section class="materials" aria-labelledby="materials-title">
        <div class="section-heading">
          <BriefcaseBusiness :size="18" aria-hidden="true" />
          <h2 id="materials-title">面试材料</h2>
        </div>

        <label class="role-field">
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
          placeholder="粘贴简历中的技能、项目和工作经历。"
          :busy="busy"
        />

        <InterviewTextFileField
          v-model="form.jobDescription"
          input-id="job-description"
          label="岗位描述"
          optional
          :rows="5"
          :max-length="30000"
          placeholder="粘贴岗位要求，让题目更贴近目标岗位。"
          :busy="busy"
        />
      </section>

      <aside class="settings" aria-labelledby="settings-title">
        <div class="settings-heading">
          <SlidersHorizontal :size="18" aria-hidden="true" />
          <h2 id="settings-title">本场设置</h2>
        </div>

        <div class="count-grid">
          <label>
            <span>首轮题数</span>
            <select v-model="form.initialQuestionCount">
              <option v-for="count in initialQuestionOptions" :key="count" :value="count">
                {{ count }} 道
              </option>
            </select>
          </label>
          <label>
            <span>最多题数</span>
            <select v-model="form.maxQuestionCount">
              <option v-for="count in maxQuestionOptions" :key="count" :value="count">
                {{ count }} 道
              </option>
            </select>
          </label>
        </div>

        <dl class="material-state">
          <div>
            <dt>简历</dt>
            <dd>{{ form.resumeText.trim() ? "已准备" : "待上传" }}</dd>
          </div>
          <div>
            <dt>岗位描述</dt>
            <dd>{{ form.jobDescription.trim() ? "已准备" : "未添加" }}</dd>
          </div>
        </dl>

        <button type="submit" :disabled="busy || !canStart">
          <Play :size="17" aria-hidden="true" />
          <span>{{ busy ? "正在提交…" : "开始面试" }}</span>
        </button>

        <p class="settings-note">需要填写目标岗位并上传简历。</p>
      </aside>
    </div>
  </form>
</template>

<style scoped lang="scss">
.setup {
  display: grid;
  gap: var(--space-xl);
}

.setup-heading {
  display: grid;
  gap: var(--space-xs);
}

h1,
h2,
p {
  margin: 0;
}

h1 {
  color: var(--color-ink);
  font-size: var(--text-2xl);
  line-height: 1.2;
}

.setup-heading p {
  max-width: 44rem;
  color: var(--color-muted);
  font-size: var(--text-base);
}

.setup-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(18rem, 0.55fr);
  align-items: start;
  gap: var(--space-md);
}

.materials,
.settings {
  display: grid;
  gap: var(--space-lg);
  min-width: 0;
  padding: var(--space-xl);
  border: var(--rule-thin) solid var(--color-rule);
  border-radius: var(--radius-surface);
  background: var(--color-surface);
}

.section-heading,
.settings-heading {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}

.section-heading {
  color: var(--color-accent);
}

.section-heading h2,
.settings-heading h2 {
  color: var(--color-ink);
  font-size: var(--text-md);
}

.role-field,
.count-grid label {
  display: grid;
  gap: var(--space-xs);
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

.settings {
  position: sticky;
  top: calc(var(--space-lg) + 4rem);
  border-color: var(--color-graphite);
  color: var(--color-graphite-text);
  background: var(--color-graphite);
}

.settings-heading h2,
.settings-heading,
.settings label {
  color: var(--color-graphite-text);
}

.settings select {
  border-color: var(--color-graphite-muted);
  color: var(--color-graphite-text);
  background: var(--color-graphite-2);
}

.count-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-sm);
}

.material-state {
  display: grid;
  gap: var(--space-sm);
  margin: 0;
  padding-block: var(--space-md);
  border-block: var(--rule-thin) solid var(--color-graphite-2);
}

.material-state div {
  display: flex;
  justify-content: space-between;
  gap: var(--space-md);
}

.material-state dt,
.material-state dd {
  margin: 0;
  font-size: var(--text-sm);
}

.material-state dt,
.settings-note {
  color: var(--color-graphite-muted);
}

.material-state dd {
  color: var(--color-graphite-text);
  font-weight: 700;
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

.settings-note {
  text-align: center;
  font-size: var(--text-xs);
}

@media (max-width: 62rem) {
  .setup-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .settings {
    position: static;
  }
}

@media (max-width: 40rem) {
  .setup {
    gap: var(--space-lg);
  }

  h1 {
    font-size: var(--text-xl);
  }

  .materials,
  .settings {
    padding: var(--space-lg) var(--space-md);
  }
}
</style>
