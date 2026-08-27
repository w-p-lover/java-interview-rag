<script setup lang="ts">
import type { MarkdownImportResponse } from "@/api/knowledge";

type ImportState = "idle" | "loading" | "success" | "duplicate" | "error";

defineProps<{
  result: MarkdownImportResponse | null;
  state: ImportState;
}>();
</script>

<template>
  <section class="ledger" aria-labelledby="ledger-title" aria-live="polite">
    <header class="panel-heading">
      <div>
        <h3 id="ledger-title">导入结果</h3>
        <p>核对稳定来源、内容版本与标题路径。</p>
      </div>
      <span v-if="result" class="chunk-count">{{ result.chunkCount }} 个切片</span>
    </header>

    <div v-if="state === 'loading'" class="empty-ledger is-loading">
      <span class="empty-symbol" aria-hidden="true">···</span>
      <strong>正在解析资料</strong>
      <p>后端正在计算内容版本、解析标题并生成切片。</p>
    </div>

    <div v-else-if="state === 'error' && !result" class="empty-ledger is-error">
      <span class="empty-symbol" aria-hidden="true">×</span>
      <strong>本次没有生成结果</strong>
      <p>根据左侧提示修正文件或来源，然后重新导入。</p>
    </div>

    <div v-else-if="!result" class="empty-ledger">
      <span class="empty-symbol" aria-hidden="true">—</span>
      <strong>等待导入资料</strong>
      <p>完成导入后，这里会显示版本状态、标题结构和切片正文。</p>
    </div>

    <template v-else>
      <div class="document-summary" :class="{ 'is-duplicate': !result.imported }">
        <div class="summary-source">
          <span class="summary-label">{{ result.imported ? "已导入" : "已存在" }}</span>
          <strong>{{ result.source }}</strong>
        </div>
        <div class="version-summary">
          <span class="summary-label">内容版本</span>
          <code :title="result.version">{{ result.version }}</code>
        </div>
        <div class="format-summary">
          <span class="summary-label">格式</span>
          <strong>{{ result.format }}</strong>
        </div>
        <div v-if="result.pageCount" class="format-summary">
          <span class="summary-label">页数</span>
          <strong>{{ result.pageCount }}</strong>
        </div>
      </div>

      <div v-if="result.warnings.length" class="status-note is-warning">
        <strong>解析提醒</strong>
        <ul>
          <li v-for="warning in result.warnings" :key="warning">{{ warning }}</li>
        </ul>
      </div>

      <div v-if="!result.imported" class="status-note is-warning">
        来源和内容版本已经登记；本次没有重复生成切片。
      </div>

      <div v-else-if="result.chunks.length === 0" class="status-note">
        资料已登记，但正文没有生成可用切片。
      </div>

      <ol v-else class="chunk-list">
        <li v-for="chunk in result.chunks" :key="chunk.order" class="chunk-row">
          <header>
            <span class="chunk-order">切片 {{ String(chunk.order + 1).padStart(2, "0") }}</span>
            <span class="heading-path">
              {{ chunk.headingPath.length ? chunk.headingPath.join(" / ") : "文档导语" }}
            </span>
            <span class="section-order">段落 {{ chunk.sectionOrder + 1 }}</span>
          </header>
          <pre>{{ chunk.content }}</pre>
        </li>
      </ol>
    </template>
  </section>
</template>

