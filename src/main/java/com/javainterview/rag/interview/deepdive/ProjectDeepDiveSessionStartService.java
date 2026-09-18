package com.javainterview.rag.interview.deepdive;

import com.javainterview.rag.interview.preparation.InterviewFocusArea;
import com.javainterview.rag.interview.preparation.InterviewFocusSource;
import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;
import com.javainterview.rag.interview.question.InterviewDifficulty;
import com.javainterview.rag.interview.question.InterviewQuestionAllocation;
import com.javainterview.rag.interview.question.InterviewQuestionSource;
import com.javainterview.rag.interview.question.InterviewSkill;
import com.javainterview.rag.interview.session.InterviewConfigurationSnapshot;
import com.javainterview.rag.interview.session.InterviewSession;
import com.javainterview.rag.interview.session.InterviewSessionMode;
import com.javainterview.rag.interview.session.InterviewSessionService;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ProjectDeepDiveSessionStartService {

    private static final int QUESTION_COUNT = 4;

    private final ProjectDeepDiveService deepDiveService;
    private final InterviewSessionService sessionService;

    public ProjectDeepDiveSessionStartService(ProjectDeepDiveService deepDiveService,
                                              InterviewSessionService sessionService) {
        this.deepDiveService = Objects.requireNonNull(deepDiveService, "项目深挖服务不能为空");
        this.sessionService = Objects.requireNonNull(sessionService, "面试会话服务不能为空");
    }

    public InterviewSession start(String targetRole, ProjectDeepDiveRequest request) {
        ProjectDeepDiveResult result = deepDiveService.generate(request);
        if (result.questions().size() != QUESTION_COUNT) {
            throw new IllegalStateException("项目深挖会话必须包含四道题");
        }
        InterviewPreparationProfile profile = profile(result.project());
        List<InterviewQuestion> questions = result.questions().stream()
                .map(this::toInterviewQuestion)
                .toList();
        return sessionService.start(
                InterviewSessionMode.PROJECT_DEEP_DIVE,
                targetRole,
                profile,
                questions,
                QUESTION_COUNT,
                configuration(result));
    }

    private InterviewConfigurationSnapshot configuration(ProjectDeepDiveResult result) {
        List<InterviewQuestionAllocation> allocations = java.util.stream.IntStream
                .range(0, QUESTION_COUNT)
                .mapToObj(index -> new InterviewQuestionAllocation(
                        index,
                        InterviewQuestionSource.PROJECT,
                        result.questions().get(index).knowledgeGrounded()
                                ? List.of(InterviewQuestionSource.RESUME, InterviewQuestionSource.KNOWLEDGE)
                                : List.of(InterviewQuestionSource.RESUME)))
                .toList();
        return new InterviewConfigurationSnapshot(
                result.strategyVersion(),
                InterviewSessionMode.PROJECT_DEEP_DIVE,
                InterviewSkill.GENERAL,
                InterviewDifficulty.ADVANCED,
                QUESTION_COUNT,
                QUESTION_COUNT,
                Map.of(InterviewQuestionSource.PROJECT, QUESTION_COUNT),
                allocations,
                "resume-evidence-confirmed",
                result.strategyVersion(),
                "deterministic");
    }

    private InterviewPreparationProfile profile(ProjectDeepDiveResult.Project project) {
        return new InterviewPreparationProfile(
                "围绕已确认项目经历进行专项深挖。",
                List.of(project.statement(), project.evidence()),
                List.of(),
                List.of(new InterviewFocusArea(
                        project.evidence(),
                        "项目事实已通过简历证据检查，围绕职责、取舍、故障和演进展开追问。",
                        List.of(InterviewFocusSource.RESUME))));
    }

    private InterviewQuestion toInterviewQuestion(ProjectDeepDiveResult.Question question) {
        return new InterviewQuestion(
                question.question(),
                question.objective(),
                List.of(InterviewFocusSource.RESUME),
                question.knowledgeQuery(),
                question.knowledgeGrounded(),
                question.knowledgeSources().stream().map(this::toSearchResult).toList());
    }

    private MarkdownSearchResult toSearchResult(ProjectDeepDiveResult.KnowledgeSource source) {
        return new MarkdownSearchResult(
                source.content(),
                source.source(),
                source.documentVersion(),
                source.chunkOrder(),
                source.headingPath(),
                source.similarity(),
                source.pageNumber(),
                source.blockType());
    }
}
