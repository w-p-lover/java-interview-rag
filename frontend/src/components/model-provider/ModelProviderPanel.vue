<script setup lang="ts">
import { Plus, RefreshCw } from "@lucide/vue";
import { shallowRef } from "vue";

import type { ModelCapability, ModelProvider, ModelProviderDefaultsRequest, ModelProviderRequest } from "@/api/modelProvider";
import { useModelProviders } from "@/composables/useModelProviders";
import ModelProviderDefaults from "./ModelProviderDefaults.vue";
import ModelProviderForm from "./ModelProviderForm.vue";
import ModelProviderList from "./ModelProviderList.vue";

const {
  overview, loading, loadError, actionKey, actionError, testResults, testErrors,
  refresh, saveProvider, removeProvider, saveDefaults, runTest,
} = useModelProviders();
const formOpen = shallowRef(false);
const editingProvider = shallowRef<ModelProvider | null>(null);

function openCreate(): void {
  editingProvider.value = null;
  formOpen.value = true;
}

function openEdit(provider: ModelProvider): void {
  editingProvider.value = provider;
  formOpen.value = true;
}

async function submitProvider(payload: ModelProviderRequest, editing: boolean): Promise<void> {
  if (await saveProvider(payload, editing)) {
    formOpen.value = false;
    editingProvider.value = null;
  }
}

async function toggleProvider(payload: ModelProviderRequest): Promise<void> {
  await saveProvider(payload, true);
}

async function confirmDelete(provider: ModelProvider): Promise<void> {
  if (window.confirm(`确认删除 Provider“${provider.name}”？`)) {
    await removeProvider(provider.id, provider.version);
  }
}

async function submitDefaults(payload: ModelProviderDefaultsRequest): Promise<void> {
  await saveDefaults(payload);
}

function testProvider(id: string, capability: ModelCapability): void {
  void runTest(id, capability);
}
</script>

<template>
  <section id="model-provider-overview" class="provider-panel" aria-labelledby="provider-title">
    <header class="provider-heading">
      <div><span>运行时配置中心</span><h1 id="provider-title">模型 Provider</h1></div>
      <div class="heading-actions">
        <button class="icon-button" type="button" :disabled="loading || Boolean(actionKey)" title="刷新 Provider" @click="refresh"><RefreshCw :size="17" aria-hidden="true" :class="{ spinning: loading }" /></button>
        <button class="primary-button" type="button" :disabled="Boolean(actionKey)" @click="openCreate"><Plus :size="17" aria-hidden="true" />新增 Provider</button>
      </div>
    </header>

    <p v-if="loadError" class="error-banner" role="alert">{{ loadError }}</p>
    <p v-if="actionError" class="error-banner" role="alert">{{ actionError }}</p>
    <div v-if="loading && !overview" class="loading-state">读取 Provider 配置</div>

    <template v-if="overview">
      <ModelProviderForm v-if="formOpen" :provider="editingProvider" :busy="actionKey.startsWith('save:')" @submit="submitProvider" @cancel="formOpen = false" />
      <ModelProviderDefaults :providers="overview.providers" :chat-provider-id="overview.defaultChatProviderId" :embedding-provider-id="overview.defaultEmbeddingProviderId" :version="overview.defaultsVersion" :busy="actionKey === 'defaults'" @submit="submitDefaults" />
      <ModelProviderList :providers="overview.providers" :action-key="actionKey" :test-results="testResults" :test-errors="testErrors" @edit="openEdit" @test="testProvider" @toggle="toggleProvider" @remove="confirmDelete" />
      <footer class="snapshot-time">配置快照 {{ new Intl.DateTimeFormat('zh-CN', { dateStyle: 'short', timeStyle: 'medium' }).format(new Date(overview.generatedAt)) }}</footer>
    </template>
  </section>
</template>

<style scoped lang="scss">
.provider-panel { display: grid; gap: var(--space-lg); }
.provider-heading, .heading-actions { display: flex; align-items: center; }
.provider-heading { justify-content: space-between; gap: var(--space-lg); padding-bottom: var(--space-lg); border-bottom: var(--rule-thin) solid var(--color-rule-2); }
.provider-heading > div:first-child { display: grid; gap: var(--space-2xs); }
.provider-heading span, .snapshot-time { color: var(--color-muted); font-size: var(--text-sm); }
.provider-heading h1 { margin: 0; color: var(--color-ink); font-family: var(--font-display); font-size: var(--text-2xl); }
.heading-actions { gap: var(--space-xs); }
.icon-button { display: inline-grid; width: 2.5rem; height: 2.5rem; place-items: center; border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); background: var(--color-surface); }
.primary-button { display: inline-flex; align-items: center; gap: var(--space-xs); min-height: 2.5rem; padding-inline: var(--space-md); border: var(--rule-thin) solid var(--color-ink); border-radius: var(--radius-control); color: var(--color-graphite-text); background: var(--color-graphite); font-weight: 700; }
.error-banner { margin: 0; padding: var(--space-sm) var(--space-md); border: var(--rule-thin) solid var(--color-error-rule); border-radius: var(--radius-control); color: var(--color-error); background: var(--color-error-soft); font-size: var(--text-sm); }
.loading-state { min-height: 15rem; display: grid; place-items: center; color: var(--color-muted); }
.snapshot-time { padding-top: var(--space-sm); }
.spinning { animation: spin 0.9s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
@media (max-width: 42rem) { .provider-heading { align-items: stretch; flex-direction: column; } .heading-actions { display: grid; grid-template-columns: auto minmax(0, 1fr); } .primary-button { justify-content: center; } }
@media (prefers-reduced-motion: reduce) { .spinning { animation: none; } }
</style>
