<script setup lang="ts">
import { CircleAlert, FileText, LibraryBig, RefreshCw } from "@lucide/vue";

import type { KnowledgeDocumentCatalogItem } from "@/api/knowledge";

defineProps<{
  loading: boolean;
  failed: boolean;
  documents: KnowledgeDocumentCatalogItem[];
  selectedSource: string;
  notice: string;
  busy: boolean;
}>();

defineEmits<{
  refresh: [];
  select: [source: string];
}>();

const dateFormatter = new Intl.DateTimeFormat("zh-CN", {
  dateStyle: "medium",
  timeStyle: "short",
});
const numberFormatter = new Intl.NumberFormat("zh-CN");

function formatRegisteredAt(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : dateFormatter.format(date);
}

function formatContentLength(value: number): string {
  return `${numberFormatter.format(value)} 字符`;
}

function formatVersion(value: string): string {
  return value.length > 12 ? value.slice(0, 12) : value;
}
</script>

<template>
  <section
    class="catalog-panel"
    :class="{ 'has-selection': Boolean(selectedSource) }"
    aria-labelledby="catalog-title"
    aria-live="polite"
  >
    <header class="catalog-header">
      <div>
        <h2 id="catalog-title">资料目录</h2>
      </div>
      <div class="catalog-actions">
        <strong v-if="!loading && !failed">{{ documents.length }} 个来源</strong>
        <button
          type="button"
          :disabled="loading || busy"
          aria-label="刷新资料目录"
          title="刷新资料目录"
          @click="$emit('refresh')"
        >
          <RefreshCw :class="{ 'is-loading': loading }" :size="17" aria-hidden="true" />
          <span>刷新</span>
        </button>
      </div>
    </header>

    <p v-if="notice" class="catalog-notice" role="status">{{ notice }}</p>

    <div v-if="loading && !documents.length" class="catalog-skeleton" aria-hidden="true">
      <span></span>
      <span></span>
      <span></span>
    </div>

    <div v-else-if="failed" class="catalog-empty">
      <CircleAlert :size="28" aria-hidden="true" />
      <div>
        <h3>目录暂时无法读取</h3>
        <p>检查服务状态后重试，检索与问答的既有结果不会受影响。</p>
        <button type="button" :disabled="loading || busy" @click="$emit('refresh')">
          <RefreshCw :size="17" aria-hidden="true" />
          <span>重新读取</span>
        </button>
      </div>
    </div>

    <ol v-else-if="documents.length" class="catalog-list">
      <li
        v-for="(document, index) in documents"
        :key="document.source"
        :class="{ 'is-selected': selectedSource === document.source }"
      >
        <span class="source-index">{{ index + 1 }}</span>
        <div class="source-identity">
          <strong>{{ document.source }}</strong>
          <span>{{ formatContentLength(document.contentLength) }}</span>
        </div>
        <dl>
          <div>
            <dt>当前版本</dt>
            <dd><code :title="document.version">{{ formatVersion(document.version) }}</code></dd>
          </div>
          <div>
            <dt>登记时间</dt>
            <dd><time :datetime="document.registeredAt">{{ formatRegisteredAt(document.registeredAt) }}</time></dd>
          </div>
        </dl>
        <button
          class="detail-button"
          type="button"
          :aria-label="`查看资料详情：${document.source}`"
          :title="`查看资料详情：${document.source}`"
          :aria-pressed="selectedSource === document.source"
          :disabled="busy"
          @click="$emit('select', document.source)"
        >
          <FileText :size="17" aria-hidden="true" />
          <span>详情</span>
        </button>
      </li>
    </ol>

    <div v-else class="catalog-empty">
      <LibraryBig :size="28" aria-hidden="true" />
      <div>
        <h3>尚未登记资料</h3>
        <p>从资料库导入 Markdown 后，逻辑来源和当前版本会显示在这里。</p>
      </div>
    </div>

  </section>
</template>

<style scoped lang="scss">
.catalog-panel {
  display: grid;
  align-content: start;
  gap: var(--space-md);
  min-width: 0;
  padding: var(--space-xs) 0 var(--space-lg);
  container-type: inline-size;
}

.catalog-header,
.catalog-actions,
.catalog-actions button,
.catalog-empty,
.catalog-empty button,
.detail-button {
  display: flex;
  align-items: center;
}

.catalog-header {
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-md);
  padding-bottom: var(--space-sm);
  border-bottom: var(--rule-thin) solid var(--color-rule-2);
}

.catalog-header > div:first-child {
  display: grid;
  gap: var(--space-2xs);
}

.catalog-header span,
.catalog-actions strong,
.source-identity span,
.catalog-empty p,
.catalog-list dt,
.catalog-list dd {
  color: var(--color-muted);
  font-size: var(--text-sm);
}

.catalog-header h2,
.catalog-empty h3,
.catalog-empty p,
.catalog-list dl {
  margin: 0;
}

.catalog-header h2 {
  color: var(--color-ink);
  font-size: var(--text-xl);
}

.catalog-actions {
  gap: var(--space-md);
}

.catalog-actions strong {
  color: var(--color-accent-strong);
}

.catalog-actions button,
.catalog-empty button {
  justify-content: center;
  gap: var(--space-xs);
  min-height: 2.5rem;
  padding-inline: var(--space-sm);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-ink-2);
  background: var(--color-surface);
  font: inherit;
  font-size: var(--text-sm);
  font-weight: 700;
}

.catalog-actions button:disabled,
.catalog-empty button:disabled {
  cursor: wait;
  opacity: 0.68;
}

.catalog-actions button:focus-visible,
.catalog-empty button:focus-visible {
  outline: var(--rule-focus) solid var(--color-focus);
  outline-offset: var(--rule-thin);
}

