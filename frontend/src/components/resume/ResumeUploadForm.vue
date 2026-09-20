<script setup lang="ts">
import { computed, reactive, shallowRef, watch } from "vue";
import { FilePlus2, Upload } from "@lucide/vue";

import type { ResumeSummary, ResumeUploadPayload } from "@/api/resumeCenter";

const props = defineProps<{
  selectedRecord: ResumeSummary | null;
  busy: boolean;
}>();
const emit = defineEmits<{ submit: [payload: ResumeUploadPayload] }>();
const mode = defineModel<"new" | "append">("mode", { required: true });
const form = reactive({ name: "", targetRole: "", saveFullText: false });
const file = shallowRef<File | null>(null);
const fileInput = shallowRef<HTMLInputElement | null>(null);
const valid = computed(() => Boolean(
  file.value && form.name.trim() && form.targetRole.trim()
    && (mode.value === "new" || props.selectedRecord),
));

watch(() => props.selectedRecord, (record) => {
  if (mode.value === "append" && record) {
    form.name = record.name;
    form.targetRole = record.targetRole;
  }
}, { immediate: true });
watch(mode, (next) => {
  if (next === "append" && props.selectedRecord) {
    form.name = props.selectedRecord.name;
    form.targetRole = props.selectedRecord.targetRole;
  }
});

function chooseFile(event: Event): void {
  file.value = (event.target as HTMLInputElement).files?.[0] ?? null;
}

function submit(): void {
  if (!valid.value || !file.value || props.busy) return;
  emit("submit", {
    file: file.value,
    resumeId: mode.value === "append" ? props.selectedRecord?.id : undefined,
    name: form.name.trim(),
    targetRole: form.targetRole.trim(),
    saveFullText: form.saveFullText,
  });
}

function clearFile(): void {
  file.value = null;
  if (fileInput.value) fileInput.value.value = "";
}

defineExpose({ clearFile });
</script>

<template>
  <section id="resume-upload" class="upload-section" aria-labelledby="resume-upload-title" tabindex="-1">
    <header class="section-heading">
      <div>
        <h2 id="resume-upload-title">保存简历版本</h2>
        <p>{{ mode === "append" && selectedRecord ? selectedRecord.name : "新建简历记录" }}</p>
      </div>
      <div class="mode-switch" aria-label="保存方式">
        <button type="button" :class="{ active: mode === 'new' }" :disabled="busy" @click="mode = 'new'">新建</button>
        <button type="button" :class="{ active: mode === 'append' }" :disabled="busy || !selectedRecord" @click="mode = 'append'">追加版本</button>
      </div>
    </header>

    <form class="upload-form" @submit.prevent="submit">
      <div class="field">
        <label for="resume-name">简历名称</label>
        <input id="resume-name" v-model="form.name" maxlength="200" required :disabled="busy" />
      </div>
      <div class="field">
        <label for="resume-target-role">目标岗位</label>
        <input id="resume-target-role" v-model="form.targetRole" maxlength="200" required :disabled="busy" />
      </div>
      <label class="file-picker" :class="{ selected: file }">
        <FilePlus2 :size="19" aria-hidden="true" />
        <span>{{ file?.name ?? "选择 Markdown、TXT 或 PDF" }}</span>
        <input ref="fileInput" type="file" accept=".md,.markdown,.txt,.pdf" :disabled="busy" @change="chooseFile" />
      </label>
      <label class="privacy-toggle">
        <input v-model="form.saveFullText" type="checkbox" :disabled="busy" />
        <span><strong>保存完整正文</strong><small>未勾选时仅保存版本与文件元数据</small></span>
      </label>
      <button class="primary-action" type="submit" :disabled="busy || !valid">
        <Upload :size="17" aria-hidden="true" />{{ busy ? "正在保存…" : "保存版本" }}
      </button>
    </form>
  </section>
</template>

<style scoped lang="scss">
.upload-section { padding-block: var(--space-lg); border-bottom: var(--rule-thin) solid var(--color-rule-2); }
.section-heading, .mode-switch, .upload-form, .file-picker, .privacy-toggle, .primary-action { display: flex; align-items: center; }
.section-heading { justify-content: space-between; gap: var(--space-lg); }
.section-heading h2, .section-heading p { margin: 0; }
.section-heading h2 { font-size: var(--text-lg); }
.section-heading p { margin-top: var(--space-2xs); color: var(--color-muted); font-size: var(--text-sm); }
.mode-switch { gap: var(--space-2xs); padding: var(--space-2xs); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); background: var(--color-paper-2); }
.mode-switch button { min-height: 2.25rem; padding-inline: var(--space-sm); border: 0; border-radius: calc(var(--radius-control) - 2px); color: var(--color-muted); background: transparent; font-size: var(--text-xs); font-weight: 700; }
.mode-switch button.active { color: var(--color-ink); background: var(--color-surface); box-shadow: 0 1px 2px color-mix(in oklch, var(--color-ink) 12%, transparent); }
.upload-form { align-items: end; gap: var(--space-md); margin-top: var(--space-lg); }
.field { display: grid; flex: 1 1 12rem; gap: var(--space-xs); min-width: 0; }
.field label { color: var(--color-ink-2); font-size: var(--text-xs); font-weight: 650; }
.field input { width: 100%; height: 2.75rem; padding-inline: var(--space-sm); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); color: var(--color-ink); background: var(--color-surface); }
.file-picker { flex: 1.2 1 15rem; justify-content: center; gap: var(--space-xs); min-height: 2.75rem; padding-inline: var(--space-sm); border: var(--rule-thin) dashed var(--color-rule-2); border-radius: var(--radius-control); color: var(--color-muted); cursor: pointer; overflow: hidden; }
.file-picker.selected { border-style: solid; color: var(--color-ink-2); }
.file-picker span { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.file-picker input { position: absolute; width: 1px; height: 1px; overflow: hidden; clip-path: inset(50%); }
.privacy-toggle { gap: var(--space-xs); min-height: 2.75rem; color: var(--color-ink-2); }
.privacy-toggle span { display: grid; gap: var(--space-3xs); }
.privacy-toggle strong { font-size: var(--text-xs); }
.privacy-toggle small { color: var(--color-muted); font-size: var(--text-xs); white-space: nowrap; }
.primary-action { justify-content: center; gap: var(--space-xs); min-height: 2.75rem; padding-inline: var(--space-md); border: var(--rule-thin) solid var(--color-accent); border-radius: var(--radius-control); color: var(--color-accent-ink); background: var(--color-accent); font-weight: 700; white-space: nowrap; }
@media (max-width: 68rem) { .upload-form { flex-wrap: wrap; } .privacy-toggle { flex: 1 1 auto; } }
@media (max-width: 40rem) { .section-heading { align-items: stretch; flex-direction: column; } .mode-switch button { flex: 1; } .upload-form { align-items: stretch; flex-direction: column; } .field, .file-picker, .privacy-toggle, .primary-action { flex: none; width: 100%; } .privacy-toggle small { white-space: normal; } }
</style>
