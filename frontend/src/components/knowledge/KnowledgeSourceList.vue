<script setup lang="ts">
import { FileText } from "@lucide/vue";

import type { KnowledgeAnswerSource } from "@/api/knowledge";

withDefaults(defineProps<{
  sources: KnowledgeAnswerSource[];
  title?: string;
}>(), {
  title: "依据资料",
});

function formatHeadingPath(source: KnowledgeAnswerSource): string {
  return source.headingPath.length ? source.headingPath.join(" / ") : "未标注标题";
}

function formatSimilarity(similarity: number | null): string {
  return similarity === null ? "未提供" : `${Math.round(similarity * 100)}%`;
}
</script>

<template>
  <section v-if="sources.length" id="knowledge-sources" class="source-list" aria-labelledby="sources-title">
    <header>
      <h3 id="sources-title">{{ title }}</h3>
      <span>{{ sources.length }} 条</span>
    </header>

    <ol>
      <li v-for="(source, index) in sources" :key="`${source.source}-${source.documentVersion}-${source.chunkOrder}`">
        <details>
          <summary>
            <span class="source-number">{{ index + 1 }}</span>
            <span class="source-title">
              <strong>{{ source.source }}</strong>
              <small>{{ formatHeadingPath(source) }}</small>
            </span>
            <span class="source-score">{{ formatSimilarity(source.similarity) }}</span>
          </summary>
          <div class="source-detail">
            <dl>
              <div>
                <dt>文档版本</dt>
                <dd>{{ source.documentVersion }}</dd>
              </div>
              <div>
                <dt>切片序号</dt>
                <dd>{{ source.chunkOrder }}</dd>
              </div>
            </dl>
            <div class="source-content">
              <FileText :size="17" aria-hidden="true" />
              <p>{{ source.content }}</p>
            </div>
          </div>
        </details>
      </li>
    </ol>
  </section>
</template>

<style scoped lang="scss">
.source-list {
  display: grid;
  gap: var(--space-sm);
  padding-top: var(--space-lg);
  border-top: var(--rule-thin) solid var(--color-rule);
}

header,
summary,
.source-content {
  display: flex;
  align-items: center;
}

header {
  justify-content: space-between;
  gap: var(--space-md);
}

h3 {
  margin: 0;
  color: var(--color-ink);
  font-size: var(--text-base);
}

header > span,
.source-score,
.source-title small,
dt,
dd {
  color: var(--color-muted);
  font-size: var(--text-xs);
}

ol {
  margin: 0;
  padding: 0;
  border-block: var(--rule-thin) solid var(--color-rule);
  list-style: none;
}

li + li {
  border-top: var(--rule-thin) solid var(--color-rule);
}

details {
  background: transparent;
}

summary {
  gap: var(--space-sm);
  min-height: 4.25rem;
  padding: var(--space-sm) var(--space-xs);
  cursor: pointer;
}

.source-number {
  display: inline-grid;
  flex: 0 0 auto;
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

.source-title {
  display: grid;
  min-width: 0;
  flex: 1;
  gap: var(--space-2xs);
}

.source-title strong {
  overflow-wrap: anywhere;
  color: var(--color-ink-2);
  font-size: var(--text-sm);
}

.source-title small {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-score {
  flex: 0 0 auto;
  font-family: var(--font-mono);
  font-weight: 700;
}

.source-detail {
  display: grid;
  gap: var(--space-md);
  padding: 0 var(--space-xs) var(--space-lg) calc(2rem + var(--space-sm));
}

dl {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm) var(--space-lg);
  margin: 0;
}

dl > div {
  display: grid;
  gap: var(--space-3xs);
}

dd {
  margin: 0;
  overflow-wrap: anywhere;
  font-family: var(--font-mono);
}

.source-content {
  align-items: flex-start;
  gap: var(--space-sm);
  padding: var(--space-md);
  border-radius: var(--radius-control);
  color: var(--color-ink-2);
  background: var(--color-paper-2);
}

.source-content svg {
  flex: 0 0 auto;
  margin-top: var(--space-3xs);
  color: var(--color-accent);
}

.source-content p {
  max-width: 72ch;
  margin: 0;
  white-space: pre-wrap;
  font-size: var(--text-sm);
  line-height: 1.7;
  overflow-wrap: anywhere;
}

@media (max-width: 40rem) {
  .source-score {
    align-self: flex-start;
    padding-top: var(--space-2xs);
  }

  .source-detail {
    padding-left: var(--space-xs);
  }
}
</style>
