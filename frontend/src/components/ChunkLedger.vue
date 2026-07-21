<script setup lang="ts">
import type { MarkdownImportResponse } from "@/api/knowledge";

defineProps<{
  result: MarkdownImportResponse | null;
}>();
</script>

<template>
  <section class="ledger" aria-labelledby="ledger-title" aria-live="polite">
    <div class="section-heading">
      <span class="section-index">B</span>
      <div>
        <p class="eyebrow">Chunk ledger</p>
        <h2 id="ledger-title">系统如何理解它</h2>
      </div>
      <span v-if="result" class="chunk-count">{{ result.chunkCount }} chunks</span>
    </div>

    <div v-if="!result" class="empty-ledger">
      <span class="empty-symbol" aria-hidden="true">{ }</span>
      <strong>等待第一份资料</strong>
      <p>导入后，这里会按标题路径和原文顺序展示可追踪的知识切片。</p>
    </div>

    <template v-else>
      <div class="document-stamp" :class="{ 'is-duplicate': !result.imported }">
        <div>
          <span>{{ result.imported ? "NEW VERSION" : "KNOWN VERSION" }}</span>
          <strong>{{ result.source }}</strong>
        </div>
        <code :title="result.version">{{ result.version }}</code>
      </div>

      <div v-if="!result.imported" class="duplicate-note">
        该来源与内容版本已经登记，本次没有重复生成 chunk。
      </div>

      <div v-else-if="result.chunks.length === 0" class="duplicate-note is-empty">
        文档已登记，但正文没有生成可用 chunk。
      </div>

      <ol v-else class="chunk-list">
        <li v-for="chunk in result.chunks" :key="chunk.order" class="chunk-card">
          <header>
            <span class="chunk-order">#{{ String(chunk.order).padStart(2, "0") }}</span>
            <span class="heading-path">
              {{ chunk.headingPath.length ? chunk.headingPath.join(" / ") : "文档导语" }}
            </span>
            <span class="section-order">S{{ chunk.sectionOrder }}</span>
          </header>
          <pre>{{ chunk.content }}</pre>
        </li>
      </ol>
    </template>
  </section>
</template>

<style scoped lang="scss">
.ledger {
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
  flex: 0 0 auto;
  place-items: center;
  border: 1px solid var(--cinnabar);
  border-radius: 50%;
  color: var(--cinnabar);
  font-family: var(--font-mono);
  font-size: 0.78rem;
}

.eyebrow {
  margin: 0 0 0.2rem;
  color: var(--jade);
  font-family: var(--font-mono);
  font-size: 0.7rem;
  letter-spacing: 0.16em;
  text-transform: uppercase;
}

h2 {
  margin: 0;
  font-family: var(--font-display);
  font-size: 1.55rem;
}

.chunk-count {
  margin-left: auto;
  padding: 0.35rem 0.55rem;
  border: 1px solid var(--lead);
  border-radius: 999px;
  color: var(--ink-soft);
  font-family: var(--font-mono);
  font-size: 0.65rem;
}

.empty-ledger {
  display: grid;
  min-height: 24rem;
  place-items: center;
  align-content: center;
  padding: 2rem;
  border: 1px solid var(--lead);
  border-radius: var(--radius-medium);
  background: var(--surface-strong);
  text-align: center;
}

.empty-symbol {
  color: var(--lead-dark);
  font-family: var(--font-mono);
  font-size: 2.3rem;
}

.empty-ledger strong {
  margin-top: 0.7rem;
  font-family: var(--font-display);
  font-size: 1.1rem;
}

.empty-ledger p {
  max-width: 24rem;
  margin: 0.4rem 0 0;
  color: var(--ink-soft);
  font-size: 0.8rem;
}

.document-stamp {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(12rem, 0.9fr);
  gap: 1rem;
  margin-bottom: 1rem;
  padding: 1rem;
  border-left: 3px solid var(--jade);
  border-radius: 0.2rem 0.8rem 0.8rem 0.2rem;
  background: var(--jade-soft);
}

.document-stamp.is-duplicate {
  border-left-color: var(--cinnabar);
  background: var(--cinnabar-soft);
}

.document-stamp div {
  display: grid;
  min-width: 0;
}

.document-stamp span {
  color: var(--ink-soft);
  font-family: var(--font-mono);
  font-size: 0.62rem;
  letter-spacing: 0.12em;
}

.document-stamp strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.document-stamp code {
  align-self: center;
  overflow: hidden;
  color: var(--ink-soft);
  font-family: var(--font-mono);
  font-size: 0.68rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.duplicate-note {
  padding: 1rem;
  border: 1px solid color-mix(in srgb, var(--cinnabar) 30%, transparent);
  border-radius: 0.8rem;
  color: var(--cinnabar);
  background: var(--surface-strong);
  font-size: 0.82rem;
}

.duplicate-note.is-empty {
  border-color: var(--lead);
  color: var(--ink-soft);
}

.chunk-list {
  display: grid;
  gap: 0.8rem;
  max-height: 40rem;
  margin: 0;
  padding: 0 0.25rem 0 0;
  overflow-y: auto;
  list-style: none;
}

.chunk-card {
  overflow: hidden;
  border: 1px solid var(--lead);
  border-radius: 0.9rem;
  background: var(--surface-strong);
  box-shadow: var(--shadow-soft);
}

.chunk-card header {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.7rem;
  padding: 0.65rem 0.8rem;
  border-bottom: 1px solid var(--lead);
  color: var(--ink-soft);
  font-family: var(--font-mono);
  font-size: 0.65rem;
}

.chunk-order {
  color: var(--cinnabar);
  font-weight: 700;
}

.heading-path {
  overflow: hidden;
  color: var(--ink);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section-order {
  color: var(--jade);
}

.chunk-card pre {
  margin: 0;
  padding: 1rem;
  overflow-x: auto;
  color: var(--code-text);
  background: var(--code-bg);
  font-family: var(--font-mono);
  font-size: 0.76rem;
  line-height: 1.7;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}

@media (max-width: 620px) {
  .section-heading {
    flex-wrap: wrap;
  }

  .chunk-count {
    width: fit-content;
    margin-left: 3.2rem;
  }

  .document-stamp {
    grid-template-columns: 1fr;
  }
}
</style>
