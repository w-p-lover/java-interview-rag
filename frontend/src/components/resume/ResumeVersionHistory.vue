<script setup lang="ts">
import { Check, FileCheck2, LockKeyhole } from "@lucide/vue";

import type { ResumeDetail } from "@/api/resumeCenter";

defineProps<{
  detail: ResumeDetail | null;
  loading: boolean;
  switchingVersionId: string;
}>();
const emit = defineEmits<{ switch: [versionId: string] }>();

function formatDate(value: string): string {
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? value : new Intl.DateTimeFormat("zh-CN", {
    dateStyle: "medium", timeStyle: "short", hour12: false,
  }).format(parsed);
}

function formatSize(bytes: number): string {
  return bytes < 1024 ? `${bytes} B` : bytes < 1024 * 1024
    ? `${(bytes / 1024).toFixed(1)} KiB` : `${(bytes / 1024 / 1024).toFixed(1)} MiB`;
}
</script>

<template>
  <section id="resume-versions" class="version-section" aria-labelledby="resume-versions-title" tabindex="-1">
    <header class="section-heading">
      <div><h2 id="resume-versions-title">版本历史</h2><p>{{ detail?.targetRole ?? "选择一条简历记录" }}</p></div>
      <span v-if="detail">{{ detail.versions.length }} 个不可变版本</span>
    </header>
    <div v-if="loading" class="loading-state" aria-live="polite">正在读取版本历史…</div>
    <p v-else-if="!detail" class="empty-state">从左侧选择简历后查看版本。</p>
    <ol v-else class="version-list">
      <li v-for="version in detail.versions" :key="version.id" :class="{ current: version.id === detail.currentVersionId }">
        <div class="version-index"><strong>v{{ version.versionNumber }}</strong><span v-if="version.id === detail.currentVersionId"><Check :size="14" aria-hidden="true" />当前</span></div>
        <div class="version-content"><strong>{{ version.originalFilename }}</strong><span>{{ formatDate(version.createdAt) }} · {{ formatSize(version.fileSize) }}</span><code>{{ version.contentHash.slice(0, 12) }}</code></div>
        <div class="privacy-state"><FileCheck2 v-if="version.fullTextSaved" :size="17" aria-hidden="true" /><LockKeyhole v-else :size="17" aria-hidden="true" /><span>{{ version.fullTextSaved ? "正文已保存" : "仅元数据" }}</span></div>
        <button v-if="version.id !== detail.currentVersionId" type="button" class="secondary-action" :disabled="Boolean(switchingVersionId)" @click="emit('switch', version.id)">{{ switchingVersionId === version.id ? "切换中…" : "设为当前" }}</button>
      </li>
    </ol>
  </section>
</template>

<style scoped lang="scss">
.version-section { min-width: 0; }
.section-heading { display: flex; align-items: baseline; justify-content: space-between; gap: var(--space-lg); padding-bottom: var(--space-sm); border-bottom: var(--rule-thin) solid var(--color-rule-2); }
.section-heading h2, .section-heading p { margin: 0; }
.section-heading h2 { font-size: var(--text-lg); }
.section-heading p, .section-heading > span { margin-top: var(--space-2xs); color: var(--color-muted); font-size: var(--text-xs); }
.version-list { margin: 0; padding: 0; list-style: none; }
.version-list li { display: grid; grid-template-columns: 4.5rem minmax(0, 1fr) auto auto; align-items: center; gap: var(--space-md); min-height: 5.5rem; padding-block: var(--space-md); border-bottom: var(--rule-thin) solid var(--color-rule); }
.version-list li.current { border-bottom-color: var(--color-success-rule); }
.version-index, .version-content, .privacy-state { display: grid; gap: var(--space-2xs); min-width: 0; }
.version-index strong { font-family: var(--font-mono); font-size: var(--text-lg); }
.version-index span { display: inline-flex; align-items: center; gap: var(--space-3xs); color: var(--color-success); font-size: var(--text-xs); }
.version-content strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-size: var(--text-sm); }
.version-content span, .privacy-state { color: var(--color-muted); font-size: var(--text-xs); }
.version-content code { width: fit-content; color: var(--color-faint); font-size: var(--text-xs); }
.privacy-state { grid-template-columns: auto auto; align-items: center; }
.secondary-action { min-height: 2.5rem; padding-inline: var(--space-sm); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); color: var(--color-accent-strong); background: var(--color-surface); font-size: var(--text-xs); font-weight: 700; white-space: nowrap; }
.loading-state, .empty-state { padding-block: var(--space-lg); color: var(--color-muted); font-size: var(--text-sm); }
@media (max-width: 48rem) { .version-list li { grid-template-columns: 3.5rem minmax(0, 1fr); } .privacy-state, .secondary-action { grid-column: 2; justify-self: start; } }
</style>
