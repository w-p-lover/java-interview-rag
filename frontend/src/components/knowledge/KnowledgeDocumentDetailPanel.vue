<script setup lang="ts">
import { shallowRef, useTemplateRef, watch } from "vue";
import { CircleAlert, History, RefreshCw, RotateCw, Settings2, ShieldCheck, Trash2, X } from "@lucide/vue";

import type { KnowledgeDocumentDetailsResponse, KnowledgeIndexConsistencyResponse } from "@/api/knowledge";
import KnowledgeDocumentOutline from "@/components/knowledge/KnowledgeDocumentOutline.vue";

const props = defineProps<{
  source: string;
  details: KnowledgeDocumentDetailsResponse | null;
  loading: boolean;
  error: string;
  deleting: boolean;
  deleteError: string;
  rebuilding: boolean;
  rebuildError: string;
  consistency: KnowledgeIndexConsistencyResponse | null;
  consistencyLoading: boolean;
  consistencyError: string;
}>();

const emit = defineEmits<{
  close: [];
  retry: [];
  retire: [];
  rebuild: [file: File];
  inspectConsistency: [];
  search: [query: string];
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

function storeStatusLabel(status: string): string {
  return {
    CONSISTENT: "一致",
    MISSING_CHUNKS: "缺少 Chunk",
    EXCESS_CHUNKS: "Chunk 超额",
    STALE_VERSIONS: "存在旧版本",
    UNVERIFIABLE: "无法核验",
    UNAVAILABLE: "存储不可用",
  }[status] || status;
}

function indexingStatusLabel(status: string | null): string {
  return {
    INDEXING: "索引中",
    READY: "已就绪",
    FAILED: "索引失败",
  }[status || ""] || "未追踪";
}

function versionStatusLabel(status: string): string {
  return {
    MATCHED: "契约匹配",
    PARTIAL_MISMATCH: "部分契约不匹配",
    ALL_MISMATCH: "契约全部不匹配",
    UNVERIFIABLE: "契约无法核验",
  }[status] || status;
}

function chunkCountLabel(value: number | null): string {
  return value === null ? "无法核验" : `${value} 个`;
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

      <div class="detail-body">
        <div class="detail-primary">
          <KnowledgeDocumentOutline
            :outline="details.outline"
            :disabled="deleting || rebuilding"
            @search="emit('search', $event)"
          />
        </div>

        <aside class="detail-secondary" aria-label="版本与资料维护">
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

          <section class="maintenance" aria-labelledby="maintenance-title">
            <header>
              <Settings2 :size="18" aria-hidden="true" />
              <h3 id="maintenance-title">资料维护</h3>
            </header>

            <div class="consistency-check" aria-live="polite">
              <div class="consistency-heading">
                <div>
                  <strong>索引一致性</strong>
                  <p>只读比较目录期望 Chunk、Dense/Qdrant、Sparse/Lucene 和编排状态。</p>
                </div>
                <button
                  type="button"
                  :disabled="deleting || rebuilding || consistencyLoading"
                  @click="emit('inspectConsistency')"
                >
                  <ShieldCheck :size="17" aria-hidden="true" />
                  <span>{{ consistencyLoading ? "检查中" : "检查索引" }}</span>
                </button>
              </div>
              <p v-if="consistencyError" class="consistency-error" role="alert">{{ consistencyError }}</p>
              <div v-else-if="consistency" class="consistency-result">
                <p :class="['consistency-overall', { 'is-consistent': consistency.consistent }]">
                  <ShieldCheck :size="16" aria-hidden="true" />
                  {{ consistency.consistent ? "当前索引与目录一致" : "发现不一致，请人工确认后再重建" }}
                </p>
                <dl>
                  <div>
                    <dt>Dense / Qdrant</dt>
                    <dd>{{ storeStatusLabel(consistency.dense.status) }}</dd>
                  </div>
                  <div>
                    <dt>Sparse / Lucene</dt>
                    <dd>{{ storeStatusLabel(consistency.sparse.status) }}</dd>
                  </div>
                  <div>
                    <dt>编排状态</dt>
                    <dd>{{ indexingStatusLabel(consistency.indexingState.status) }}</dd>
                  </div>
                  <div>
                    <dt>期望 Chunk</dt>
                    <dd>{{ chunkCountLabel(consistency.expectedChunkCount) }}</dd>
                  </div>
                </dl>
                <div class="store-diagnostics">
                  <section>
                    <strong>Dense / Qdrant 明细</strong>
                    <dl>
                      <div>
                        <dt>当前版本</dt>
                        <dd>{{ chunkCountLabel(consistency.dense.currentVersionChunkCount) }}</dd>
                      </div>
                      <div>
                        <dt>来源总数</dt>
                        <dd>{{ chunkCountLabel(consistency.dense.sourceChunkCount) }}</dd>
                      </div>
                      <div>
                        <dt>旧版本残留</dt>
                        <dd>{{ chunkCountLabel(consistency.dense.staleVersionChunkCount) }}</dd>
                      </div>
                      <div>
                        <dt>版本契约</dt>
                        <dd>{{ versionStatusLabel(consistency.dense.indexVersionStatus) }}</dd>
                      </div>
                    </dl>
                    <p v-if="consistency.dense.errorType" class="store-error">
                      存储错误：{{ consistency.dense.errorType }}
                    </p>
                  </section>
                  <section>
                    <strong>Sparse / Lucene 明细</strong>
                    <dl>
                      <div>
                        <dt>当前版本</dt>
                        <dd>{{ chunkCountLabel(consistency.sparse.currentVersionChunkCount) }}</dd>
                      </div>
                      <div>
                        <dt>来源总数</dt>
                        <dd>{{ chunkCountLabel(consistency.sparse.sourceChunkCount) }}</dd>
                      </div>
                      <div>
                        <dt>旧版本残留</dt>
                        <dd>{{ chunkCountLabel(consistency.sparse.staleVersionChunkCount) }}</dd>
                      </div>
                      <div>
                        <dt>版本契约</dt>
                        <dd>{{ versionStatusLabel(consistency.sparse.indexVersionStatus) }}</dd>
                      </div>
                    </dl>
                    <p v-if="consistency.sparse.errorType" class="store-error">
                      存储错误：{{ consistency.sparse.errorType }}
                    </p>
                  </section>
                </div>
                <p v-if="consistency.indexingState.errorType" class="store-error">
                  最近编排错误：{{ consistency.indexingState.errorType }}
                </p>
                <p v-if="!consistency.consistent" class="consistency-guidance">
                  请先确认异常原因，再选择下方“重建当前版本”，并提供内容一致的原文件；不会自动删除或修复索引。
                </p>
              </div>
            </div>

            <dl class="lifecycle-summary">
              <div>
                <dt>版本替换</dt>
                <dd>新版本入库后，旧向量退出检索，历史登记继续保留。</dd>
              </div>
              <div>
                <dt>人工淘汰</dt>
                <dd>删除全部向量和版本登记，页面内无法撤销。</dd>
              </div>
            </dl>

            <div class="rebuild-action">
              <div>
                <strong>重建当前版本</strong>
                <p>重新选择当前原文件；内容不一致时服务端会拒绝。</p>
              </div>
              <button type="button" :disabled="deleting || rebuilding" @click="chooseRebuildFile">
                <RotateCw :size="17" aria-hidden="true" />
                <span>{{ rebuilding ? "正在重建" : "选择原文件" }}</span>
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
                <p>将删除“{{ details.source }}”的全部向量和 {{ details.versionCount }} 条版本登记。</p>
                <p v-if="deleteError" class="delete-error" role="alert">{{ deleteError }}</p>
                <p v-if="deleteError && consistency" class="delete-guidance">
                  {{ consistency.consistent ? "巡检显示当前索引已收敛，可再次确认淘汰。" : "巡检显示仍可能存在索引残留，请确认后重试淘汰。" }}
                </p>
              </div>
              <div class="confirmation-actions">
                <button type="button" :disabled="deleting || rebuilding" @click="confirmingDelete = false">取消</button>
                <button class="danger-button" type="button" :disabled="deleting || rebuilding" @click="emit('retire')">
                  <Trash2 :size="17" aria-hidden="true" />
                  <span>{{ deleting ? "正在淘汰" : deleteError ? "重试淘汰" : "确认淘汰" }}</span>
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
          </section>
        </aside>
      </div>
    </template>
  </section>
</template>

<style scoped lang="scss">
.detail-panel {
  display: grid;
  gap: var(--space-md);
  min-width: 0;
  padding: var(--space-lg);
  border-block: var(--rule-thin) solid var(--color-rule-2);
  background: var(--color-paper-2);
  container-type: inline-size;
}

.detail-header,
.detail-error,
.detail-error button,
.version-history header,
.maintenance header,
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

.delete-confirmation .delete-error {
  color: var(--color-error);
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

.detail-body {
  display: grid;
  grid-template-columns: minmax(20rem, 1.35fr) minmax(16rem, 0.65fr);
  align-items: start;
  gap: var(--space-xl);
  padding-top: var(--space-sm);
  border-top: var(--rule-thin) solid var(--color-rule);
}

.detail-primary,
.detail-secondary {
  min-width: 0;
}

.detail-secondary {
  display: grid;
  gap: var(--space-xl);
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

.maintenance {
  display: grid;
  gap: var(--space-md);
  padding-top: var(--space-lg);
  border-top: var(--rule-thin) solid var(--color-rule-2);
}

.consistency-check {
  display: grid;
  gap: var(--space-sm);
  padding: var(--space-md);
  border: var(--rule-thin) solid var(--color-rule);
  border-radius: var(--radius-control);
  background: var(--color-surface);
}

.consistency-heading,
.consistency-heading > div,
.consistency-result,
.consistency-result dl,
.consistency-result dl > div {
  display: grid;
}

.consistency-heading {
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: var(--space-sm);
}

.consistency-heading > div,
.consistency-result,
.consistency-result dl,
.consistency-result dl > div {
  gap: var(--space-2xs);
}

.consistency-heading strong,
.consistency-heading p,
.consistency-error,
.consistency-overall,
.consistency-guidance,
.consistency-result dl,
.consistency-result dt,
.consistency-result dd {
  margin: 0;
}

.consistency-heading strong,
.consistency-result dd {
  color: var(--color-ink);
  font-size: var(--text-sm);
}

.consistency-heading p,
.consistency-error,
.consistency-guidance,
.consistency-result dt,
.consistency-result dd {
  font-size: var(--text-xs);
  line-height: 1.55;
}

.consistency-heading p,
.consistency-result dt {
  color: var(--color-muted);
}

.consistency-heading button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-xs);
  min-height: 2.25rem;
  padding-inline: var(--space-sm);
  border: var(--rule-thin) solid var(--color-rule-2);
  border-radius: var(--radius-control);
  color: var(--color-ink-2);
  background: var(--color-surface);
  font: inherit;
  font-size: var(--text-xs);
  font-weight: 700;
}

.consistency-overall {
  display: inline-flex;
  align-items: center;
  gap: var(--space-xs);
  color: var(--color-warning-strong, var(--color-warning));
  font-size: var(--text-sm);
  font-weight: 700;
}

.consistency-overall.is-consistent {
  color: var(--color-success);
}

.consistency-result dl {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-sm);
  padding-top: var(--space-xs);
}

.store-diagnostics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-sm);
  padding-top: var(--space-sm);
}

