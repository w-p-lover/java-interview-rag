package com.javainterview.rag.knowledge.experiment;

import com.javainterview.rag.knowledge.document.KnowledgeDocumentSummary;
import com.javainterview.rag.knowledge.document.KnowledgeIndexVersionMetadata;
import com.javainterview.rag.knowledge.document.MarkdownImportRegistry;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;

public class RagRetrievalExperimentContractProvider {

    private final RagRetrievalExperimentEvaluator evaluator;
    private final MarkdownImportRegistry importRegistry;
    private final KnowledgeIndexVersionMetadata indexVersion;
    private final String gitSha;

    public RagRetrievalExperimentContractProvider(
            RagRetrievalExperimentEvaluator evaluator,
            MarkdownImportRegistry importRegistry,
            KnowledgeIndexVersionMetadata indexVersion,
            String gitSha) {
        this.evaluator = Objects.requireNonNull(evaluator, "检索实验评估器不能为空");
        this.importRegistry = Objects.requireNonNull(importRegistry, "知识资料登记器不能为空");
        this.indexVersion = Objects.requireNonNull(indexVersion, "索引契约不能为空");
        this.gitSha = requireText(gitSha, "Git SHA 不能为空");
    }

    public Contract current(
            RagRetrievalExperimentStrategy strategy,
            RagRetrievalExperimentSplit split) {
        String corpusHash = sha256(importRegistry.findCurrentDocuments().stream()
                .sorted(java.util.Comparator.comparing(KnowledgeDocumentSummary::source))
                .map(document -> document.source() + "@" + document.version())
                .collect(java.util.stream.Collectors.joining("\n")));
        String indexContract = indexVersion.toString();
        String contractHash = sha256(String.join("|", evaluator.datasetVersion(), split.name(),
                strategy.name(), corpusHash, indexContract, gitSha));
        return new Contract(evaluator.datasetVersion(), split, strategy, corpusHash,
                indexContract, gitSha, contractHash, evaluator.cases(split).size());
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("SHA-256 不可用", exception);
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }

    public record Contract(
            String datasetVersion,
            RagRetrievalExperimentSplit split,
            RagRetrievalExperimentStrategy strategy,
            String corpusHash,
            String indexContract,
            String gitSha,
            String contractHash,
            int totalCaseCount
    ) {
    }
}
