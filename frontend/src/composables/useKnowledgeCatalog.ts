import { onMounted, shallowRef } from "vue";

import {
  listKnowledgeDocuments,
  type KnowledgeDocumentCatalogResponse,
} from "@/api/knowledge";
import { resolveApiError } from "@/api/request";

export function useKnowledgeCatalog() {
  const result = shallowRef<KnowledgeDocumentCatalogResponse | null>(null);
  const loading = shallowRef(false);
  const error = shallowRef("");

  async function load(): Promise<void> {
    loading.value = true;
    error.value = "";
    try {
      result.value = await listKnowledgeDocuments();
    } catch (requestError) {
      error.value = resolveApiError(requestError, "资料目录读取失败");
    } finally {
      loading.value = false;
    }
  }

  onMounted(() => {
    void load();
  });

  return {
    result,
    loading,
    error,
    load,
  };
}
