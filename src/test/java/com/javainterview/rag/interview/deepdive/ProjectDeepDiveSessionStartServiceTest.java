package com.javainterview.rag.interview.deepdive;

import com.javainterview.rag.interview.evaluation.InterviewAnswerAssessment;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluation;
import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluator;
import com.javainterview.rag.interview.question.InterviewQuestion;
import com.javainterview.rag.interview.resume.ResumeEvidenceAuditResult;
import com.javainterview.rag.interview.session.InterviewSession;
import com.javainterview.rag.interview.session.InterviewSessionMode;
import com.javainterview.rag.interview.session.InterviewSessionService;
import com.javainterview.rag.interview.session.InterviewSessionStatus;
import com.javainterview.rag.interview.session.InterviewSessionStore;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProjectDeepDiveSessionStartServiceTest {

    @Test
    void startsFourQuestionSessionAndDoesNotAppendDynamicFollowUpAtTheCap() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        InterviewSessionStore store = mock(InterviewSessionStore.class);
        InterviewAnswerEvaluator evaluator = mock(InterviewAnswerEvaluator.class);
        InterviewSessionService sessionService = new InterviewSessionService(store, evaluator);
        ProjectDeepDiveSessionStartService service = new ProjectDeepDiveSessionStartService(
                new ProjectDeepDiveService(searchService), sessionService);

        InterviewSession session = service.start(
                " Java 后端开发工程师 ",
                new ProjectDeepDiveRequest(
                        new ProjectDeepDiveRequest.ConfirmedProject(
                                ResumeEvidenceAuditResult.FactType.PROJECT,
                                "负责 Java 面试训练平台",
                                "Java Interview RAG"),
                        false));

        assertEquals(InterviewSessionMode.PROJECT_DEEP_DIVE, session.mode());
        assertEquals("Java 后端开发工程师", session.targetRole());
        assertEquals(4, session.questions().size());
        assertEquals(4, session.maxQuestionCount());
        assertEquals(List.of("负责 Java 面试训练平台", "Java Interview RAG"),
                session.profile().resumeEvidence());
        assertEquals(List.of(com.javainterview.rag.interview.preparation.InterviewFocusSource.RESUME),
                session.questions().get(0).basisSources());
        assertFalse(session.questions().get(0).knowledgeGrounded());
        verify(store).create(session);
        verifyNoInteractions(searchService);

        InterviewQuestion followUp = session.questions().get(0);
        when(store.findById(session.id())).thenReturn(Optional.of(session));
        when(evaluator.evaluate(any(), any(), eq("项目回答"))).thenReturn(
                new InterviewAnswerAssessment(evaluation(), followUp));
        when(store.update(any(InterviewSession.class), eq(0L))).thenReturn(true);

        InterviewSession answered = sessionService.submitAnswer(session.id(), 0, 0L, "项目回答");

        assertEquals(4, answered.questions().size());
        assertEquals(InterviewSessionStatus.IN_PROGRESS, answered.status());

        InterviewSession completed = answered;
        for (int index = 1; index < 4; index++) {
            completed = completed.answer(
                    "第 " + (index + 1) + " 题回答",
                    evaluation(),
                    followUp,
                    session.createdAt().plusMinutes(index + 1));
        }
        assertEquals(4, completed.questions().size());
        assertEquals(InterviewSessionStatus.COMPLETED, completed.status());
        assertEquals(4, completed.report().answeredQuestions());
    }

    private InterviewAnswerEvaluation evaluation() {
        return new InterviewAnswerEvaluation(
                80, 80, 80, 80,
                List.of("说明了个人职责"),
                List.of("缺少量化结果"),
                "补充结果指标。",
                false);
    }
}
