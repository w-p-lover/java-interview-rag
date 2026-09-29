<script setup lang="ts">
import WorkspaceHeader from "@/components/WorkspaceHeader.vue";
import RetrievalExperimentPanel from "@/components/evaluation/RetrievalExperimentPanel.vue";
import AnswerExperimentPanel from "@/components/evaluation/AnswerExperimentPanel.vue";
</script>

<template>
  <WorkspaceHeader mode="evaluations" />
  <main class="evaluation-workspace">
    <RetrievalExperimentPanel />
    <AnswerExperimentPanel />
  </main>
</template>

<style scoped>
.evaluation-workspace { margin-left: var(--workspace-rail); padding: var(--space-xl) clamp(var(--space-lg), 4vw, var(--space-3xl)) var(--space-2xl); }
@media (max-width: 68rem) { .evaluation-workspace { margin-left: 0; } }
@media (max-width: 42rem) { .evaluation-workspace { width: calc(100% - (var(--space-md) * 2)); margin-inline: auto; padding: var(--space-lg) 0 var(--space-xl); } }
</style>
