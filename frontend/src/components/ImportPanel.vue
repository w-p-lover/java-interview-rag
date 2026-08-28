<script setup lang="ts">
import { computed, shallowRef } from "vue";

interface SubmitPayload {
  file: File;
  source: string;
}

type ImportState = "idle" | "loading" | "success" | "duplicate" | "error";

const props = defineProps<{
  busy: boolean;
  error: string;
  status: ImportState;
}>();

const emit = defineEmits<{
  reset: [];
  submit: [payload: SubmitPayload];
}>();

const selectedFile = shallowRef<File | null>(null);
const source = shallowRef("");
const localError = shallowRef("");
const dragActive = shallowRef(false);

const visibleError = computed(() => localError.value || props.error);
const submitLabel = computed(() => {
  if (props.busy) {
    return "正在检查资料";
  }
  if (props.status === "success") {
    return "资料检查完成";
  }
  if (props.status === "duplicate") {
    return "版本已经登记";
  }
  if (props.status === "error") {
    return "修正后重新导入";
  }
  return "导入并检查";
});

function resetParentState(): void {
  if (props.status !== "idle") {
    emit("reset");
  }
}

function selectFile(file: File | undefined): void {
  if (!file || props.busy) {
    return;
  }
  selectedFile.value = file;
  source.value = file.name;
  localError.value = "";
  resetParentState();
}

function setDragActive(active: boolean): void {
  if (!props.busy) {
    dragActive.value = active;
  }
}

function handleFileInput(event: Event): void {
  const input = event.target as HTMLInputElement;
  selectFile(input.files?.[0]);
}

function handleSourceInput(): void {
  localError.value = "";
  resetParentState();
}

function handleDrop(event: DragEvent): void {
  dragActive.value = false;
  selectFile(event.dataTransfer?.files[0]);
}

function submitImport(): void {
  if (!selectedFile.value) {
    localError.value = "请先选择一份 Markdown 或 PDF 知识资料。";
    return;
  }
  if (!source.value.trim()) {
    localError.value = "请填写这份资料的稳定来源。";
    return;
  }
  localError.value = "";
  emit("submit", {
    file: selectedFile.value,
    source: source.value.trim(),
  });
}

function formatFileSize(size: number): string {
  return size < 1024 ? `${size} B` : `${(size / 1024).toFixed(1)} KB`;
}
</script>

<template>
  <section class="import-panel" aria-labelledby="import-title">
    <header class="panel-heading">
      <h3 id="import-title">导入知识资料</h3>
    </header>

    <form class="import-form" @submit.prevent="submitImport">
      <label
        class="drop-zone"
        :class="{
          'is-dragging': dragActive,
          'has-file': selectedFile,
          'is-disabled': busy,
          'is-error': visibleError,
        }"
        for="markdown-file"
        @dragenter.prevent="setDragActive(true)"
        @dragover.prevent="setDragActive(true)"
        @dragleave.prevent="setDragActive(false)"
        @drop.prevent="handleDrop"
      >
        <input
          id="markdown-file"
          class="file-input"
          type="file"
          accept=".md,.markdown,.pdf,text/markdown,text/plain,application/pdf"
          :disabled="busy"
          @change="handleFileInput"
        />
        <span class="file-mark" aria-hidden="true">{{ selectedFile?.name.toLowerCase().endsWith('.pdf') ? 'PDF' : 'MD' }}</span>
        <strong>{{ selectedFile ? selectedFile.name : "选择 Markdown 文件" }}</strong>
        <span>{{ selectedFile ? formatFileSize(selectedFile.size) : "支持 Markdown 与文本型 PDF" }}</span>
      </label>

      <div class="field-group">
        <label for="document-source">资料来源</label>
        <input
          id="document-source"
          v-model="source"
          name="source"
          type="text"
          autocomplete="off"
          placeholder="例如 java/concurrency/thread-pool.md 或 java/concurrency.pdf"
          :disabled="busy"
          :aria-invalid="Boolean(visibleError)"
          aria-describedby="source-helper"
          @input="handleSourceInput"
        />
        <p id="source-helper">同一来源用于识别后续版本。</p>
      </div>

      <div v-if="visibleError" class="error-message" role="alert">
        <span aria-hidden="true">×</span>
        {{ visibleError }}
      </div>

      <button
        class="submit-button"
        :class="{
          'is-success': status === 'success' || status === 'duplicate',
          'is-error': status === 'error',
        }"
        type="submit"
        :disabled="busy"
        :aria-busy="busy"
      >
        <span v-if="busy" class="loading-mark" aria-hidden="true"></span>
        {{ submitLabel }}
      </button>
    </form>

  </section>
</template>

<style scoped lang="scss">
.import-panel {
  padding: 0;
  background: transparent;
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}

h3 {
  margin: 0;
  color: var(--color-ink);
  font-family: var(--font-display);
  font-size: var(--text-md);
  font-style: normal;
  font-weight: 700;
  letter-spacing: 0;
}

