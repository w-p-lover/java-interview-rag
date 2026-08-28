package com.javainterview.rag.knowledge.evaluation;

import com.javainterview.rag.knowledge.document.KnowledgeBlockType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FixedRagStructureEvaluationDataset {

    public static final String STRUCTURED_PDF = "java/structured-java-notes.pdf";

    private static final List<RagEvaluationCase> CASES = List.of(
            structureCase(
                    "pdf-structure-code-treeify",
                    "TREEIFY_THRESHOLD 达到后由哪个方法把 HashMap 桶转换为树？",
                    List.of("1. HashMap"),
                    1,
                    KnowledgeBlockType.CODE,
                    "TREEIFY_THRESHOLD", "treeifyBin"),
            structureCase(
                    "pdf-structure-table-capacity",
                    "HashMap 示例中的 initialCapacity 和 resizeThreshold 分别是多少？",
                    List.of("1. HashMap"),
                    1,
                    KnowledgeBlockType.TABLE,
                    "initialCapacity 为 16", "resizeThreshold 为 12"),
            structureCase(
                    "pdf-structure-cross-page-treeification",
                    "碰撞链在什么条件下会转换为 red black tree？",
                    List.of("1. HashMap", "1.1 Treeification"),
                    2,
                    KnowledgeBlockType.TEXT,
                    "碰撞链达到阈值后可以转换为红黑树")
    );

    public List<RagEvaluationCase> findAll() {
        return CASES;
    }

    private static RagEvaluationCase structureCase(String id, String question, List<String> headingPath,
                                                    int pageNumber, KnowledgeBlockType blockType,
                                                    String... expectedAnswerPoints) {
        RagEvaluationStructureExpectation expectation = new RagEvaluationStructureExpectation(
                STRUCTURED_PDF, headingPath, pageNumber, blockType);
        return new RagEvaluationCase(
                id,
                question,
                RagEvaluationCaseType.DOCUMENT_STRUCTURE,
                List.of(STRUCTURED_PDF),
                List.of(headingPath),
                List.of(expectation),
                List.of(expectedAnswerPoints)
        );
    }
}