.store-diagnostics section {
  display: grid;
  gap: var(--space-xs);
  padding: var(--space-sm);
  border: var(--rule-thin) solid var(--color-rule);
  border-radius: var(--radius-control);
  background: var(--color-paper-2);
}

.store-diagnostics strong {
  color: var(--color-ink);
  font-size: var(--text-xs);
}

.store-diagnostics dl {
  grid-template-columns: minmax(0, 1fr);
  padding-top: 0;
}

.store-error {
  margin: 0;
  color: var(--color-error);
  font-size: var(--text-xs);
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.consistency-error {
  color: var(--color-error);
}

.consistency-guidance {
  color: var(--color-warning-strong, var(--color-warning));
}

.maintenance header {
  gap: var(--space-xs);
  color: var(--color-accent-strong);
}

.maintenance h3 {
  margin: 0;
  color: var(--color-ink);
  font-size: var(--text-md);
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
  gap: var(--space-sm);
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
  gap: var(--space-sm);
}

.version-history dl > div {
  display: grid;
  gap: var(--space-3xs);
}

.lifecycle-summary {
  display: grid;
  gap: var(--space-sm);
  padding: 0;
}

.rebuild-action {
  display: grid;
  gap: var(--space-sm);
  padding-top: var(--space-md);
  border-top: var(--rule-thin) solid var(--color-rule);
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
  width: fit-content;
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
  width: 100%;
  color: var(--color-error);
}

.delete-confirmation {
  align-items: stretch;
  flex-direction: column;
  gap: var(--space-md);
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
  justify-content: flex-end;
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

@container (max-width: 46rem) {
  .detail-body {
    grid-template-columns: minmax(0, 1fr);
  }

  .detail-summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@container (max-width: 32rem) {
  .detail-summary,
  .version-history dl {
    grid-template-columns: minmax(0, 1fr);
  }

  .rebuild-action button {
    width: 100%;
  }

  .consistency-heading {
    grid-template-columns: minmax(0, 1fr);
  }

  .consistency-heading button {
    width: 100%;
  }

  .consistency-result dl {
    grid-template-columns: minmax(0, 1fr);
  }

  .store-diagnostics {
    grid-template-columns: minmax(0, 1fr);
  }

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
