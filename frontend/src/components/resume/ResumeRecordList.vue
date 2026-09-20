<script setup lang="ts">
import { FileText } from "@lucide/vue";

import type { ResumeSummary } from "@/api/resumeCenter";

defineProps<{
  records: readonly ResumeSummary[];
  selectedId: string;
  loading: boolean;
}>();
const emit = defineEmits<{ select: [id: string] }>();

function formatDate(value: string): string {
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? value : new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", hour12: false,
  }).format(parsed);
}
</script>

<template>
  <section id="resume-records" class="record-section" aria-labelledby="resume-records-title" tabindex="-1">
    <header class="section-heading"><h2 id="resume-records-title">简历记录</h2><span>{{ records.length }}</span></header>
    <div v-if="loading && !records.length" class="skeleton-list" aria-label="正在读取简历记录"><span v-for="index in 3" :key="index"></span></div>
    <p v-else-if="!records.length" class="empty-state">还没有简历记录。</p>
    <ol v-else class="record-list">
      <li v-for="record in records" :key="record.id">
        <button type="button" :class="{ selected: record.id === selectedId }" @click="emit('select', record.id)">
          <FileText :size="18" aria-hidden="true" />
          <span class="record-copy"><strong>{{ record.name }}</strong><small>{{ record.targetRole }}</small></span>
          <span class="record-meta"><b>v{{ record.currentVersionNumber ?? "-" }}</b><small>{{ record.versionCount }} 版 · {{ formatDate(record.updatedAt) }}</small></span>
        </button>
      </li>
    </ol>
  </section>
</template>

<style scoped lang="scss">
.record-section { min-width: 0; }
.section-heading { display: flex; align-items: baseline; justify-content: space-between; gap: var(--space-sm); padding-bottom: var(--space-sm); border-bottom: var(--rule-thin) solid var(--color-rule-2); }
.section-heading h2 { margin: 0; font-size: var(--text-lg); }
.section-heading span { color: var(--color-muted); font-family: var(--font-mono); font-size: var(--text-xs); }
.record-list { margin: 0; padding: 0; list-style: none; }
.record-list li { border-bottom: var(--rule-thin) solid var(--color-rule); }
.record-list button { display: grid; grid-template-columns: auto minmax(0, 1fr) auto; align-items: center; gap: var(--space-sm); width: 100%; min-height: 4.75rem; padding: var(--space-sm); border: 0; border-left: 3px solid transparent; color: var(--color-ink-2); background: transparent; text-align: left; }
.record-list button:hover { background: var(--color-paper-2); }
.record-list button.selected { border-left-color: var(--color-accent); background: var(--color-accent-soft); }
.record-copy, .record-meta { display: grid; gap: var(--space-2xs); min-width: 0; }
.record-copy strong, .record-copy small { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.record-copy strong { font-size: var(--text-sm); }
.record-copy small, .record-meta small { color: var(--color-muted); font-size: var(--text-xs); }
.record-meta { justify-items: end; }
.record-meta b { color: var(--color-accent-strong); font-family: var(--font-mono); font-size: var(--text-xs); }
.skeleton-list { display: grid; gap: var(--space-xs); padding-top: var(--space-md); }
.skeleton-list span { height: 4rem; background: var(--color-paper-2); animation: pulse 1.2s ease-in-out infinite alternate; }
.empty-state { color: var(--color-muted); font-size: var(--text-sm); }
@keyframes pulse { to { opacity: 0.5; } }
@media (prefers-reduced-motion: reduce) { .skeleton-list span { animation: none; } }
</style>
