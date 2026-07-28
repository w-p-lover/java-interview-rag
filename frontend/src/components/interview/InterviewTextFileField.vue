<script setup lang="ts">
import { computed, shallowRef } from "vue";

import { extractInterviewDocumentText } from "@/utils/interviewDocumentText";

const props = defineProps<{
  inputId: string;
  label: string;
  optional?: boolean;
  rows: number;
  maxLength: number;
  placeholder: string;
  busy: boolean;
}>();

const model = defineModel<string>({ required: true });

type ReadState = "idle" | "reading" | "success" | "error";

const readState = shallowRef<ReadState>("idle");
const fileName = shallowRef("");
const extractedCharacters = shallowRef(0);
const extractedPageCount = shallowRef<number | null>(null);
const localError = shallowRef("");

const fileInputId = computed(() => `${props.inputId}-file`);
const textInputId = computed(() => `${props.inputId}-text`);
const statusId = computed(() => `${props.inputId}-status`);
const inputDisabled = computed(() => props.busy || readState.value === "reading");
const extractionSummary = computed(() => extractedPageCount.value
  ? `PDF 共 ${extractedPageCount.value} 页，已提取 ${extractedCharacters.value} 个字符，可在下方检查和编辑`
  : `已提取 ${extractedCharacters.value} 个字符，可在下方检查和编辑`);

function setError(message: string): void {
  readState.value = "error";
  extractedCharacters.value = 0;
  extractedPageCount.value = null;
  localError.value = message;
}

async function handleFileInput(event: Event): Promise<void> {
  const input = event.target as HTMLInputElement;
  const file = input.files?.[0];
  input.value = "";

  if (!file || inputDisabled.value) {
    return;
  }

  fileName.value = file.name;
  localError.value = "";

  readState.value = "reading";
  try {
    const extraction = await extractInterviewDocumentText(file);
    const content = extraction.content;
    if (!content.trim()) {
      setError("文件中没有可用正文，请选择其他文件或直接粘贴文本。");
      return;
    }
    if (content.length > props.maxLength) {
      setError(`正文共 ${content.length} 个字符，超过 ${props.maxLength} 个字符上限。`);
      return;
    }

    model.value = content;
    extractedCharacters.value = content.length;
    extractedPageCount.value = extraction.pageCount ?? null;
    readState.value = "success";
  } catch (error) {
    setError(error instanceof Error
      ? error.message
      : "文件读取失败，请重新选择或直接粘贴文本。");
  }
}
</script>

<template>
  <fieldset class="text-file-field" :disabled="busy">
    <legend>
      {{ label }}
      <small v-if="optional">可选</small>
    </legend>

    <label
      class="file-selector"
      :class="{
        'is-reading': readState === 'reading',
        'is-success': readState === 'success',
        'is-error': readState === 'error',
      }"
      :for="fileInputId"
    >
      <input
        :id="fileInputId"
        class="file-input"
        type="file"
        accept=".pdf,.md,.markdown,.txt,application/pdf,text/markdown,text/plain"
        :disabled="inputDisabled"
        :aria-describedby="statusId"
        @change="handleFileInput"
      />
      <span class="file-mark" aria-hidden="true">PDF / TEXT</span>
      <span class="file-copy">
        <strong>{{ fileName || `选择${label}文件` }}</strong>
        <small v-if="readState === 'reading'">正在提取正文…</small>
        <small v-else-if="readState === 'success'">{{ extractionSummary }}</small>
        <small v-else>PDF 仅在本次请求中发送到后端提取，不会进入资料库或保存</small>
      </span>
      <span class="select-command">{{ fileName ? "重新选择" : "选择文件" }}</span>
    </label>

    <p :id="statusId" class="file-status" :class="{ 'is-error': localError }" aria-live="polite">
      {{ localError || "支持带文本层的 PDF、Markdown、TXT；扫描件 OCR 和 DOCX 暂未开放。" }}
    </p>

    <label class="text-fallback" :for="textInputId">
      <span>备用文本输入</span>
      <textarea
        :id="textInputId"
        v-model="model"
        :rows="rows"
        :maxlength="maxLength"
        :required="!optional"
        :placeholder="placeholder"
      ></textarea>
    </label>
  </fieldset>
</template>

<style scoped lang="scss">
.text-file-field {
  display: grid;
  gap: var(--space-xs);
  min-width: 0;
  margin: 0;
  padding: 0;
  border: 0;
}

legend,
.text-fallback > span {
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  font-weight: 600;
}

legend {
  margin-bottom: var(--space-xs);
}

legend small {
  margin-left: var(--space-xs);
  color: var(--color-muted);
  font-weight: 400;
}

.file-selector {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-md);
  min-height: 5.5rem;
  padding: var(--space-md);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  background: var(--color-paper);
  cursor: pointer;
  transition: border-color var(--dur-short) var(--ease-out), background-color var(--dur-short) var(--ease-out);
}

.file-selector.is-success {
  border-color: var(--color-success);
  background: var(--color-success-soft);
}

.file-selector.is-error {
  border-color: var(--color-error-rule);
  background: var(--color-error-soft);
}

.file-selector.is-reading,
.text-file-field:disabled .file-selector {
  cursor: wait;
  opacity: 0.65;
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
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 4.25rem;
  min-height: 2.5rem;
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-accent);
  background: var(--color-paper-2);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  font-weight: 700;
}

.file-copy {
  display: grid;
  min-width: 0;
  gap: var(--space-2xs);
}

.file-copy strong,
.file-copy small {
  overflow-wrap: anywhere;
}

.file-copy strong {
  color: var(--color-ink);
  font-size: var(--text-sm);
}

.file-copy small,
.file-status {
  color: var(--color-muted);
  font-size: var(--text-xs);
  font-weight: 400;
  line-height: 1.5;
}

.select-command {
  color: var(--color-accent);
  font-size: var(--text-xs);
  font-weight: 700;
  white-space: nowrap;
}

.file-status {
  min-height: 1.5em;
  margin: 0;
}

.file-status.is-error {
  color: var(--color-error);
}

.text-fallback {
  display: grid;
  gap: var(--space-xs);
  margin-top: var(--space-xs);
}

textarea {
  width: 100%;
  min-height: 8rem;
  padding: var(--space-md);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-ink);
  background: var(--color-paper);
  resize: vertical;
  line-height: 1.65;
}

@media (hover: hover) and (pointer: fine) {
  .file-selector:hover:not(.is-reading) {
    border-color: var(--color-accent);
    background: var(--color-accent-soft);
  }
}

@media (max-width: 40rem) {
  .file-selector {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .select-command {
    grid-column: 2;
  }
}
</style>
