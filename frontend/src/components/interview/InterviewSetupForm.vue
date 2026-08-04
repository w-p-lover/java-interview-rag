<script setup lang="ts">
import { BriefcaseBusiness, CheckCircle2, Circle, Play, SlidersHorizontal } from "@lucide/vue";
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
const readyRequirementCount = computed(() => [form.targetRole, form.resumeText]
  .filter((value) => value.trim()).length);
const readinessPercent = computed(() => readyRequirementCount.value * 50);
const readinessLabel = computed(() => canStart.value ? "可以开始" : "还差简历");
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
      <div class="heading-copy">
        <h1>开始一场模拟面试</h1>
        <p>上传简历，选择目标岗位。题目会根据你的经历和岗位要求生成。</p>
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
    </header>

    <div class="setup-layout">
      <section class="materials" aria-labelledby="materials-title">
        <div class="section-heading">
          <BriefcaseBusiness :size="18" aria-hidden="true" />
          <h2 id="materials-title">面试材料</h2>
        </div>

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

        <div class="readiness" aria-live="polite">
          <div class="readiness-copy">
            <span>准备状态</span>
            <strong>{{ readinessLabel }}</strong>
          </div>
          <div
            class="readiness-track"
            role="progressbar"
            aria-label="必要信息准备进度"
            aria-valuemin="0"
            aria-valuemax="2"
            :aria-valuenow="readyRequirementCount"
          >
            <span :style="{ width: `${readinessPercent}%` }"></span>
          </div>
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
            <dt>目标岗位</dt>
            <dd :class="{ ready: form.targetRole.trim() }">
              <CheckCircle2 v-if="form.targetRole.trim()" :size="15" aria-hidden="true" />
              <Circle v-else :size="15" aria-hidden="true" />
              {{ form.targetRole.trim() ? "已填写" : "待填写" }}
            </dd>
          </div>
          <div>
            <dt>简历</dt>
            <dd :class="{ ready: form.resumeText.trim() }">
              <CheckCircle2 v-if="form.resumeText.trim()" :size="15" aria-hidden="true" />
              <Circle v-else :size="15" aria-hidden="true" />
              {{ form.resumeText.trim() ? "已准备" : "待上传" }}
            </dd>
          </div>
          <div>
            <dt>岗位描述</dt>
            <dd :class="{ ready: form.jobDescription.trim() }">
              <CheckCircle2 v-if="form.jobDescription.trim()" :size="15" aria-hidden="true" />
              <Circle v-else :size="15" aria-hidden="true" />
              {{ form.jobDescription.trim() ? "已准备" : "可选" }}
            </dd>
          </div>
        </dl>

        <button type="submit" :class="{ ready: canStart }" :disabled="busy || !canStart">
          <Play :size="17" aria-hidden="true" />
          <span>{{ busy ? "正在提交…" : "开始面试" }}</span>
        </button>

        <p class="settings-note">
          {{ canStart ? "材料已就绪，可以开始。" : "上传简历后即可开始。" }}
        </p>
      </aside>
    </div>
  </form>
</template>

<style scoped lang="scss">
.setup {
  display: grid;
  gap: var(--space-2xl);
}

.setup-heading {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(21rem, 0.72fr);
  align-items: end;
  gap: var(--space-3xl);
  padding-block: var(--space-md) var(--space-xl);
  border-bottom: var(--rule-thin) solid var(--color-rule-2);
}

.heading-copy {
  display: grid;
  gap: var(--space-sm);
}

h1,
h2,
p {
  margin: 0;
}

h1 {
  color: var(--color-ink);
  font-size: var(--text-3xl);
  letter-spacing: -0.035em;
  line-height: 1.08;
  text-wrap: balance;
}

.setup-heading p {
  max-width: 44rem;
  color: var(--color-muted);
  font-size: var(--text-base);
}

.setup-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(19rem, 0.58fr);
  align-items: start;
  gap: var(--space-2xl);
}

.materials,
.settings {
  display: grid;
  gap: var(--space-lg);
  min-width: 0;
}

.materials {
  padding-block: var(--space-xs);
}

.section-heading,
.settings-heading {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}

.section-heading {
  color: var(--color-accent);
  padding-bottom: var(--space-sm);
  border-bottom: var(--rule-thin) solid var(--color-rule);
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

.role-field {
  padding: var(--space-sm) var(--space-md);
  border: var(--rule-thin) solid var(--color-rule);
  border-radius: var(--radius-control);
  background: var(--color-surface);
}

.role-field input {
  min-height: 2.25rem;
  padding: 0;
  border: 0;
  border-radius: 0;
  background: transparent;
  font-size: var(--text-md);
  font-weight: 700;
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
  padding: var(--space-xl);
  border-radius: var(--radius-surface);
  color: var(--color-graphite-text);
  background: var(--color-graphite);
  box-shadow: var(--shadow-whisper);
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

.readiness {
  display: grid;
  gap: var(--space-sm);
  padding-block: var(--space-sm) var(--space-md);
}

.readiness-copy {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
}

.readiness-copy span {
  color: var(--color-graphite-muted);
  font-size: var(--text-xs);
}

.readiness-copy strong {
  color: var(--color-graphite-text);
  font-size: var(--text-sm);
}

.readiness-track {
  height: var(--space-xs);
  overflow: hidden;
  border-radius: var(--radius-round);
  background: var(--color-graphite-2);
}

.readiness-track span {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--color-accent);
  transition: width var(--dur-long) var(--ease-out);
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
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  color: var(--color-graphite-muted);
  font-weight: 700;
}

.material-state dd.ready {
  color: var(--color-graphite-text);
}

.material-state dd.ready svg {
  color: var(--color-success-rule);
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
  transition: background-color var(--dur-short) var(--ease-out), transform var(--dur-micro) var(--ease-out), box-shadow var(--dur-short) var(--ease-out);
}

button.ready {
  box-shadow: 0 2px 6px oklch(16% 0.012 255 / 0.24);
}

.settings-note {
  text-align: center;
  font-size: var(--text-xs);
}

@media (max-width: 62rem) {
  .setup-heading,
  .setup-layout {
    grid-template-columns: minmax(0, 1fr);
  }

  .setup-heading {
    gap: var(--space-xl);
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
    font-size: var(--text-2xl);
  }

  .settings {
    padding: var(--space-lg) var(--space-md);
  }
}

@media (hover: hover) and (pointer: fine) {
  button.ready:hover {
    background: var(--color-accent-strong);
    transform: translateY(-1px);
  }
}
</style>
