<script setup lang="ts">
import type { KnowledgeDocumentTaskResponse } from "@/api/knowledge";

defineProps<{
  task: KnowledgeDocumentTaskResponse | null;
  error: string;
}>();

const emit = defineEmits<{
  cancel: [];
}>();
</script>

<template>
  <section v-if="task || error" class="task-panel" aria-live="polite">
    <div v-if="task" class="task-heading">
      <div>
        <strong>资料索引任务</strong>
        <span>{{ task.taskId }}</span>
      </div>
      <b :class="`task-status task-status--${task.status.toLowerCase()}`">
        {{ task.status === "QUEUED" ? "排队中" : task.status === "RUNNING" ? "处理中" : task.status === "SUCCEEDED" ? "已完成" : task.status === "CANCELLED" ? "已取消" : "处理失败" }}
      </b>
    </div>
    <p v-if="task && task.status === 'SUCCEEDED'">
      已完成 {{ task.operation === "IMPORT" ? "资料导入" : "当前版本重建" }}，生成 {{ task.chunkCount }} 个切片{{ task.warningCount ? `，${task.warningCount} 条解析告警` : "" }}。正文不会保存在任务记录中。
    </p>
    <p v-else-if="task && task.status === 'FAILED'" class="task-error">
      任务失败（{{ task.errorType || "UNKNOWN" }}），请重新选择文件后重试；上传正文不会被保留。
    </p>
    <p v-else-if="task && task.status === 'CANCELLED'" class="task-error">
      已取消排队中的任务，资料正文没有写入索引；如需处理请重新提交。
    </p>
    <p v-else-if="task">任务正在后台执行，页面会自动刷新状态。</p>
    <button v-if="task?.status === 'QUEUED'" type="button" class="cancel-button" @click="emit('cancel')">取消排队任务</button>
    <p v-if="error" class="task-error">{{ error }}</p>
  </section>
</template>

<style scoped>
.task-panel { display: grid; gap: var(--space-xs); margin-top: var(--space-md); padding: var(--space-md); border-left: 3px solid var(--color-accent); background: var(--color-paper-2); }
.task-heading { display: flex; align-items: center; justify-content: space-between; gap: var(--space-md); }
.task-heading > div { display: grid; gap: var(--space-2xs); }
.task-heading span { color: var(--color-muted); font-family: var(--font-mono); font-size: var(--text-xs); overflow-wrap: anywhere; }
.task-panel p { margin: 0; color: var(--color-muted); font-size: var(--text-sm); line-height: 1.5; }
.task-error { color: var(--color-error) !important; }
.task-status { padding: var(--space-2xs) var(--space-xs); border-radius: var(--radius-round); font-size: var(--text-xs); }
.task-status--queued, .task-status--running { color: var(--color-warning); background: var(--color-warning-soft); }
.task-status--succeeded { color: var(--color-success); background: var(--color-success-soft); }
.task-status--failed, .task-status--cancelled { color: var(--color-error); background: var(--color-error-soft); }
.cancel-button { justify-self: start; min-height: 2.25rem; padding: var(--space-xs) var(--space-sm); border: var(--rule-thin) solid var(--color-error-rule); border-radius: var(--radius-control); color: var(--color-error); background: transparent; font-size: var(--text-xs); }
</style>
