<script setup lang="ts">
import { shallowRef, useTemplateRef } from "vue";
import { RefreshCw } from "@lucide/vue";

import WorkspaceHeader from "@/components/WorkspaceHeader.vue";
import ResumeRecordList from "@/components/resume/ResumeRecordList.vue";
import ResumeFactComparison from "@/components/resume/ResumeFactComparison.vue";
import ResumeUploadForm from "@/components/resume/ResumeUploadForm.vue";
import ResumeVersionHistory from "@/components/resume/ResumeVersionHistory.vue";
import { useResumeCenter } from "@/composables/useResumeCenter";
import type { ResumeUploadPayload } from "@/api/resumeCenter";

const center = useResumeCenter();
const mode = shallowRef<"new" | "append">("new");
const uploadForm = useTemplateRef<InstanceType<typeof ResumeUploadForm>>("uploadForm");

async function upload(payload: ResumeUploadPayload): Promise<void> {
  const result = await center.upload(payload);
  if (result) {
    uploadForm.value?.clearFile();
    mode.value = "append";
  }
}

function select(id: string): void {
  mode.value = "append";
  void center.select(id);
}
</script>

<template>
  <WorkspaceHeader mode="resumes" />
  <main id="main-content" class="resume-center" tabindex="-1">
    <header class="page-heading">
      <div><h1>简历中心</h1><p>版本、岗位与正文保存边界</p></div>
      <button type="button" class="icon-action" :disabled="center.loading.value || center.busy.value" aria-label="刷新简历中心" title="刷新" @click="center.loadRecords()"><RefreshCw :size="19" aria-hidden="true" /></button>
    </header>

    <ResumeUploadForm ref="uploadForm" v-model:mode="mode" :selected-record="center.selectedRecord.value" :busy="center.saving.value" @submit="upload" />
    <p v-if="center.error.value" class="error-message" role="alert">{{ center.error.value }}</p>
    <p v-if="center.actionMessage.value" class="success-message" role="status">{{ center.actionMessage.value }}</p>

    <div class="resume-grid">
      <ResumeRecordList :records="center.records.value" :selected-id="center.selectedId.value" :loading="center.loading.value" @select="select" />
      <ResumeVersionHistory :detail="center.detail.value" :loading="center.detailLoading.value" :switching-version-id="center.switchingVersionId.value" @switch="center.switchCurrent" />
    </div>
    <ResumeFactComparison :detail="center.detail.value" />
  </main>
</template>

<style scoped lang="scss">
.resume-center { box-sizing: border-box; width: min(calc(100% - var(--workspace-rail)), 82rem); margin-left: calc(var(--workspace-rail) + max(0rem, (100vw - var(--workspace-rail) - 82rem) / 2)); padding: var(--space-xl) var(--space-xl) var(--space-3xl); }
.page-heading { display: flex; align-items: center; justify-content: space-between; gap: var(--space-md); padding-bottom: var(--space-lg); border-bottom: var(--rule-thin) solid var(--color-rule-2); }
.page-heading h1, .page-heading p { margin: 0; }
.page-heading h1 { font-size: var(--text-2xl); letter-spacing: 0; }
.page-heading p { margin-top: var(--space-2xs); color: var(--color-muted); font-size: var(--text-sm); }
.icon-action { display: inline-grid; width: 2.75rem; height: 2.75rem; place-items: center; border: var(--rule-thin) solid var(--color-rule-2); border-radius: var(--radius-control); color: var(--color-ink-2); background: var(--color-surface); }
.resume-grid { display: grid; grid-template-columns: minmax(17rem, 0.38fr) minmax(0, 1fr); gap: var(--space-2xl); padding-top: var(--space-xl); }
.error-message, .success-message { margin: var(--space-md) 0 0; padding: var(--space-sm) var(--space-md); border: var(--rule-thin) solid; font-size: var(--text-sm); }
.error-message { border-color: var(--color-error-rule); color: var(--color-error); background: var(--color-error-soft); }
.success-message { border-color: var(--color-success-rule); color: var(--color-success); background: var(--color-success-soft); }
@media (max-width: 68rem) { .resume-center { width: 100%; margin-left: 0; } .resume-grid { grid-template-columns: minmax(0, 1fr); gap: var(--space-xl); } }
@media (max-width: 48rem) { .resume-center { width: calc(100% - (var(--space-md) * 2)); padding-top: var(--space-lg); } }
</style>
