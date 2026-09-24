<script setup lang="ts">
import { computed, reactive, watch } from "vue";
import { Save } from "@lucide/vue";

import type { ModelProvider, ModelProviderDefaultsRequest } from "@/api/modelProvider";

const props = defineProps<{
  providers: readonly ModelProvider[];
  chatProviderId: string;
  embeddingProviderId: string;
  version: number;
  busy: boolean;
}>();
const emit = defineEmits<{ submit: [payload: ModelProviderDefaultsRequest] }>();
const selection = reactive({ chatProviderId: "", embeddingProviderId: "" });
const chatProviders = computed(() => props.providers.filter(provider => provider.enabled && provider.chatModel));
const embeddingProviders = computed(() => props.providers.filter(
  provider => provider.enabled && provider.embeddingModel,
));

watch(() => [props.chatProviderId, props.embeddingProviderId], ([chat, embedding]) => {
  selection.chatProviderId = chat ?? "";
  selection.embeddingProviderId = embedding ?? "";
}, { immediate: true });

function submit(): void {
  emit("submit", { ...selection, version: props.version });
}
</script>

<template>
  <form class="defaults" @submit.prevent="submit">
    <div class="defaults-copy">
      <span>业务默认值</span>
      <p>切换会应用到后续模型调用；运行中的长任务存在时后端会拒绝。</p>
    </div>
    <label><span>Chat</span><select v-model="selection.chatProviderId" :disabled="busy"><option v-for="item in chatProviders" :key="item.id" :value="item.id">{{ item.name }} · {{ item.chatModel }}</option></select></label>
    <label><span>Embedding</span><select v-model="selection.embeddingProviderId" :disabled="busy"><option v-for="item in embeddingProviders" :key="item.id" :value="item.id">{{ item.name }} · {{ item.embeddingModel }} / {{ item.embeddingDimensions }} 维</option></select></label>
    <button class="save-button" type="submit" :disabled="busy"><Save :size="16" aria-hidden="true" />{{ busy ? "保存中" : "保存默认值" }}</button>
  </form>
</template>

<style scoped lang="scss">
.defaults { display: grid; grid-template-columns: minmax(12rem, 1fr) repeat(2, minmax(12rem, 1fr)) auto; align-items: end; gap: var(--space-md); padding-block: var(--space-lg); border-bottom: var(--rule-thin) solid var(--color-rule-2); }
.defaults-copy { align-self: center; }
.defaults-copy span { color: var(--color-ink-2); font-size: var(--text-sm); font-weight: 750; }
.defaults-copy p { max-width: 34ch; margin: var(--space-3xs) 0 0; color: var(--color-muted); font-size: var(--text-xs); }
.defaults label { display: grid; gap: var(--space-xs); color: var(--color-ink-2); font-size: var(--text-xs); font-weight: 650; }
.defaults select { width: 100%; min-width: 0; height: 2.75rem; padding-inline: var(--space-sm); border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); color: var(--color-ink); background: var(--color-surface); font-size: var(--text-sm); }
.save-button { display: inline-flex; align-items: center; justify-content: center; gap: var(--space-xs); min-height: 2.75rem; padding-inline: var(--space-md); border: var(--rule-thin) solid var(--color-accent); border-radius: var(--radius-control); color: var(--color-accent-ink); background: var(--color-accent); font-weight: 700; white-space: nowrap; }
@media (max-width: 64rem) { .defaults { grid-template-columns: repeat(2, minmax(0, 1fr)); } .defaults-copy { grid-column: 1 / -1; } }
@media (max-width: 42rem) { .defaults { grid-template-columns: 1fr; } .defaults-copy { grid-column: auto; } }
</style>
