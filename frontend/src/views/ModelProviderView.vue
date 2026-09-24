<script setup lang="ts">
import WorkspaceHeader from "@/components/WorkspaceHeader.vue";
import ModelProviderPanel from "@/components/model-provider/ModelProviderPanel.vue";
</script>

<template>
  <WorkspaceHeader mode="model-providers" />
  <main class="provider-workspace">
    <ModelProviderPanel />
  </main>
</template>

<style scoped>
.provider-workspace {
  margin-left: var(--workspace-rail);
  padding: var(--space-xl) clamp(var(--space-lg), 4vw, var(--space-3xl)) var(--space-2xl);
}

@media (max-width: 68rem) {
  .provider-workspace { margin-left: 0; }
}

@media (max-width: 42rem) {
  .provider-workspace {
    width: calc(100% - (var(--space-md) * 2));
    margin-inline: auto;
    padding: var(--space-lg) 0 var(--space-xl);
  }
}
</style>
