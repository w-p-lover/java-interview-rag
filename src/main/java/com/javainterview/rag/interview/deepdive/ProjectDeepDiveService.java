package com.javainterview.rag.interview.deepdive;

import com.javainterview.rag.interview.resume.ResumeEvidenceAuditResult;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;

import java.util.List;
import java.util.Objects;

public class ProjectDeepDiveService {

    private static final int PROJECT_TEXT_MAX_LENGTH = 500;
    private static final int KNOWLEDGE_TOP_K = 3;
    private static final double KNOWLEDGE_SIMILARITY_THRESHOLD = 0.60;
    private static final String STRATEGY_VERSION = "project-deep-dive-v1";
    private static final List<QuestionTemplate> QUESTION_TEMPLATES = List.of(
            new QuestionTemplate(
                    ProjectDeepDiveResult.Dimension.FOUNDATION,
                    "请介绍“%s”的业务背景、主要使用者、你的职责边界，以及最终取得的可验证结果。",
                    "确认项目背景、个人职责和结果证据是否清楚",
                    "项目背景 职责边界 结果"),
            new QuestionTemplate(
                    ProjectDeepDiveResult.Dimension.TRADE_OFF,
                    "在“%s”中，请选择一个关键技术方案，说明候选方案、选择依据、放弃了什么，以及这项选择带来的代价。",
                    "检验技术选型是否有候选方案、约束和真实取舍",
                    "架构 技术选型 权衡 代价"),
            new QuestionTemplate(
                    ProjectDeepDiveResult.Dimension.FAILURE,
                    "“%s”遇到过哪些最棘手的故障或质量问题？请说明现象、定位过程、止损措施和复盘后的改进。",
                    "检验故障定位、恢复和复盘是否形成完整链路",
                    "故障 定位 恢复 复盘"),
            new QuestionTemplate(
                    ProjectDeepDiveResult.Dimension.EVOLUTION,
                    "如果“%s”的数据量、并发或业务复杂度继续增长，当前最先出现的瓶颈是什么？你会按什么顺序演进？",
                    "检验容量判断、演进顺序和避免过度设计的能力",
                    "扩展 并发 性能 容量 演进")
    );

    private final MarkdownSimilaritySearchService searchService;

    public ProjectDeepDiveService(MarkdownSimilaritySearchService searchService) {
        this.searchService = Objects.requireNonNull(searchService, "相似度检索服务不能为空");
    }

    public ProjectDeepDiveResult generate(ProjectDeepDiveRequest request) {
        ProjectDeepDiveRequest.ConfirmedProject project = validate(request);
        List<ProjectDeepDiveResult.Question> questions = QUESTION_TEMPLATES.stream()
                .map(template -> createQuestion(project.evidence(), template, request.includeKnowledge()))
                .toList();
        return new ProjectDeepDiveResult(
                STRATEGY_VERSION,
                new ProjectDeepDiveResult.Project(project.statement(), project.evidence()),
                request.includeKnowledge(),
                questions);
    }

    private ProjectDeepDiveRequest.ConfirmedProject validate(ProjectDeepDiveRequest request) {
        if (request == null || request.project() == null) {
            throw new IllegalArgumentException("已确认项目事实不能为空");
        }
        ProjectDeepDiveRequest.ConfirmedProject project = request.project();
        if (project.type() != ResumeEvidenceAuditResult.FactType.PROJECT) {
            throw new IllegalArgumentException("项目深挖只能使用 PROJECT 类型的已确认事实");
        }
        return new ProjectDeepDiveRequest.ConfirmedProject(
                project.type(),
                requireText(project.statement(), "项目事实描述不能为空", "项目事实描述不能超过 500 个字符"),
                requireText(project.evidence(), "项目逐字证据不能为空", "项目逐字证据不能超过 500 个字符"));
    }

    private ProjectDeepDiveResult.Question createQuestion(
            String projectEvidence,
            QuestionTemplate template,
            boolean includeKnowledge) {
        String knowledgeQuery = projectEvidence + " " + template.knowledgeIntent();
        List<ProjectDeepDiveResult.KnowledgeSource> knowledgeSources = includeKnowledge
                ? searchService.search(knowledgeQuery, KNOWLEDGE_TOP_K, KNOWLEDGE_SIMILARITY_THRESHOLD).stream()
                        .map(this::toKnowledgeSource)
                        .toList()
                : List.of();
        return new ProjectDeepDiveResult.Question(
                template.dimension(),
                template.questionTemplate().formatted(projectEvidence),
                template.objective(),
                projectEvidence,
                knowledgeQuery,
                !knowledgeSources.isEmpty(),
                knowledgeSources);
    }

    private ProjectDeepDiveResult.KnowledgeSource toKnowledgeSource(MarkdownSearchResult source) {
        return new ProjectDeepDiveResult.KnowledgeSource(
                source.content(), source.source(), source.documentVersion(), source.chunkOrder(),
                source.headingPath(), source.similarity(), source.pageNumber(), source.blockType());
    }

    private String requireText(String value, String requiredMessage, String lengthMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(requiredMessage);
        }
        String normalized = value.strip();
        if (normalized.length() > PROJECT_TEXT_MAX_LENGTH) {
            throw new IllegalArgumentException(lengthMessage);
        }
        return normalized;
    }

    private record QuestionTemplate(
            ProjectDeepDiveResult.Dimension dimension,
            String questionTemplate,
            String objective,
            String knowledgeIntent
    ) {
    }
}
