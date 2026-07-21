<script setup lang="ts">
import type { MarkdownImportResponse } from "@/api/knowledge";

defineProps<{
  result: MarkdownImportResponse | null;
}>();
</script>

<template>
  <section class="ledger" aria-labelledby="ledger-title" aria-live="polite">
    <header class="panel-heading">
      <div>
        <span class="panel-kicker">资料检查</span>
        <h2 id="ledger-title">导入结果</h2>
      </div>
      <span v-if="result" class="chunk-count">{{ result.chunkCount }} 个切片</span>
    </header>

    <div v-if="!result" class="empty-ledger">
      <span class="empty-symbol" aria-hidden="true">--</span>
      <strong>等待导入资料</strong>
      <p>导入后，这里会显示版本状态、标题结构和可供后续训练引用的切片。</p>
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
      </div>

      <div v-if="!result.imported" class="status-note is-warning">
        这份资料的来源和内容版本已经登记，本次没有重复生成切片。
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
  margin-bottom: 1.15rem;
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

.chunk-count {
  padding: 0.25rem 0.45rem;
  border: 1px solid var(--border);
  border-radius: 0.35rem;
  color: var(--ink-muted);
  font-size: 0.7rem;
  white-space: nowrap;
}

.empty-ledger {
  display: grid;
  min-height: 19rem;
  place-items: center;
  align-content: center;
  padding: 2rem;
  border: 1px dashed var(--border-strong);
  border-radius: var(--radius);
  background: var(--surface-subtle);
  text-align: center;
}

.empty-symbol {
  color: var(--ink-faint);
  font-family: var(--font-mono);
  font-size: 1.6rem;
}

.empty-ledger strong {
  margin-top: 0.75rem;
  color: var(--ink);
  font-size: 0.92rem;
  font-weight: 650;
}

.empty-ledger p {
  max-width: 24rem;
  margin: 0.45rem 0 0;
  color: var(--ink-muted);
  font-size: 0.78rem;
  line-height: 1.55;
}

.document-summary {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(12rem, 0.9fr);
  gap: 1rem;
  margin-bottom: 0.9rem;
  padding: 0.85rem;
  border: 1px solid var(--success-border);
  border-radius: 0.4rem;
  background: var(--success-soft);
}

.document-summary.is-duplicate {
  border-color: var(--warning-border);
  background: var(--warning-soft);
}

.summary-source,
.version-summary {
  display: grid;
  min-width: 0;
  gap: 0.25rem;
}

.summary-label {
  color: var(--ink-muted);
  font-size: 0.68rem;
  font-weight: 600;
}

.summary-source strong {
  overflow: hidden;
  color: var(--ink);
  font-size: 0.82rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.version-summary code {
  overflow: hidden;
  color: var(--ink-muted);
  font-family: var(--font-mono);
  font-size: 0.66rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-note {
  padding: 0.75rem 0.85rem;
  border: 1px solid var(--border);
  border-radius: 0.4rem;
  color: var(--ink-muted);
  background: var(--surface-subtle);
  font-size: 0.78rem;
  line-height: 1.5;
}

.status-note.is-warning {
  border-color: var(--warning-border);
  color: var(--warning);
  background: var(--warning-soft);
}

.chunk-list {
  display: grid;
  max-height: 39rem;
  margin: 0;
  padding: 0;
  overflow-y: auto;
  list-style: none;
}

.chunk-row {
  overflow: hidden;
  border: 1px solid var(--border);
  border-bottom: 0;
  background: var(--surface);
}

.chunk-row:first-child {
  border-radius: 0.4rem 0.4rem 0 0;
}

.chunk-row:last-child {
  border-bottom: 1px solid var(--border);
  border-radius: 0 0 0.4rem 0.4rem;
}

.chunk-row header {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.65rem;
  padding: 0.6rem 0.75rem;
  border-bottom: 1px solid var(--border);
  color: var(--ink-muted);
  font-size: 0.68rem;
}

.chunk-order {
  color: var(--accent);
  font-family: var(--font-mono);
  font-weight: 650;
}

.heading-path {
  overflow: hidden;
  color: var(--ink);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-order {
  color: var(--ink-muted);
  white-space: nowrap;
}

.chunk-row pre {
  margin: 0;
  padding: 0.85rem;
  overflow-x: auto;
  color: var(--code-text);
  background: var(--code-bg);
  font-family: var(--font-mono);
  font-size: 0.74rem;
  line-height: 1.65;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

@media (max-width: 620px) {
  .document-summary {
    grid-template-columns: 1fr;
  }

  .chunk-row header {
    grid-template-columns: auto minmax(0, 1fr);
  }

  .section-order {
    grid-column: 2;
  }
}
</style>
