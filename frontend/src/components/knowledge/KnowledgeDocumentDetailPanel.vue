<script setup lang="ts">
import { shallowRef, useTemplateRef, watch } from "vue";
import { CircleAlert, History, RefreshCw, RotateCw, Trash2, X } from "@lucide/vue";

import type { KnowledgeDocumentDetailsResponse } from "@/api/knowledge";
import KnowledgeDocumentOutline from "@/components/knowledge/KnowledgeDocumentOutline.vue";

const props = defineProps<{
  source: string;
  details: KnowledgeDocumentDetailsResponse | null;
  loading: boolean;
  error: string;
  deleting: boolean;
  rebuilding: boolean;
  rebuildError: string;
}>();

const emit = defineEmits<{
  close: [];
  retry: [];
  retire: [];
  rebuild: [file: File];
}>();

const confirmingDelete = shallowRef(false);
const rebuildFileInput = useTemplateRef<HTMLInputElement>("rebuildFileInput");
const dateFormatter = new Intl.DateTimeFormat("zh-CN", {
  dateStyle: "medium",
  timeStyle: "short",
});
const numberFormatter = new Intl.NumberFormat("zh-CN");

watch(() => props.source, () => {
  confirmingDelete.value = false;
  if (rebuildFileInput.value) {
    rebuildFileInput.value.value = "";
  }
});

function chooseRebuildFile(): void {
  rebuildFileInput.value?.click();
}

function handleRebuildFileChange(event: Event): void {
  const file = (event.target as HTMLInputElement).files?.[0];
  if (!file) {
    return;
  }
  emit("rebuild", file);
  (event.target as HTMLInputElement).value = "";
}

function formatRegisteredAt(value: string): string {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? value : dateFormatter.format(date);
}

function formatContentLength(value: number): string {
  return `${numberFormatter.format(value)} 字符`;
}

function formatVersion(value: string): string {
  return value.length > 16 ? value.slice(0, 16) : value;
}
</script>

<template>
  <section class="detail-panel" aria-labelledby="document-detail-title" aria-live="polite">
    <header class="detail-header">
      <div>
        <span>来源详情</span>
        <h2 id="document-detail-title">{{ source }}</h2>
      </div>
      <button
        class="icon-button"
        type="button"
        aria-label="关闭资料详情"
        title="关闭资料详情"
        :disabled="deleting || rebuilding"
        @click="emit('close')"
      >
        <X :size="18" aria-hidden="true" />
      </button>
    </header>

    <div v-if="loading" class="detail-skeleton" aria-hidden="true">
      <span></span>
      <span></span>
    </div>

    <div v-else-if="error" class="detail-error" role="alert">
      <CircleAlert :size="24" aria-hidden="true" />
      <div>
        <strong>详情暂时无法读取</strong>
        <p>{{ error }}</p>
        <button type="button" :disabled="deleting || rebuilding" @click="emit('retry')">
          <RefreshCw :size="17" aria-hidden="true" />
          <span>重试</span>
        </button>
      </div>
    </div>

    <template v-else-if="details">
      <dl class="detail-summary">
        <div>
          <dt>当前版本</dt>
          <dd><code :title="details.currentVersion">{{ formatVersion(details.currentVersion) }}</code></dd>
        </div>
        <div>
          <dt>版本登记</dt>
          <dd>{{ details.versionCount }} 条</dd>
        </div>
        <div>
          <dt>当前字符数</dt>
          <dd>{{ formatContentLength(details.versions[0].contentLength) }}</dd>
        </div>
        <div>
          <dt>当前登记时间</dt>
          <dd>
            <time :datetime="details.versions[0].registeredAt">
              {{ formatRegisteredAt(details.versions[0].registeredAt) }}
            </time>
          </dd>
        </div>
      </dl>

      <KnowledgeDocumentOutline :outline="details.outline" />

      <div class="version-history">
        <header>
          <History :size="18" aria-hidden="true" />
          <h3>版本历史</h3>
        </header>
        <ol>
          <li v-for="version in details.versions" :key="version.version">
            <div>
              <code :title="version.version">{{ formatVersion(version.version) }}</code>
              <strong v-if="version.current">当前生效</strong>
              <span v-else>历史登记</span>
            </div>
            <dl>
              <div>
                <dt>内容规模</dt>
                <dd>{{ formatContentLength(version.contentLength) }}</dd>
              </div>
              <div>
                <dt>登记时间</dt>
                <dd><time :datetime="version.registeredAt">{{ formatRegisteredAt(version.registeredAt) }}</time></dd>
              </div>
            </dl>
          </li>
        </ol>
      </div>

      <dl class="lifecycle-summary">
        <div>
          <dt>版本替换</dt>
          <dd>新版本完成向量写入后，旧版本向量退出检索，历史登记继续保留。</dd>
        </div>
        <div>
          <dt>当前版本重建</dt>
          <dd>重新选择当前原文件后重建向量，不会恢复历史版本或修改版本登记。</dd>
        </div>
        <div>
          <dt>人工淘汰</dt>
          <dd>删除该来源全部向量和版本登记；向量清理失败时不会先删除登记。</dd>
        </div>
      </dl>

      <div class="rebuild-action">
        <div>
          <strong>重建当前版本</strong>
          <p>请选择与当前版本一致的原 Markdown 文件。内容不一致时服务端会拒绝重建。</p>
        </div>
        <button type="button" :disabled="deleting || rebuilding" @click="chooseRebuildFile">
          <RotateCw :size="17" aria-hidden="true" />
          <span>{{ rebuilding ? "正在重建" : "选择原文件并重建" }}</span>
        </button>
        <input
          ref="rebuildFileInput"
          hidden
          type="file"
          accept=".md,.markdown,text/markdown"
          :disabled="deleting || rebuilding"
          @change="handleRebuildFileChange"
        />
        <p v-if="rebuildError" class="rebuild-error" role="alert">{{ rebuildError }}</p>
      </div>

      <div v-if="confirmingDelete" class="delete-confirmation">
        <div>
          <strong>确认淘汰这份资料？</strong>
          <p>将删除“{{ details.source }}”的全部向量和 {{ details.versionCount }} 条版本登记，页面内无法撤销。</p>
        </div>
        <div class="confirmation-actions">
          <button type="button" :disabled="deleting || rebuilding" @click="confirmingDelete = false">取消</button>
          <button class="danger-button" type="button" :disabled="deleting || rebuilding" @click="emit('retire')">
            <Trash2 :size="17" aria-hidden="true" />
            <span>{{ deleting ? "正在淘汰" : "确认淘汰" }}</span>
          </button>
        </div>
      </div>

      <button
        v-else
        class="delete-button"
        type="button"
        :disabled="deleting || rebuilding"
        @click="confirmingDelete = true"
      >
        <Trash2 :size="17" aria-hidden="true" />
        <span>淘汰资料</span>
      </button>
    </template>
  </section>
