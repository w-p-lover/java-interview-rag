package com.javainterview.rag.interview.deepdive;

import com.javainterview.rag.interview.resume.ResumeEvidenceAuditResult;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectDeepDiveServiceTest {

    @Test
    void generatesFourDeterministicDimensionsWithoutKnowledgeLookup() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        ProjectDeepDiveService service = new ProjectDeepDiveService(searchService);

        ProjectDeepDiveResult result = service.generate(request(false));

        assertEquals("project-deep-dive-v1", result.strategyVersion());
        assertEquals("Java Interview RAG", result.project().evidence());
        assertEquals(List.of(
                        ProjectDeepDiveResult.Dimension.FOUNDATION,
                        ProjectDeepDiveResult.Dimension.TRADE_OFF,
                        ProjectDeepDiveResult.Dimension.FAILURE,
                        ProjectDeepDiveResult.Dimension.EVOLUTION),
                result.questions().stream().map(ProjectDeepDiveResult.Question::dimension).toList());
        assertTrue(result.questions().stream()
                .allMatch(question -> question.basisEvidence().equals("Java Interview RAG")));
        assertTrue(result.questions().stream()
                .noneMatch(ProjectDeepDiveResult.Question::knowledgeGrounded));
        verify(searchService, never()).search(anyString(), eq(3), eq(0.60));
    }

    @Test
    void bindsReadOnlyKnowledgeSourcesWhenExplicitlyRequested() {
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        when(searchService.search(anyString(), eq(3), eq(0.60))).thenReturn(List.of(
                new MarkdownSearchResult(
                        "资料正文", "project.md", "v1", 2,
                        List.of("架构", "检索"), 0.83)));
        ProjectDeepDiveService service = new ProjectDeepDiveService(searchService);

        ProjectDeepDiveResult result = service.generate(request(true));

        assertTrue(result.knowledgeIncluded());
        assertTrue(result.questions().stream().allMatch(ProjectDeepDiveResult.Question::knowledgeGrounded));
        assertTrue(result.questions().stream().allMatch(question -> question.knowledgeSources().size() == 1));
        assertEquals("project.md", result.questions().get(0).knowledgeSources().get(0).source());
        verify(searchService, times(4)).search(anyString(), eq(3), eq(0.60));
    }

    @Test
    void rejectsFactsOutsideProjectBoundary() {
        ProjectDeepDiveService service = new ProjectDeepDiveService(mock(MarkdownSimilaritySearchService.class));
        ProjectDeepDiveRequest request = new ProjectDeepDiveRequest(
                new ProjectDeepDiveRequest.ConfirmedProject(
                        ResumeEvidenceAuditResult.FactType.TECHNOLOGY,
                        "使用 Spring AI",
                        "Spring AI"),
                false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.generate(request));

        assertTrue(exception.getMessage().contains("PROJECT"));
        assertFalse(exception.getMessage().contains("Spring AI"));
    }

    private ProjectDeepDiveRequest request(boolean includeKnowledge) {
        return new ProjectDeepDiveRequest(
                new ProjectDeepDiveRequest.ConfirmedProject(
                        ResumeEvidenceAuditResult.FactType.PROJECT,
                        "负责 Java 面试训练平台",
                        "Java Interview RAG"),
                includeKnowledge);
    }
}
