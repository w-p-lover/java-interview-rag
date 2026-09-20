import { computed, onMounted, onUnmounted, readonly, shallowRef } from "vue";

import { resolveApiError } from "@/api/request";
import {
  getResumeDetail,
  listResumes,
  switchResumeCurrentVersion,
  uploadResumeVersion,
  type ResumeDetail,
  type ResumeSaveResult,
  type ResumeSummary,
  type ResumeUploadPayload,
} from "@/api/resumeCenter";

export function useResumeCenter() {
  const records = shallowRef<ResumeSummary[]>([]);
  const selectedId = shallowRef("");
  const detail = shallowRef<ResumeDetail | null>(null);
  const loading = shallowRef(false);
  const detailLoading = shallowRef(false);
  const saving = shallowRef(false);
  const switchingVersionId = shallowRef("");
  const error = shallowRef("");
  const actionMessage = shallowRef("");
  let disposed = false;
  let listController: AbortController | null = null;
  let detailController: AbortController | null = null;

  const selectedRecord = computed(() => records.value.find((item) => item.id === selectedId.value) ?? null);
  const busy = computed(() => saving.value || Boolean(switchingVersionId.value));

  async function loadRecords(preferredId = selectedId.value): Promise<void> {
    listController?.abort();
    const controller = new AbortController();
    listController = controller;
    loading.value = true;
    error.value = "";
    try {
      const result = await listResumes(controller.signal);
      if (disposed) return;
      records.value = result;
      const nextId = result.some((item) => item.id === preferredId)
        ? preferredId : result[0]?.id ?? "";
      if (nextId) await select(nextId);
      else {
        selectedId.value = "";
        detail.value = null;
      }
    } catch (requestError) {
      if (!disposed && !controller.signal.aborted) {
        error.value = resolveApiError(requestError, "读取简历记录失败");
      }
    } finally {
      if (!disposed) loading.value = false;
    }
  }

  async function select(id: string): Promise<void> {
    if (!id || (selectedId.value === id && detail.value?.id === id)) return;
    detailController?.abort();
    const controller = new AbortController();
    detailController = controller;
    selectedId.value = id;
    detailLoading.value = true;
    error.value = "";
    try {
      const result = await getResumeDetail(id, controller.signal);
      if (!disposed && selectedId.value === id) detail.value = result;
    } catch (requestError) {
      if (!disposed && !controller.signal.aborted) {
        error.value = resolveApiError(requestError, "读取简历版本失败");
      }
    } finally {
      if (!disposed && selectedId.value === id) detailLoading.value = false;
    }
  }

  async function upload(payload: ResumeUploadPayload): Promise<ResumeSaveResult | null> {
    if (saving.value) return null;
    saving.value = true;
    error.value = "";
    actionMessage.value = "";
    try {
      const result = await uploadResumeVersion(payload);
      if (disposed) return null;
      selectedId.value = result.detail.id;
      detail.value = result.detail;
      actionMessage.value = result.duplicate
        ? `已存在相同文件，定位到第 ${result.detail.versions.find((item) => item.id === result.savedVersionId)?.versionNumber ?? "-"} 版。`
        : `已保存第 ${result.detail.versions.find((item) => item.id === result.savedVersionId)?.versionNumber ?? "-"} 版。`;
      await loadRecords(result.detail.id);
      return result;
    } catch (requestError) {
      if (!disposed) error.value = resolveApiError(requestError, "保存简历版本失败");
      return null;
    } finally {
      if (!disposed) saving.value = false;
    }
  }

  async function switchCurrent(versionId: string): Promise<void> {
    if (!detail.value || switchingVersionId.value) return;
    switchingVersionId.value = versionId;
    error.value = "";
    actionMessage.value = "";
    try {
      const result = await switchResumeCurrentVersion(
        detail.value.id, versionId, detail.value.version,
      );
      if (disposed) return;
      detail.value = result;
      actionMessage.value = `当前版本已切换为第 ${result.versions.find((item) => item.id === versionId)?.versionNumber ?? "-"} 版。`;
      await loadRecords(result.id);
    } catch (requestError) {
      if (!disposed) error.value = resolveApiError(requestError, "切换当前版本失败");
    } finally {
      if (!disposed) switchingVersionId.value = "";
    }
  }

  onMounted(() => void loadRecords());
  onUnmounted(() => {
    disposed = true;
    listController?.abort();
    detailController?.abort();
  });

  return {
    records: readonly(records), selectedId, detail: readonly(detail),
    selectedRecord, loading, detailLoading, saving, switchingVersionId,
    busy, error, actionMessage,
    loadRecords, select, upload, switchCurrent,
  };
}
