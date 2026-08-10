import { shallowRef } from "vue";

import {
  searchKnowledge,
  type KnowledgeSearchResponse,
} from "@/api/knowledge";
import { resolveApiError } from "@/api/request";

export const KNOWLEDGE_SEARCH_MAX_LENGTH = 2_000;

export function useKnowledgeSearch() {
  const query = shallowRef("");
  const submittedQuery = shallowRef("");
  const result = shallowRef<KnowledgeSearchResponse | null>(null);
  const loading = shallowRef(false);
  const error = shallowRef("");

  async function search(): Promise<void> {
    const normalizedQuery = query.value.trim();
    if (!normalizedQuery) {
      error.value = "请输入要查找的内容。";
      return;
    }
    if (normalizedQuery.length > KNOWLEDGE_SEARCH_MAX_LENGTH) {
      error.value = `检索内容不能超过 ${KNOWLEDGE_SEARCH_MAX_LENGTH} 个字符。`;
      return;
    }

    loading.value = true;
    error.value = "";
    result.value = null;
    submittedQuery.value = normalizedQuery;
    try {
      result.value = await searchKnowledge(normalizedQuery);
    } catch (requestError) {
      error.value = resolveApiError(requestError, "资料检索失败");
    } finally {
      loading.value = false;
    }
  }

  function clear(): void {
    query.value = "";
    submittedQuery.value = "";
    result.value = null;
    error.value = "";
  }

  return {
    query,
    submittedQuery,
    result,
    loading,
    error,
    search,
    clear,
  };
}
