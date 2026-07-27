package com.javainterview.rag.interview.session;

import com.javainterview.rag.interview.preparation.InterviewFocusArea;
import com.javainterview.rag.interview.preparation.InterviewFocusSource;
import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.preparation.InterviewPreparationResult;
import com.javainterview.rag.interview.preparation.InterviewPreparationService;
import com.javainterview.rag.interview.question.InterviewQuestion;
import com.javainterview.rag.interview.question.InterviewQuestionGenerationResult;
import com.javainterview.rag.interview.question.InterviewQuestionGenerationService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InterviewSessionStartServiceTest {

    @Test
    void chainsPreparationQuestionGenerationAndSessionCreationWithDefaults() {
        InterviewPreparationService preparationService = mock(InterviewPreparationService.class);
        InterviewQuestionGenerationService questionService = mock(InterviewQuestionGenerationService.class);
        InterviewSessionService sessionService = mock(InterviewSessionService.class);
        InterviewPreparationResult preparation = new InterviewPreparationResult(
                "Java AI 应用开发工程师", "interview-preparation-v1", profile());
        InterviewQuestionGenerationResult generated = new InterviewQuestionGenerationResult(
                preparation.targetRole(), "interview-question-v1", questions(5));
        InterviewSession expected = mock(InterviewSession.class);
        when(preparationService.prepare("Java AI 应用开发工程师", "简历", null))
                .thenReturn(preparation);
        when(questionService.generate(preparation.targetRole(), preparation.profile(), 5))
                .thenReturn(generated);
        when(sessionService.start(
                preparation.targetRole(), preparation.profile(), generated.questions(), 8))
                .thenReturn(expected);
        InterviewSessionStartService service = new InterviewSessionStartService(
                preparationService, questionService, sessionService);

        InterviewSession result = service.start(
                "Java AI 应用开发工程师", "简历", null, null, null);

        assertEquals(expected, result);
        verify(questionService).generate(preparation.targetRole(), preparation.profile(), 5);
        verify(sessionService).start(
                preparation.targetRole(), preparation.profile(), generated.questions(), 8);
    }

    @Test
    void rejectsInvalidCountsBeforeCallingDownstreamServices() {
        InterviewPreparationService preparationService = mock(InterviewPreparationService.class);
        InterviewQuestionGenerationService questionService = mock(InterviewQuestionGenerationService.class);
        InterviewSessionService sessionService = mock(InterviewSessionService.class);
        InterviewSessionStartService service = new InterviewSessionStartService(
                preparationService, questionService, sessionService);

        assertEquals("初始题数必须在 3 到 8 之间",
                assertThrows(IllegalArgumentException.class,
                        () -> service.start("Java 后端", "简历", null, 2, 8)).getMessage());
        assertEquals("整场题数上限必须在初始题数到 12 之间",
                assertThrows(IllegalArgumentException.class,
                        () -> service.start("Java 后端", "简历", null, 5, 4)).getMessage());
        verifyNoInteractions(preparationService, questionService, sessionService);
    }

    private InterviewPreparationProfile profile() {
        return new InterviewPreparationProfile(
                "具备 Java 后端和 RAG 项目经验。",
                List.of("使用 Spring Boot 和 Qdrant 开发项目"),
                List.of("岗位要求 MySQL 调优"),
                List.of(new InterviewFocusArea(
                        "RAG 工程化", "目标岗位和简历涉及 RAG",
                        List.of(InterviewFocusSource.TARGET_ROLE, InterviewFocusSource.RESUME)))
        );
    }

    private List<InterviewQuestion> questions(int count) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> new InterviewQuestion(
                        "题目 " + index,
                        "目标 " + index,
                        List.of(InterviewFocusSource.TARGET_ROLE),
                        "查询 " + index,
                        false,
                        List.of()))
                .toList();
    }
}