.catalog-list {
  margin: 0;
  padding: 0;
  border-block: var(--rule-thin) solid var(--color-rule);
  list-style: none;
}

.catalog-list li {
  display: grid;
  grid-template-columns: 2rem minmax(14rem, 1.2fr) minmax(18rem, 1fr) auto;
  align-items: center;
  gap: var(--space-md);
  min-height: 4.25rem;
  padding: var(--space-sm) var(--space-xs);
}

.catalog-list li.is-selected {
  background: var(--color-paper-2);
}

.catalog-list li + li {
  border-top: var(--rule-thin) solid var(--color-rule);
}

.source-index {
  display: inline-grid;
  width: 2rem;
  height: 2rem;
  place-items: center;
  border-radius: var(--radius-round);
  color: var(--color-accent-strong);
  background: var(--color-accent-soft);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  font-weight: 800;
}

.source-identity {
  display: grid;
  min-width: 0;
  gap: var(--space-2xs);
}

.source-identity strong {
  color: var(--color-ink-2);
  overflow-wrap: anywhere;
}

.catalog-list dl {
  display: grid;
  grid-template-columns: minmax(8rem, 0.8fr) minmax(10rem, 1fr);
  gap: var(--space-md);
}

.catalog-list dl > div {
  display: grid;
  gap: var(--space-3xs);
}

.catalog-list code {
  color: var(--color-ink-2);
  font-family: var(--font-mono);
}

.detail-button {
  justify-content: center;
  gap: var(--space-xs);
  min-height: 2.5rem;
  padding-inline: var(--space-sm);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-ink-2);
  background: var(--color-surface);
  font: inherit;
  font-size: var(--text-sm);
  font-weight: 700;
}

.detail-button[aria-pressed="true"] {
  border-color: var(--color-accent);
  color: var(--color-accent-strong);
  background: var(--color-accent-soft);
}

.detail-button:focus-visible {
  outline: var(--rule-focus) solid var(--color-focus);
  outline-offset: var(--rule-thin);
}

.catalog-notice {
  margin: 0;
  padding: var(--space-sm) var(--space-md);
  border: var(--rule-thin) solid var(--color-success-rule);
  border-radius: var(--radius-control);
  color: var(--color-success);
  background: var(--color-success-soft);
  font-size: var(--text-sm);
  overflow-wrap: anywhere;
}

.catalog-empty {
  align-items: flex-start;
  gap: var(--space-md);
  margin-block: auto;
  padding: var(--space-xl) 0;
  color: var(--color-muted);
}

.catalog-empty > svg {
  flex: 0 0 auto;
  color: var(--color-accent);
}

.catalog-empty > div {
  display: grid;
  gap: var(--space-xs);
}

.catalog-empty h3 {
  color: var(--color-ink);
  font-size: var(--text-lg);
}

.catalog-empty p {
  max-width: 52ch;
  line-height: 1.6;
}

.catalog-empty button {
  width: fit-content;
  margin-top: var(--space-xs);
}

.catalog-skeleton {
  display: grid;
  gap: var(--space-sm);
}

.catalog-skeleton span {
  height: 5rem;
  border-radius: var(--radius-control);
  background: var(--color-paper-3);
  animation: skeleton-pulse 1.2s ease-in-out infinite alternate;
}

.catalog-skeleton span:nth-child(2) {
  opacity: 0.78;
}

.catalog-skeleton span:nth-child(3) {
  opacity: 0.56;
}

.is-loading {
  animation: loading-spin 800ms linear infinite;
}

@keyframes skeleton-pulse {
  to {
    opacity: 0.42;
  }
}

@keyframes loading-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 50rem) {
  .catalog-panel.has-selection {
    max-height: 32rem;
    padding-inline-end: var(--space-xs);
    overflow-y: auto;
  }

  .catalog-list li {
    grid-template-columns: 2rem minmax(0, 1fr);
  }

  .catalog-list dl {
    grid-column: 2;
  }

  .detail-button {
    grid-column: 2;
    width: fit-content;
  }
}

@container (max-width: 34rem) {
  .catalog-header {
    align-items: flex-start;
    flex-direction: column;
    gap: var(--space-sm);
  }

  .catalog-actions {
    width: 100%;
    justify-content: space-between;
  }

  .catalog-list li {
    grid-template-columns: 2rem minmax(0, 1fr) auto;
    gap: var(--space-sm);
  }

  .catalog-list dl {
    display: none;
  }

  .detail-button {
    grid-column: 3;
    width: auto;
    padding-inline: var(--space-xs);
  }

  .detail-button span {
    display: none;
  }
}

@media (min-width: 58.001rem) {
  .catalog-panel {
    position: sticky;
    top: var(--space-md);
    max-height: calc(100dvh - (var(--space-3xl) * 2));
    padding-inline-end: var(--space-xs);
    overflow-y: auto;
  }
}

@media (max-width: 40rem) {
  .catalog-panel {
    padding-bottom: var(--space-md);
  }

  .catalog-header {
    align-items: flex-start;
    flex-direction: column;
    gap: var(--space-sm);
  }

  .catalog-actions {
    width: 100%;
    justify-content: space-between;
  }

  .catalog-list dl {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (hover: hover) and (pointer: fine) {
  .catalog-actions button:hover:not(:disabled),
  .catalog-empty button:hover:not(:disabled),
  .detail-button:hover:not(:disabled) {
    border-color: var(--color-accent);
    color: var(--color-accent-strong);
    background: var(--color-accent-soft);
  }
}

@media (prefers-reduced-motion: reduce) {
  .catalog-skeleton span,
  .is-loading {
    animation: none;
  }
}
</style>
