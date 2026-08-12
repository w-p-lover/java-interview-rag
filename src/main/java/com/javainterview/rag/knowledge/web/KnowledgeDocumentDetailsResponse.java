package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.document.KnowledgeDocumentOutline;
import com.javainterview.rag.knowledge.document.KnowledgeDocumentSummary;

import java.util.List;

public record KnowledgeDocumentDetailsResponse(
        String source,
        String currentVersion,
        int versionCount,
        KnowledgeDocumentOutline outline,
        List<KnowledgeDocumentVersionItem> versions
) {

    static KnowledgeDocumentDetailsResponse from(List<KnowledgeDocumentSummary> summaries) {
        if (summaries.isEmpty()) {
            throw new IllegalArgumentException("资料版本不能为空");
        }
        String currentVersion = summaries.get(0).version();
        List<KnowledgeDocumentVersionItem> versions = summaries.stream()
                .map(summary -> KnowledgeDocumentVersionItem.from(
                        summary, summary.version().equals(currentVersion)))
                .toList();
        return new KnowledgeDocumentDetailsResponse(
                summaries.get(0).source(), currentVersion, versions.size(),
                summaries.get(0).outline(), versions);
    }
}
