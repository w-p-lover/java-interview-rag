<script setup lang="ts">
import { CircleAlert, Files, LibraryBig, RefreshCw } from "@lucide/vue";

import type { KnowledgeDocumentCatalogItem } from "@/api/knowledge";

defineProps<{
  loading: boolean;
  failed: boolean;
  documents: KnowledgeDocumentCatalogItem[];
}>();

defineEmits<{
  refresh: [];
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
  <section class="catalog-panel" aria-labelledby="catalog-title" aria-live="polite">
    <header class="catalog-header">
      <div>
        <span>当前登记范围</span>
        <h2 id="catalog-title">资料目录</h2>
      </div>
      <div class="catalog-actions">
        <strong v-if="!loading && !failed">{{ documents.length }} 个来源</strong>
        <button
          type="button"
          :disabled="loading"
          aria-label="刷新资料目录"
          title="刷新资料目录"
          @click="$emit('refresh')"
        >
          <RefreshCw :class="{ 'is-loading': loading }" :size="17" aria-hidden="true" />
          <span>刷新</span>
        </button>
      </div>
    </header>

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
        <button type="button" :disabled="loading" @click="$emit('refresh')">
          <RefreshCw :size="17" aria-hidden="true" />
          <span>重新读取</span>
        </button>
      </div>
    </div>

    <ol v-else-if="documents.length" class="catalog-list">
      <li v-for="(document, index) in documents" :key="document.source">
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
      </li>
    </ol>

    <div v-else class="catalog-empty">
      <LibraryBig :size="28" aria-hidden="true" />
      <div>
        <h3>尚未登记资料</h3>
        <p>从资料库导入 Markdown 后，逻辑来源和当前版本会显示在这里。</p>
      </div>
    </div>

    <footer class="catalog-note">
      <Files :size="17" aria-hidden="true" />
      <p>目录只展示每个逻辑来源当前登记的版本，不包含历史版本或资料正文。</p>
    </footer>
  </section>
</template>

<style scoped lang="scss">
.catalog-panel {
  display: grid;
  align-content: start;
  gap: var(--space-lg);
  min-width: 0;
  min-height: 28rem;
  padding: var(--space-xs) 0 var(--space-xl);
}

.catalog-header,
.catalog-actions,
.catalog-actions button,
.catalog-empty,
.catalog-empty button,
.catalog-note {
  display: flex;
  align-items: center;
}

.catalog-header {
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-lg);
  padding-bottom: var(--space-md);
  border-bottom: var(--rule-thin) solid var(--color-rule-2);
}

.catalog-header > div:first-child {
  display: grid;
  gap: var(--space-2xs);
}

.catalog-header span,
.catalog-actions strong,
.source-identity span,
.catalog-note,
.catalog-empty p,
.catalog-list dt,
.catalog-list dd {
  color: var(--color-muted);
  font-size: var(--text-sm);
}

.catalog-header h2,
.catalog-empty h3,
.catalog-empty p,
.catalog-note p,
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
  grid-template-columns: 2rem minmax(14rem, 1.2fr) minmax(18rem, 1fr);
  align-items: center;
  gap: var(--space-md);
  min-height: 5rem;
  padding: var(--space-md) var(--space-xs);
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

.catalog-empty {
  align-items: flex-start;
  gap: var(--space-md);
  margin-block: auto;
  padding: var(--space-xl) 0;
  color: var(--color-muted);
}

.catalog-empty > svg,
.catalog-note > svg {
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

.catalog-note {
  align-items: flex-start;
  gap: var(--space-sm);
  padding: var(--space-md);
  border-radius: var(--radius-control);
  background: var(--color-paper-2);
  line-height: 1.6;
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
  .catalog-list li {
    grid-template-columns: 2rem minmax(0, 1fr);
  }

  .catalog-list dl {
    grid-column: 2;
  }
}

@media (max-width: 40rem) {
  .catalog-panel {
    min-height: 20rem;
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
  .catalog-empty button:hover:not(:disabled) {
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
