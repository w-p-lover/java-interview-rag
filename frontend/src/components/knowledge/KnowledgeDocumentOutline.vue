<script setup lang="ts">
import { computed } from "vue";
import { BookOpenText, CircleAlert } from "@lucide/vue";

import type {
  KnowledgeDocumentOutline,
  KnowledgeDocumentOutlineNode,
} from "@/api/knowledge";

const props = defineProps<{
  outline: KnowledgeDocumentOutline | null;
}>();

interface OutlineRow {
  key: string;
  node: KnowledgeDocumentOutlineNode;
  depth: number;
}

const rows = computed(() => {
  if (!props.outline) {
    return [];
  }
  const flattened: OutlineRow[] = [];
  const append = (nodes: KnowledgeDocumentOutlineNode[], depth: number, parentKey: string): void => {
    nodes.forEach((node, index) => {
      const key = `${parentKey}.${index}`;
      flattened.push({ key, node, depth });
      append(node.children, depth + 1, key);
    });
  };
  append(props.outline.roots, 0, "root");
  return flattened;
});

function formatChunkRange(node: KnowledgeDocumentOutlineNode): string {
  if (node.firstChunkOrder === null || node.directChunkCount === 0) {
    return "无直属切片";
  }
  const first = node.firstChunkOrder + 1;
  const last = first + node.directChunkCount - 1;
  return first === last ? `Chunk ${first}` : `Chunk ${first}-${last}`;
}
</script>

<template>
  <section class="outline" aria-labelledby="knowledge-outline-title">
    <header class="outline-header">
      <div>
        <BookOpenText :size="18" aria-hidden="true" />
        <h3 id="knowledge-outline-title">当前版本章节</h3>
      </div>
      <span v-if="outline">
        {{ outline.headingCount }} 个标题 · {{ outline.chunkCount }} 个 Chunk
      </span>
    </header>

    <div v-if="!outline" class="outline-empty">
      <CircleAlert :size="20" aria-hidden="true" />
      <div>
        <strong>章节目录待重建</strong>
        <p>该版本登记早于章节目录功能。请在下方重新选择当前原文件完成重建。</p>
      </div>
    </div>

    <ol v-else-if="rows.length" class="outline-tree">
      <li
        v-for="row in rows"
        :key="row.key"
        class="outline-row"
        :style="{ '--outline-depth': row.depth }"
      >
        <span class="outline-level">H{{ row.node.level }}</span>
        <strong class="outline-title">{{ row.node.title }}</strong>
        <span class="outline-chunks">{{ formatChunkRange(row.node) }}</span>
      </li>
    </ol>

    <p v-else class="outline-empty-text">当前版本没有 Markdown 标题。</p>
  </section>
</template>

<style scoped lang="scss">
.outline {
  display: grid;
  min-width: 0;
  gap: var(--space-sm);
}

.outline-header,
.outline-header > div,
.outline-empty,
.outline-row {
  display: flex;
  align-items: center;
}

.outline-header {
  justify-content: space-between;
  gap: var(--space-md);
}

.outline-header > div {
  gap: var(--space-xs);
  color: var(--color-accent-strong);
}

.outline-header h3,
.outline-empty p,
.outline-empty-text {
  margin: 0;
}

.outline-header h3 {
  color: var(--color-ink);
  font-size: var(--text-md);
}

.outline-header > span,
.outline-chunks,
.outline-empty-text {
  color: var(--color-muted);
  font-size: var(--text-sm);
}

.outline-tree {
  margin: 0;
  padding: 0;
  border-block: var(--rule-thin) solid var(--color-rule);
  list-style: none;
}

.outline-row {
  --outline-depth: 0;
  min-width: 0;
  gap: var(--space-sm);
  min-height: 2.75rem;
  padding-block: var(--space-xs);
  padding-inline: calc(var(--space-sm) + var(--outline-depth) * var(--space-lg)) var(--space-sm);
}

.outline-row + .outline-row {
  border-top: var(--rule-thin) solid var(--color-rule);
}

.outline-level {
  width: 2rem;
  flex: 0 0 auto;
  color: var(--color-accent-strong);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
}

.outline-title {
  min-width: 0;
  flex: 1 1 auto;
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  overflow-wrap: anywhere;
}

.outline-chunks {
  flex: 0 0 auto;
  font-family: var(--font-mono);
}

.outline-empty {
  align-items: flex-start;
  gap: var(--space-sm);
  padding: var(--space-md);
  border: var(--rule-thin) solid var(--color-warning-rule);
  border-radius: var(--radius-control);
  color: var(--color-warning);
  background: var(--color-warning-soft);
}

.outline-empty > div {
  display: grid;
  gap: var(--space-2xs);
}

.outline-empty strong {
  color: var(--color-ink);
  font-size: var(--text-sm);
}

.outline-empty p,
.outline-empty-text {
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  line-height: 1.55;
}

@media (max-width: 32rem) {
  .outline-header {
    align-items: flex-start;
    flex-direction: column;
    gap: var(--space-2xs);
  }

  .outline-row {
    align-items: flex-start;
    flex-wrap: wrap;
    padding-inline-start: calc(var(--space-xs) + var(--outline-depth) * var(--space-sm));
  }

  .outline-title {
    flex-basis: calc(100% - 3rem);
  }

  .outline-chunks {
    width: 100%;
    padding-inline-start: calc(2rem + var(--space-sm));
  }
}
</style>
