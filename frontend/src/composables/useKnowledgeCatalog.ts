import { onMounted, shallowRef } from "vue";

import {
  deleteKnowledgeDocument,
  getKnowledgeDocumentDetails,
  listKnowledgeDocuments,
  type KnowledgeDocumentCatalogResponse,
  type KnowledgeDocumentDetailsResponse,
} from "@/api/knowledge";
import { resolveApiError } from "@/api/request";

export function useKnowledgeCatalog() {
  const result = shallowRef<KnowledgeDocumentCatalogResponse | null>(null);
  const loading = shallowRef(false);
  const error = shallowRef("");
  const selectedSource = shallowRef("");
  const details = shallowRef<KnowledgeDocumentDetailsResponse | null>(null);
  const detailsLoading = shallowRef(false);
  const detailsError = shallowRef("");
  const deleting = shallowRef(false);
  const notice = shallowRef("");
  let detailsRequestId = 0;

  async function load(): Promise<void> {
    loading.value = true;
    error.value = "";
    try {
      result.value = await listKnowledgeDocuments();
      if (selectedSource.value
        && !result.value.documents.some(document => document.source === selectedSource.value)) {
        closeDetails();
      }
    } catch (requestError) {
      error.value = resolveApiError(requestError, "资料目录读取失败");
    } finally {
      loading.value = false;
    }
  }

  async function refresh(): Promise<void> {
    const source = selectedSource.value;
    notice.value = "";
    await load();
    if (source && selectedSource.value === source && !error.value) {
      await selectDocument(source);
    }
  }

  async function selectDocument(source: string): Promise<void> {
    if (deleting.value) {
      return;
    }
    const requestId = ++detailsRequestId;
    selectedSource.value = source;
    details.value = null;
    detailsLoading.value = true;
    detailsError.value = "";
    notice.value = "";
    try {
      const response = await getKnowledgeDocumentDetails(source);
      if (requestId === detailsRequestId) {
        details.value = response;
      }
    } catch (requestError) {
      if (requestId === detailsRequestId) {
        detailsError.value = resolveApiError(requestError, "资料详情读取失败");
      }
    } finally {
      if (requestId === detailsRequestId) {
        detailsLoading.value = false;
      }
    }
  }

  function closeDetails(): void {
    detailsRequestId += 1;
    selectedSource.value = "";
    details.value = null;
    detailsLoading.value = false;
    detailsError.value = "";
  }

  async function retryDetails(): Promise<void> {
    if (selectedSource.value) {
      await selectDocument(selectedSource.value);
    }
  }

  async function deleteSelectedDocument(): Promise<void> {
    const source = selectedSource.value;
    if (!source || deleting.value) {
      return;
    }

    deleting.value = true;
    detailsError.value = "";
    try {
      const response = await deleteKnowledgeDocument(source);
      closeDetails();
      notice.value = response.deleted
        ? `已淘汰资料：${source}`
        : `资料已不在目录中：${source}`;
      await load();
    } catch (requestError) {
      detailsError.value = resolveApiError(requestError, "资料淘汰失败");
    } finally {
      deleting.value = false;
    }
  }

  onMounted(() => {
    void load();
  });

  return {
    result,
    loading,
    error,
    selectedSource,
    details,
    detailsLoading,
    detailsError,
    deleting,
    notice,
    load,
    refresh,
    selectDocument,
    closeDetails,
    retryDetails,
    deleteSelectedDocument,
  };
}
