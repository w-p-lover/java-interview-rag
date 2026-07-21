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
    localError.value = "请先选择一份 Markdown 文件。";
    return;
  }
  if (!source.value.trim()) {
    localError.value = "请填写稳定的逻辑来源。";
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
    <div class="section-heading">
      <span class="section-index">A</span>
      <div>
        <p class="eyebrow">Source intake</p>
        <h2 id="import-title">送入一份 Markdown</h2>
      </div>
    </div>

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
        <strong>{{ selectedFile ? selectedFile.name : "选择或拖入文件" }}</strong>
        <span>
          {{ selectedFile ? formatFileSize(selectedFile.size) : "第一版按 UTF-8 读取单个文件" }}
        </span>
      </label>

      <div class="field-group">
        <label for="document-source">逻辑来源</label>
        <input
          id="document-source"
          v-model="source"
          name="source"
          type="text"
          autocomplete="off"
          placeholder="例如 java/collections.md"
          :disabled="busy"
        />
        <p>用于版本去重和后续来源引用，不使用本机绝对路径。</p>
      </div>

      <div v-if="visibleError" class="error-message" role="alert">
        <span aria-hidden="true">!</span>
        {{ visibleError }}
      </div>

      <button class="submit-button" type="submit" :disabled="busy">
        <span>{{ busy ? "正在解析" : "解析并登记" }}</span>
        <span class="button-arrow" aria-hidden="true">↗</span>
      </button>
    </form>

    <footer class="session-note">
      <span class="pulse" aria-hidden="true"></span>
      当前为内存会话，后端重启后登记状态会清空
    </footer>
  </section>
</template>

<style scoped lang="scss">
.import-panel {
  display: flex;
  flex-direction: column;
  min-height: 34rem;
  padding: clamp(1.35rem, 3vw, 2rem);
  border: 1px solid var(--lead);
  border-radius: var(--radius-large);
  background: color-mix(in srgb, var(--surface) 94%, transparent);
  box-shadow: var(--shadow);
}

.section-heading {
  display: flex;
  align-items: flex-start;
  gap: 0.9rem;
  margin-bottom: 1.7rem;
}

.section-index {
  display: grid;
  width: 2.35rem;
  height: 2.35rem;
  place-items: center;
  border: 1px solid var(--jade);
  border-radius: 50%;
  color: var(--jade);
  font-family: var(--font-mono);
  font-size: 0.78rem;
}

.eyebrow {
  margin: 0 0 0.2rem;
  color: var(--cinnabar);
  font-family: var(--font-mono);
  font-size: 0.7rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.55rem;
  font-weight: 700;
}

.import-form {
  display: grid;
  gap: 1.3rem;
}

.drop-zone {
  display: grid;
  min-height: 11.5rem;
  padding: 1.5rem;
  place-items: center;
  align-content: center;
  border: 1px dashed var(--lead-dark);
  border-radius: var(--radius-medium);
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--jade-soft) 58%, transparent), transparent),
    var(--surface-strong);
  text-align: center;
  transition: border-color 180ms ease, transform 180ms ease, background 180ms ease;
}

.drop-zone:hover,
.drop-zone.is-dragging {
  border-color: var(--jade);
  transform: translateY(-2px);
}

.drop-zone.has-file {
  border-style: solid;
}

.drop-zone strong {
  margin-top: 0.7rem;
  font-size: 1rem;
}

.drop-zone > span:last-child {
  margin-top: 0.2rem;
  color: var(--ink-soft);
  font-size: 0.78rem;
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
  width: 3.2rem;
  height: 3.7rem;
  place-items: center;
  border: 1px solid var(--jade);
  border-radius: 0.45rem 0.85rem 0.45rem 0.45rem;
  color: var(--jade);
  background: var(--jade-soft);
  font-family: var(--font-mono);
  font-size: 0.78rem;
  font-weight: 700;
}

.field-group {
  display: grid;
  gap: 0.5rem;
}

.field-group label {
  font-size: 0.82rem;
  font-weight: 700;
}

.field-group input {
  width: 100%;
  padding: 0.85rem 1rem;
  border: 1px solid var(--lead);
  border-radius: 0.75rem;
  color: var(--ink);
  background: var(--surface-strong);
  font-family: var(--font-mono);
}

.field-group input:focus {
  border-color: var(--jade);
}

.field-group p {
  margin: 0;
  color: var(--ink-soft);
  font-size: 0.72rem;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  padding: 0.75rem 0.85rem;
  border: 1px solid color-mix(in srgb, var(--cinnabar) 35%, transparent);
  border-radius: 0.7rem;
  color: var(--cinnabar);
  background: var(--cinnabar-soft);
  font-size: 0.82rem;
}

.error-message span {
  display: grid;
  width: 1.25rem;
  height: 1.25rem;
  place-items: center;
  border: 1px solid currentColor;
  border-radius: 50%;
  font-family: var(--font-mono);
}

.submit-button {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 0.9rem 1rem 0.9rem 1.2rem;
  border: 0;
  border-radius: 0.8rem;
  color: #fff;
  background: var(--ink);
  font-weight: 700;
  transition: transform 180ms ease, background 180ms ease;
}

.submit-button:hover:not(:disabled) {
  transform: translateY(-1px);
  background: var(--jade);
}

.submit-button:disabled {
  cursor: wait;
  opacity: 0.65;
}

.button-arrow {
  font-size: 1.2rem;
}

.session-note {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  margin-top: auto;
  padding-top: 1.4rem;
  color: var(--ink-soft);
  font-size: 0.72rem;
}

.pulse {
  width: 0.45rem;
  height: 0.45rem;
  border-radius: 50%;
  background: var(--cinnabar);
  box-shadow: 0 0 0 0.25rem var(--cinnabar-soft);
}
</style>