</template>

<style scoped lang="scss">
.detail-panel {
  display: grid;
  gap: var(--space-lg);
  min-width: 0;
  padding: var(--space-lg);
  border-top: var(--rule-thin) solid var(--color-rule-2);
  background: var(--color-paper-2);
}

.detail-header,
.detail-error,
.detail-error button,
.version-history header,
.version-history li > div,
.rebuild-action,
.rebuild-action button,
.delete-button,
.delete-confirmation,
.confirmation-actions,
.danger-button,
.icon-button {
  display: flex;
  align-items: center;
}

.detail-header,
.delete-confirmation {
  justify-content: space-between;
  gap: var(--space-lg);
}

.detail-header > div {
  display: grid;
  min-width: 0;
  gap: var(--space-2xs);
}

.detail-header span,
.detail-summary dt,
.version-history dt,
.version-history span,
.lifecycle-summary dt {
  color: var(--color-muted);
  font-size: var(--text-sm);
}

.detail-header h2,
.detail-error p,
.detail-summary,
.version-history h3,
.version-history dl,
.lifecycle-summary,
.delete-confirmation p {
  margin: 0;
}

.detail-header h2 {
  color: var(--color-ink);
  font-size: var(--text-lg);
  overflow-wrap: anywhere;
}

.icon-button {
  width: 2.5rem;
  height: 2.5rem;
  flex: 0 0 auto;
  justify-content: center;
  border: var(--rule-thin) solid transparent;
  border-radius: var(--radius-control);
  color: var(--color-muted);
  background: transparent;
}

.detail-summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-md);
}

.detail-summary > div,
.lifecycle-summary > div {
  display: grid;
  gap: var(--space-3xs);
}

.detail-summary dd,
.version-history dd,
.lifecycle-summary dd {
  margin: 0;
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.detail-summary code,
.version-history code {
  color: var(--color-ink-2);
  font-family: var(--font-mono);
}

.version-history {
  display: grid;
  gap: var(--space-sm);
}

.version-history header {
  gap: var(--space-xs);
  color: var(--color-accent-strong);
}

.version-history h3 {
  color: var(--color-ink);
  font-size: var(--text-md);
}

.version-history ol {
  margin: 0;
  padding: 0;
  border-block: var(--rule-thin) solid var(--color-rule);
  list-style: none;
}

.version-history li {
  display: grid;
  grid-template-columns: minmax(12rem, 0.8fr) minmax(0, 1.2fr);
  gap: var(--space-md);
  padding: var(--space-sm) 0;
}

.version-history li + li {
  border-top: var(--rule-thin) solid var(--color-rule);
}

.version-history li > div {
  gap: var(--space-sm);
}

.version-history li strong {
  color: var(--color-success);
  font-size: var(--text-xs);
}

.version-history dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-md);
}