.import-form,
.field-group {
  display: grid;
  gap: var(--space-md);
}

.drop-zone {
  display: grid;
  min-height: 11rem;
  padding: var(--space-lg);
  place-items: center;
  align-content: center;
  border-radius: var(--radius-control);
  outline: var(--rule-focus) solid transparent;
  outline-offset: var(--space-3xs);
  color: var(--color-ink);
  background: var(--color-paper-2);
  text-align: center;
  transition: border-color var(--dur-short) var(--ease-out), background-color var(--dur-short) var(--ease-out);
}

.drop-zone.is-dragging {
  border-color: var(--color-accent);
  background: var(--color-accent-soft);
}

.drop-zone.has-file {
  border-style: solid;
}

.drop-zone:focus-within {
  border-color: var(--color-accent);
  outline-color: var(--color-focus);
}

.drop-zone.is-disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.drop-zone.is-error {
  border-color: var(--color-error);
}

.drop-zone strong {
  max-width: 100%;
  margin-top: var(--space-sm);
  overflow: hidden;
  color: var(--color-ink);
  font-size: var(--text-sm);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drop-zone > span:last-child {
  margin-top: var(--space-2xs);
  color: var(--color-muted);
  font-size: var(--text-xs);
}

.file-input {
  position: absolute;
  width: var(--rule-thin);
  height: var(--rule-thin);
  overflow: hidden;
  clip-path: inset(50%);
  white-space: nowrap;
}

.file-mark {
  display: grid;
  width: 3rem;
  height: 2.5rem;
  place-items: center;
  border-radius: var(--radius-control);
  color: var(--color-accent);
  background: var(--color-surface);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  font-weight: 700;
}

.field-group {
  gap: var(--space-xs);
}

.field-group label {
  color: var(--color-ink);
  font-size: var(--text-sm);
  font-weight: 600;
}

.field-group input {
  width: 100%;
  min-height: 2.75rem;
  padding-inline: var(--space-sm);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  outline: var(--rule-focus) solid transparent;
  outline-offset: var(--space-3xs);
  color: var(--color-ink);
  background: var(--color-surface);
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  transition: border-color var(--dur-short) var(--ease-out), background-color var(--dur-short) var(--ease-out);
}

.field-group input:focus-visible {
  border-color: var(--color-ink-2);
  outline-color: var(--color-focus);
}

.field-group input:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.field-group input[aria-invalid="true"] {
  border-color: var(--color-error);
}

.field-group p {
  min-height: 1lh;
  margin: 0;
  color: var(--color-muted);
  font-size: var(--text-xs);
}

.error-message {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  padding: var(--space-sm);
  border: var(--rule-thin) solid var(--color-error-rule);
  border-radius: var(--radius-control);
  color: var(--color-error);
  background: var(--color-error-soft);
  font-size: var(--text-sm);
}

.error-message span {
  display: grid;
  width: var(--space-md);
  height: var(--space-md);
  flex: 0 0 auto;
  place-items: center;
  border: var(--rule-thin) solid currentColor;
  border-radius: var(--radius-round);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.submit-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-xs);
  min-height: 2.75rem;
  padding: var(--space-sm) var(--space-md);
  border: var(--rule-thin) solid var(--color-accent);
  border-radius: var(--radius-control);
  color: var(--color-accent-ink);
  background: var(--color-accent);
  font-size: var(--text-sm);
  font-weight: 600;
  white-space: nowrap;
  transition: background-color var(--dur-short) var(--ease-out), border-color var(--dur-short) var(--ease-out), transform var(--dur-micro) var(--ease-out);
}

.submit-button:active:not(:disabled) {
  transform: translateY(var(--rule-thin));
}

.submit-button:disabled {
  cursor: wait;
  opacity: 0.6;
}

.submit-button.is-success {
  border-color: var(--color-success);
  color: var(--color-success);
  background: var(--color-success-soft);
}

.submit-button.is-error {
  border-color: var(--color-error);
  color: var(--color-error);
  background: var(--color-error-soft);
}

.loading-mark {
  width: var(--space-sm);
  height: var(--space-sm);
  border: var(--rule-focus) solid var(--color-accent-soft);
  border-top-color: var(--color-accent-ink);
  border-radius: var(--radius-round);
  animation: loading-spin 1s linear infinite;
}

@keyframes loading-spin {
  to { transform: rotate(1turn); }
}

@media (hover: hover) and (pointer: fine) {
  .drop-zone:hover:not(.is-disabled) {
    background: var(--color-accent-soft);
  }

  .field-group input:hover:not(:disabled) {
    background: var(--color-paper-2);
  }

  .submit-button:hover:not(:disabled) {
    background: var(--color-accent-strong);
  }
}

@media (max-width: 39.99rem) {
  .import-panel {
    padding: 0;
  }

  .panel-heading {
    display: grid;
  }

  .drop-zone {
    min-height: 9rem;
  }
}

@media (prefers-reduced-motion: reduce) {
  .loading-mark {
    animation-duration: 1.5s !important;
  }
}
</style>
