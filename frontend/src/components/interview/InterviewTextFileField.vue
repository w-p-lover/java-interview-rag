<script setup lang="ts">
import { AlertTriangle, CheckCircle2, ChevronDown, FileText, Upload } from "@lucide/vue";
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
const reorderedPageCount = shallowRef(0);
const extractionWarnings = shallowRef<string[]>([]);
const localError = shallowRef("");

const fileInputId = computed(() => `${props.inputId}-file`);
const textInputId = computed(() => `${props.inputId}-text`);
const statusId = computed(() => `${props.inputId}-status`);
const inputDisabled = computed(() => props.busy || readState.value === "reading");
const extractionSummary = computed(() => extractedPageCount.value
  ? [
      `${extractedPageCount.value} 页`,
      `${extractedCharacters.value} 字`,
      reorderedPageCount.value > 0 ? `${reorderedPageCount.value} 页双栏已调整` : "",
    ].filter(Boolean).join(" · ")
  : `${extractedCharacters.value} 字`);
const extractionWarning = computed(() => extractionWarnings.value.join("；"));

function setError(message: string): void {
  readState.value = "error";
  extractedCharacters.value = 0;
  extractedPageCount.value = null;
  reorderedPageCount.value = 0;
  extractionWarnings.value = [];
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
  extractionWarnings.value = [];

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
    reorderedPageCount.value = extraction.reorderedPageCount ?? 0;
    extractionWarnings.value = extraction.warnings ?? [];
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
      <span class="file-mark" aria-hidden="true">
        <CheckCircle2 v-if="readState === 'success'" :size="19" />
        <FileText v-else :size="19" />
      </span>
      <span class="file-copy">
        <strong>{{ fileName || `选择${label}文件` }}</strong>
        <small v-if="readState === 'reading'">正在读取…</small>
        <small v-else-if="readState === 'success'">
          <CheckCircle2 :size="13" aria-hidden="true" />
          已读取 · {{ extractionSummary }}
        </small>
        <small v-else>PDF、Markdown 或 TXT</small>
      </span>
      <span class="select-command">
        <Upload :size="15" aria-hidden="true" />
        {{ fileName ? "更换" : "选择" }}
      </span>
    </label>

    <p
      :id="statusId"
      class="file-status"
      :class="{ 'is-error': localError, 'is-warning': !localError && extractionWarning }"
      aria-live="polite"
    >
      <AlertTriangle
        v-if="!localError && extractionWarning"
        :size="14"
        aria-hidden="true"
      />
      <span>{{ localError || extractionWarning || "文件只用于本场面试；扫描版 PDF 和 DOCX 暂不支持。" }}</span>
    </p>

    <details class="text-fallback">
      <summary>
        <FileText :size="15" aria-hidden="true" />
        <span>{{ model.trim() ? "查看或修改文本" : "改为粘贴文本" }}</span>
        <ChevronDown class="summary-chevron" :size="15" aria-hidden="true" />
      </summary>
      <label :for="textInputId">{{ label }}文本</label>
      <textarea
        :id="textInputId"
        v-model="model"
        :rows="rows"
        :maxlength="maxLength"
        :placeholder="placeholder"
      ></textarea>
    </details>
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
  padding: var(--space-lg);
  overflow: hidden;
  border-radius: var(--radius-control);
  background: var(--color-paper-2);
  box-shadow: inset 0 0 0 var(--rule-thin) var(--color-rule);
  cursor: pointer;
  transition: border-color var(--dur-short) var(--ease-out), background-color var(--dur-short) var(--ease-out), transform var(--dur-short) var(--ease-out);
}

.file-selector.is-success {
  background: var(--color-success-soft);
  box-shadow: inset 0 0 0 var(--rule-thin) var(--color-success-rule);
}

.file-selector.is-error {
  background: var(--color-error-soft);
  box-shadow: inset 0 0 0 var(--rule-thin) var(--color-error-rule);
}

.file-selector.is-reading,
.text-file-field:disabled .file-selector {
  cursor: wait;
  opacity: 0.65;
}

.file-selector:has(.file-input:focus-visible) {
  outline: var(--rule-focus) solid var(--color-focus);
  outline-offset: var(--rule-thin);
}

.file-selector:active:not(.is-reading) {
  background: var(--color-paper-3);
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
  place-items: center;
  width: 2.5rem;
  height: 2.5rem;
  border-radius: var(--radius-control);
  color: var(--color-accent);
  background: var(--color-accent-soft);
  transition: color var(--dur-short) var(--ease-out), background-color var(--dur-short) var(--ease-out), transform var(--dur-short) var(--ease-out);
}

.file-selector.is-success .file-mark {
  color: var(--color-success);
  background: var(--color-success-soft);
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

.file-copy small,
.select-command {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2xs);
}

.file-copy small svg {
  color: var(--color-success);
}

.select-command {
  color: var(--color-accent);
  font-size: var(--text-xs);
  font-weight: 700;
  white-space: nowrap;
}

.file-status {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2xs);
  min-height: 1.5em;
  margin: 0;
}

.file-status.is-error {
  color: var(--color-error);
}

.file-status.is-warning {
  color: var(--color-warning, #7a4b08);
}

.file-status.is-warning svg {
  flex: 0 0 auto;
  margin-top: 0.15em;
}

.text-fallback {
  display: grid;
  gap: var(--space-xs);
  margin-top: var(--space-2xs);
}

.text-fallback summary {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-xs);
  min-height: 2.75rem;
  color: var(--color-muted);
  cursor: pointer;
  font-size: var(--text-sm);
  list-style: none;
}

.text-fallback summary::-webkit-details-marker {
  display: none;
}

.summary-chevron {
  transition: transform var(--dur-short) var(--ease-out);
}

.text-fallback[open] .summary-chevron {
  transform: rotate(180deg);
}

.text-fallback label {
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  font-weight: 600;
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
