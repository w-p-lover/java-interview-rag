<script setup lang="ts">
import { computed, shallowRef } from "vue";

interface SubmitPayload {
  file: File;
  source: string;
}

const props = defineProps<{
  busy: boolean;
  error: string;
}>();

const emit = defineEmits<{
  submit: [payload: SubmitPayload];
}>();

const selectedFile = shallowRef<File | null>(null);
const source = shallowRef("");
const localError = shallowRef("");
const dragActive = shallowRef(false);

const visibleError = computed(() => localError.value || props.error);

function selectFile(file: File | undefined): void {
  if (!file) {
    return;
  }
  selectedFile.value = file;
  source.value = file.name;
  localError.value = "";
}

function handleFileInput(event: Event): void {
  const input = event.target as HTMLInputElement;
  selectFile(input.files?.[0]);
}

function handleDrop(event: DragEvent): void {
  dragActive.value = false;
  selectFile(event.dataTransfer?.files[0]);
}

function submitImport(): void {
  if (!selectedFile.value) {
    localError.value = "请先选择一份 Markdown 简历或资料。";
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
      <div>
        <span class="panel-kicker">资料准备</span>
        <h2 id="import-title">导入简历或岗位资料</h2>
        <p class="panel-intro">当前先完成原文导入和结构检查，后续会用它辅助面试训练。</p>
      </div>
      <span class="available-state">当前可用</span>
    </header>

    <form class="import-form" @submit.prevent="submitImport">
      <label
        class="drop-zone"
        :class="{ 'is-dragging': dragActive, 'has-file': selectedFile }"
        for="markdown-file"
        @dragenter.prevent="dragActive = true"
        @dragover.prevent="dragActive = true"
        @dragleave.prevent="dragActive = false"
        @drop.prevent="handleDrop"
      >
        <input
          id="markdown-file"
          class="file-input"
          type="file"
          accept=".md,text/markdown,text/plain"
          @change="handleFileInput"
        />
        <span class="file-mark" aria-hidden="true">MD</span>
        <strong>{{ selectedFile ? selectedFile.name : "选择一份 Markdown 文件" }}</strong>
        <span>{{ selectedFile ? formatFileSize(selectedFile.size) : "简历、岗位描述或面试资料" }}</span>
      </label>

      <div class="field-group">
        <label for="document-source">资料来源</label>
        <input
          id="document-source"
          v-model="source"
          name="source"
          type="text"
          autocomplete="off"
          placeholder="例如 resume/java-backend.md"
          :disabled="busy"
        />
        <p>使用稳定名称记录版本，不会保存本机绝对路径。</p>
      </div>

      <div v-if="visibleError" class="error-message" role="alert">
        <span aria-hidden="true">!</span>
        {{ visibleError }}
      </div>

      <button class="submit-button" type="submit" :disabled="busy">
        {{ busy ? "正在检查资料" : "导入并检查" }}
      </button>
    </form>

    <footer class="session-note">
      当前结果只保存在本次会话；候选人画像和模拟面试尚未接入。
    </footer>
  </section>
</template>

<style scoped lang="scss">
.import-panel {
  padding: 1.25rem;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface);
}

.panel-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.2rem;
}

.panel-kicker {
  display: block;
  margin-bottom: 0.25rem;
  color: var(--ink-muted);
  font-size: 0.68rem;
  font-weight: 600;
}

h2 {
  margin: 0;
  color: var(--ink);
  font-size: 1.15rem;
  font-weight: 650;
  letter-spacing: -0.02em;
}

.panel-intro {
  max-width: 34rem;
  margin: 0.5rem 0 0;
  color: var(--ink-muted);
  font-size: 0.8rem;
  line-height: 1.5;
}

.available-state {
  flex: 0 0 auto;
  padding: 0.25rem 0.45rem;
  border: 1px solid var(--success-border);
  border-radius: 0.35rem;
  color: var(--success);
  background: var(--success-soft);
  font-size: 0.68rem;
  font-weight: 600;
}

.import-form {
  display: grid;
  gap: 1rem;
}

.drop-zone {
  display: grid;
  min-height: 10rem;
  padding: 1.25rem;
  place-items: center;
  align-content: center;
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius);
  background: var(--surface-subtle);
  text-align: center;
  transition: border-color 160ms ease, background-color 160ms ease;
}

.drop-zone:hover,
.drop-zone.is-dragging {
  border-color: var(--accent);
  background: var(--accent-soft);
}

.drop-zone.has-file {
  border-style: solid;
}

.drop-zone strong {
  max-width: 100%;
  margin-top: 0.7rem;
  overflow: hidden;
  color: var(--ink);
  font-size: 0.9rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drop-zone > span:last-child {
  margin-top: 0.2rem;
  color: var(--ink-muted);
  font-size: 0.75rem;
}

.file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  clip-path: inset(50%);
  white-space: nowrap;
}

.file-mark {
  display: grid;
  width: 2.75rem;
  height: 2.25rem;
  place-items: center;
  border: 1px solid var(--border-strong);
  border-radius: 0.35rem;
  color: var(--accent);
  background: var(--surface);
  font-family: var(--font-mono);
  font-size: 0.68rem;
  font-weight: 700;
}

.field-group {
  display: grid;
  gap: 0.45rem;
}

.field-group label {
  color: var(--ink);
  font-size: 0.8rem;
  font-weight: 600;
}

.field-group input {
  width: 100%;
  padding: 0.7rem 0.8rem;
  border: 1px solid var(--border-strong);
  border-radius: 0.4rem;
  color: var(--ink);
  background: var(--surface);
  font-family: var(--font-mono);
  font-size: 0.78rem;
}

.field-group input:focus {
  border-color: var(--accent);
}

.field-group p {
  margin: 0;
  color: var(--ink-muted);
  font-size: 0.7rem;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  padding: 0.7rem 0.8rem;
  border: 1px solid var(--danger-border);
  border-radius: 0.4rem;
  color: var(--danger);
  background: var(--danger-soft);
  font-size: 0.78rem;
}

.error-message span {
  display: grid;
  width: 1.1rem;
  height: 1.1rem;
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid currentColor;
  border-radius: 50%;
  font-family: var(--font-mono);
  font-size: 0.65rem;
}

.submit-button {
  min-height: 2.55rem;
  padding: 0.65rem 0.9rem;
  border: 1px solid var(--accent);
  border-radius: 0.4rem;
  color: #fff;
  background: var(--accent);
  font-size: 0.82rem;
  font-weight: 650;
  transition: background-color 160ms ease, border-color 160ms ease;
}

.submit-button:hover:not(:disabled) {
  border-color: var(--accent-strong);
  background: var(--accent-strong);
}

.submit-button:disabled {
  cursor: wait;
  opacity: 0.65;
}

.session-note {
  margin: 1rem 0 0;
  padding-top: 0.9rem;
  border-top: 1px solid var(--border);
  color: var(--ink-muted);
  font-size: 0.7rem;
  line-height: 1.5;
}
</style>
