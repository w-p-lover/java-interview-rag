package com.javainterview.rag.interview.deepdive;

import com.javainterview.rag.knowledge.document.KnowledgeBlockType;

import java.util.List;
import java.util.Objects;

public record ProjectDeepDiveResult(
        String strategyVersion,
        Project project,
        boolean knowledgeIncluded,
        List<Question> questions
) {

    public ProjectDeepDiveResult {
        strategyVersion = requireText(strategyVersion, "项目深挖策略版本不能为空");
        Objects.requireNonNull(project, "项目事实不能为空");
        questions = List.copyOf(Objects.requireNonNull(questions, "项目深挖题不能为空"));
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("项目深挖题不能为空");
        }
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.strip();
    }

    public record Project(String statement, String evidence) {

        public Project {
            statement = requireText(statement, "项目事实描述不能为空");
            evidence = requireText(evidence, "项目逐字证据不能为空");
        }
    }

    public record Question(
            Dimension dimension,
            String question,
            String objective,
            String basisEvidence,
            String knowledgeQuery,
            boolean knowledgeGrounded,
            List<KnowledgeSource> knowledgeSources
    ) {

        public Question {
            Objects.requireNonNull(dimension, "项目深挖维度不能为空");
            question = requireText(question, "项目深挖题不能为空");
            objective = requireText(objective, "项目深挖考察目标不能为空");
            basisEvidence = requireText(basisEvidence, "项目深挖依据不能为空");
            knowledgeQuery = requireText(knowledgeQuery, "项目深挖知识查询不能为空");
            knowledgeSources = List.copyOf(Objects.requireNonNull(knowledgeSources, "项目深挖知识来源不能为空"));
            if (knowledgeGrounded != !knowledgeSources.isEmpty()) {
                throw new IllegalArgumentException("项目深挖知识来源状态与实际来源不一致");
            }
        }
    }

    public record KnowledgeSource(
            String content,
            String source,
            String documentVersion,
            int chunkOrder,
            List<String> headingPath,
            Double similarity,
            Integer pageNumber,
            KnowledgeBlockType blockType
    ) {

        public KnowledgeSource {
            content = requireText(content, "项目深挖知识正文不能为空");
            source = requireText(source, "项目深挖知识来源不能为空");
            documentVersion = requireText(documentVersion, "项目深挖文档版本不能为空");
            headingPath = List.copyOf(Objects.requireNonNull(headingPath, "项目深挖标题路径不能为空"));
            Objects.requireNonNull(blockType, "项目深挖内容块类型不能为空");
        }
    }

    public enum Dimension {
        FOUNDATION,
        TRADE_OFF,
        FAILURE,
        EVOLUTION
    }
}