<style scoped lang="scss">
.ledger {
  min-width: 0;
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

.panel-heading h3 {
  margin: 0;
  color: var(--color-ink);
  font-family: var(--font-display);
  font-size: var(--text-md);
  font-style: normal;
  font-weight: 700;
  letter-spacing: 0;
}

.panel-heading p {
  margin: var(--space-xs) 0 0;
  color: var(--color-muted);
  font-size: var(--text-sm);
}

.chunk-count {
  padding: var(--space-2xs) var(--space-xs);
  border-radius: var(--radius-control);
  color: var(--color-muted);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  white-space: nowrap;
}

.empty-ledger {
  display: grid;
  min-height: 22rem;
  place-items: center;
  align-content: center;
  padding: var(--space-xl);
  border-radius: var(--radius-control);
  background: var(--color-paper-2);
  text-align: center;
}

.empty-symbol {
  color: var(--color-faint);
  font-family: var(--font-mono);
  font-size: var(--text-xl);
}

.empty-ledger strong {
  margin-top: var(--space-sm);
  color: var(--color-ink);
  font-size: var(--text-sm);
  font-weight: 600;
}

.empty-ledger p {
  max-width: 45ch;
  margin: var(--space-xs) 0 0;
  color: var(--color-muted);
  font-size: var(--text-sm);
  line-height: 1.55;
}

.empty-ledger.is-loading {
  border-color: var(--color-accent);
  background: var(--color-accent-soft);
}

.empty-ledger.is-loading .empty-symbol {
  color: var(--color-accent);
  animation: loading-pulse 1.2s var(--ease-in-out) infinite;
}

.empty-ledger.is-error {
  border-color: var(--color-error-rule);
  background: var(--color-error-soft);
}

.empty-ledger.is-error .empty-symbol,
.empty-ledger.is-error strong {
  color: var(--color-error);
}

.document-summary {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(12rem, 0.9fr);
  gap: var(--space-md);
  margin-bottom: var(--space-sm);
  padding-block: var(--space-sm);
  background: var(--color-success-soft);
}

.document-summary.is-duplicate {
  border-color: var(--color-warning-rule);
  background: var(--color-warning-soft);
}

.summary-source,
.version-summary,
.format-summary {
  display: grid;
  min-width: 0;
  gap: var(--space-2xs);
  padding-inline: var(--space-sm);
}

.summary-label {
  color: var(--color-muted);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.summary-source strong,
.version-summary code,
.format-summary strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-source strong {
  color: var(--color-ink);
  font-size: var(--text-sm);
}

.version-summary code {
  color: var(--color-muted);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.format-summary strong {
  color: var(--color-ink);
  font-size: var(--text-sm);
}

.status-note {
  margin-bottom: var(--space-sm);
  padding: var(--space-sm);
  border-radius: var(--radius-control);
  color: var(--color-muted);
  background: var(--color-paper-2);
  font-size: var(--text-sm);
  line-height: 1.5;
}

.status-note.is-warning {
  color: var(--color-warning);
  background: var(--color-warning-soft);
}

.status-note ul {
  margin: var(--space-xs) 0 0;
  padding-left: var(--space-lg);
}

.chunk-list {
  display: grid;
  gap: var(--space-xs);
  max-height: 42rem;
  margin: 0;
  padding: 0;
  overflow-y: auto;
  list-style: none;
}

.chunk-row {
  min-width: 0;
  overflow: hidden;
  border-radius: var(--radius-control);
}

.chunk-row header {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm);
  color: var(--color-muted);
  background: var(--color-paper);
  font-size: var(--text-xs);
}

.chunk-order {
  color: var(--color-accent);
  font-family: var(--font-mono);
  font-weight: 600;
  white-space: nowrap;
}

.heading-path {
  overflow: hidden;
  color: var(--color-ink);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-order {
  white-space: nowrap;
}

.chunk-row pre {
  margin: 0;
  padding: var(--space-sm);
  overflow-x: auto;
  color: var(--color-ink-2);
  background: var(--color-paper-2);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  line-height: 1.65;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

@keyframes loading-pulse {
  50% { opacity: 0.45; }
}

@media (max-width: 39.99rem) {
  .ledger {
    padding: 0;
  }

  .panel-heading {
    display: grid;
  }

  .chunk-count {
    justify-self: start;
  }

  .empty-ledger {
    min-height: 16rem;
    padding: var(--space-lg);
  }

  .document-summary {
    grid-template-columns: minmax(0, 1fr);
  }

  .chunk-row header {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .section-order {
    grid-column: 2;
  }
}

@media (prefers-reduced-motion: reduce) {
  .empty-ledger.is-loading .empty-symbol {
    animation: none;
  }
}
</style>