.version-history dl > div {
  display: grid;
  gap: var(--space-3xs);
}

.lifecycle-summary {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-md);
  padding: var(--space-md);
  border-left: var(--rule-focus) solid var(--color-accent);
  background: var(--color-surface);
}

.rebuild-action {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-md);
  padding: var(--space-md);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  background: var(--color-surface);
}

.rebuild-action > div {
  display: grid;
  gap: var(--space-2xs);
}

.rebuild-action strong,
.rebuild-action p {
  margin: 0;
}

.rebuild-action strong {
  color: var(--color-ink);
  font-size: var(--text-sm);
}

.rebuild-action p {
  color: var(--color-muted);
  font-size: var(--text-sm);
  line-height: 1.55;
}

.rebuild-action button {
  flex: 0 0 auto;
  justify-content: center;
  gap: var(--space-xs);
  min-height: 2.5rem;
  padding-inline: var(--space-sm);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-ink-2);
  background: var(--color-paper-2);
  font: inherit;
  font-size: var(--text-sm);
  font-weight: 700;
}

.rebuild-action .rebuild-error {
  grid-column: 1 / -1;
  width: 100%;
  min-width: 0;
  color: var(--color-error);
  overflow-wrap: anywhere;
}

.delete-button,
.detail-error button,
.confirmation-actions button,
.danger-button {
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

.delete-button {
  width: fit-content;
  color: var(--color-error);
}

.delete-confirmation {
  align-items: flex-end;
  padding: var(--space-md);
  border: var(--rule-thin) solid var(--color-error-rule);
  border-radius: var(--radius-control);
  background: var(--color-error-soft);
}

.delete-confirmation > div:first-child {
  display: grid;
  gap: var(--space-xs);
}

.delete-confirmation strong,
.danger-button {
  color: var(--color-error);
}

.delete-confirmation p {
  max-width: 64ch;
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.confirmation-actions {
  flex: 0 0 auto;
  gap: var(--space-sm);
}

.danger-button {
  border-color: var(--color-error-rule);
}

.detail-error {
  align-items: flex-start;
  gap: var(--space-sm);
  color: var(--color-error);
}

.detail-error > div {
  display: grid;
  gap: var(--space-xs);
}

.detail-error p {
  color: var(--color-ink-2);
  font-size: var(--text-sm);
}

.detail-error button {
  width: fit-content;
}

.detail-skeleton {
  display: grid;
  gap: var(--space-sm);
}

.detail-skeleton span {
  height: 4rem;
  background: var(--color-paper-3);
  animation: detail-pulse 1.2s ease-in-out infinite alternate;
}

button:disabled {
  cursor: wait;
  opacity: 0.68;
}

button:focus-visible {
  outline: var(--rule-focus) solid var(--color-focus);
  outline-offset: var(--rule-thin);
}

@keyframes detail-pulse {
  to {
    opacity: 0.45;
  }
}

@media (max-width: 48rem) {
  .detail-summary,
  .version-history li,
  .version-history dl,
  .lifecycle-summary {
    grid-template-columns: minmax(0, 1fr);
  }

  .delete-confirmation {
    align-items: stretch;
    flex-direction: column;
  }

  .rebuild-action {
    align-items: stretch;
    grid-template-columns: minmax(0, 1fr);
  }

  .rebuild-action button {
    width: 100%;
  }

  .confirmation-actions {
    justify-content: flex-end;
  }
}

@media (max-width: 32rem) {
  .confirmation-actions {
    align-items: stretch;
    flex-direction: column-reverse;
  }

  .confirmation-actions button {
    width: 100%;
  }
}

@media (hover: hover) and (pointer: fine) {
  .icon-button:hover:not(:disabled),
  .detail-error button:hover:not(:disabled),
  .rebuild-action button:hover:not(:disabled),
  .confirmation-actions button:hover:not(:disabled),
  .delete-button:hover:not(:disabled) {
    border-color: var(--color-accent);
    background: var(--color-accent-soft);
  }

  .danger-button:hover:not(:disabled),
  .delete-button:hover:not(:disabled) {
    border-color: var(--color-error-rule);
    background: var(--color-error-soft);
  }
}

@media (prefers-reduced-motion: reduce) {
  .detail-skeleton span {
    animation: none;
  }
}
</style>
