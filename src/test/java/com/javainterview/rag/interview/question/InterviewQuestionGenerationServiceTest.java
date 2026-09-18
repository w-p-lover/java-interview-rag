package com.javainterview.rag.interview.question;

import com.javainterview.rag.interview.preparation.InterviewFocusArea;
import com.javainterview.rag.interview.preparation.InterviewFocusSource;
import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import com.javainterview.rag.knowledge.vector.MarkdownSimilaritySearchService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.atLeast;

class InterviewQuestionGenerationServiceTest {

    @Test
    void supportsSpringAiStructuredOutputConversion() {
        BeanOutputConverter<InterviewQuestionDraftSet> converter =
                new BeanOutputConverter<>(InterviewQuestionDraftSet.class);

        InterviewQuestionDraftSet draftSet = converter.convert("""
                {
                  "questions":[
                    {
                      "question":"请说明 RAG 文档更新时如何避免旧向量残留？",
                      "objective":"验证向量生命周期治理能力",
                      "basisSources":["RESUME","JOB_DESCRIPTION"],
                      "knowledgeQuery":"RAG 向量更新 删除 重建"
                    },
                    {
                      "question":"MySQL 索引失效时你会如何定位？",
                      "objective":"验证数据库排查能力",
                      "basisSources":["JOB_DESCRIPTION"],
                      "knowledgeQuery":"MySQL 索引失效 排查"
                    },
                    {
                      "question":"Spring Boot 项目如何处理模型调用失败？",
                      "objective":"验证 Java AI 工程化能力",
                      "basisSources":["TARGET_ROLE","RESUME"],
                      "knowledgeQuery":"Spring AI 模型调用失败 重试"
                    }
                  ]
                }
                """);

        assertEquals(3, draftSet.questions().size());
        assertEquals(List.of(InterviewFocusSource.RESUME, InterviewFocusSource.JOB_DESCRIPTION),
                draftSet.questions().get(0).basisSources());
    }

    @Test
    void generatesQuestionsAndBindsReliableKnowledgeSources() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        InterviewQuestionDraftSet generatedDrafts = new InterviewQuestionDraftSet(List.of(
                new InterviewQuestionDraft(
                        "请说明 RAG 文档更新时如何避免旧向量残留？",
                        "验证向量生命周期治理能力",
                        List.of(InterviewFocusSource.RESUME, InterviewFocusSource.JOB_DESCRIPTION),
                        "RAG 向量更新 删除 重建"),
                new InterviewQuestionDraft(
                        "MySQL 索引失效时你会如何定位？",
                        "验证数据库排查能力",
                        List.of(InterviewFocusSource.JOB_DESCRIPTION),
                        "MySQL 索引失效 排查"),
                new InterviewQuestionDraft(
                        "Spring Boot 项目如何处理模型调用失败？",
                        "验证 Java AI 工程化能力",
                        List.of(InterviewFocusSource.TARGET_ROLE, InterviewFocusSource.RESUME),
                        "Spring AI 模型调用失败 重试")
        ));
        when(chatClient.prompt(anyString()).call().entity(InterviewQuestionDraftSet.class))
                .thenReturn(generatedDrafts);
        clearInvocations(chatClient);
        MarkdownSearchResult vectorSource = new MarkdownSearchResult(
                "新版本向量写入成功后再删除旧版本。",
                "rag/vector-lifecycle.md",
                "version-1",
                2,
                List.of("RAG", "向量生命周期"),
                0.87
        );
        when(searchService.search("RAG 向量更新 删除 重建", 3, 0.60))
                .thenReturn(List.of(vectorSource));
        when(searchService.search("MySQL 索引失效 排查", 3, 0.60))
                .thenReturn(List.of());
        when(searchService.search("Spring AI 模型调用失败 重试", 3, 0.60))
                .thenReturn(List.of());
        InterviewQuestionGenerationService service =
                new InterviewQuestionGenerationService(chatClient, searchService);
        InterviewPreparationProfile profile = profile();

        InterviewQuestionGenerationResult result =
                service.generate("Java AI 应用开发工程师", profile, 3);

        assertEquals("Java AI 应用开发工程师", result.targetRole());
        assertEquals("interview-question-v1", result.promptVersion());
        assertEquals(3, result.questions().size());
        assertTrue(result.questions().get(0).knowledgeGrounded());
        assertEquals(List.of(vectorSource), result.questions().get(0).knowledgeSources());
        assertFalse(result.questions().get(1).knowledgeGrounded());
        assertEquals(List.of(), result.questions().get(1).knowledgeSources());
        verify(searchService).search("RAG 向量更新 删除 重建", 3, 0.60);
        verify(searchService).search("MySQL 索引失效 排查", 3, 0.60);
        verify(searchService).search("Spring AI 模型调用失败 重试", 3, 0.60);

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatClient).prompt(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertTrue(prompt.contains("目标岗位：Java AI 应用开发工程师"));
        assertTrue(prompt.contains("具备 Java 后端和 RAG 项目经验"));
        assertTrue(prompt.contains("RAG 向量生命周期"));
        assertTrue(prompt.contains("必须恰好生成 3 道题"));
        assertTrue(prompt.contains("不得使用内部固定 RAG 评估题集"));
    }

    @Test
    void rejectsMissingTargetRoleBeforeCallingModelOrSearch() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        InterviewQuestionGenerationService service =
                new InterviewQuestionGenerationService(chatClient, searchService);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.generate(" ", profile()));

        assertEquals("目标岗位不能为空", exception.getMessage());
        verifyNoInteractions(chatClient, searchService);
    }

    @Test
    void supportsFiveCoreQuestionsAndRejectsInvalidCounts() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        List<InterviewQuestionDraft> drafts = java.util.stream.IntStream.range(0, 5)
                .mapToObj(index -> new InterviewQuestionDraft(
                        "核心题 " + index,
                        "考察目标 " + index,
                        List.of(InterviewFocusSource.RESUME),
                        "知识查询 " + index))
                .toList();
        when(chatClient.prompt(anyString()).call().entity(InterviewQuestionDraftSet.class))
                .thenReturn(new InterviewQuestionDraftSet(drafts));
        when(searchService.search(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.eq(3), org.mockito.ArgumentMatchers.eq(0.60)))
                .thenReturn(List.of());
        InterviewQuestionGenerationService service =
                new InterviewQuestionGenerationService(chatClient, searchService);

        InterviewQuestionGenerationResult result = service.generate("Java 后端", profile(), 5);

        assertEquals(5, result.questions().size());
        assertEquals("初始题数必须在 3 到 8 之间",
                assertThrows(IllegalArgumentException.class,
                        () -> service.generate("Java 后端", profile(), 9)).getMessage());
    }

    @Test
    void keepsGeneratedQuestionsWhenOneKnowledgeLookupFails() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        List<InterviewQuestionDraft> drafts = java.util.stream.IntStream.range(0, 3)
                .mapToObj(index -> new InterviewQuestionDraft(
                        "题目 " + index,
                        "考察目标 " + index,
                        List.of(InterviewFocusSource.RESUME),
                        "知识查询 " + index))
                .toList();
        when(chatClient.prompt(anyString()).call().entity(InterviewQuestionDraftSet.class))
                .thenReturn(new InterviewQuestionDraftSet(drafts));
        when(searchService.search("知识查询 0", 3, 0.60))
                .thenThrow(new IllegalStateException("检索服务不可用"));
        when(searchService.search("知识查询 1", 3, 0.60)).thenReturn(List.of());
        when(searchService.search("知识查询 2", 3, 0.60)).thenReturn(List.of());
        InterviewQuestionGenerationService service =
                new InterviewQuestionGenerationService(chatClient, searchService);

        InterviewQuestionGenerationResult result = service.generate("Java 后端", profile(), 3);

        assertEquals(3, result.questions().size());
        assertEquals("题目 0", result.questions().get(0).question());
        assertFalse(result.questions().get(0).knowledgeGrounded());
        assertEquals(List.of(InterviewQuestionSource.KNOWLEDGE), result.unavailableSources());
        verify(searchService).search("知识查询 2", 3, 0.60);
    }

    @Test
    void generatesSixtyPercentResumeAndFortyPercentDirectionQuestionsWithHistory() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        when(chatClient.prompt(anyString())).thenAnswer(invocation -> {
            String prompt = invocation.getArgument(0);
            int count = prompt.contains("必须恰好生成 3 道题") ? 3 : 2;
            InterviewFocusSource source = prompt.contains("只生成基于简历明确事实")
                    ? InterviewFocusSource.RESUME : InterviewFocusSource.TARGET_ROLE;
            List<InterviewQuestionDraft> drafts = java.util.stream.IntStream.range(0, count)
                    .mapToObj(index -> new InterviewQuestionDraft(
                            source + "题" + index,
                            "考察目标" + index,
                            List.of(source),
                            "查询" + source + index))
                    .toList();
            ChatClient.ChatClientRequestSpec spec = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec call = mock(ChatClient.CallResponseSpec.class);
            when(spec.call()).thenReturn(call);
            when(call.entity(InterviewQuestionDraftSet.class))
                    .thenReturn(new InterviewQuestionDraftSet(drafts));
            return spec;
        });
        when(searchService.search(anyString(),
                org.mockito.ArgumentMatchers.eq(3), org.mockito.ArgumentMatchers.eq(0.60)))
                .thenReturn(List.of());
        InterviewQuestionGenerationService service =
                new InterviewQuestionGenerationService(chatClient, searchService);

        InterviewQuestionSourceGenerationResult result = service.generateBySources(
                "Java 后端工程师",
                profile(),
                5,
                List.of(new HistoricalInterviewQuestion("旧题", "RESUME", "旧知识点")));

        assertEquals(5, result.questions().size());
        assertEquals(3, result.questions().stream()
                .filter(question -> question.basisSources().contains(InterviewFocusSource.RESUME)).count());
        assertEquals(2, result.questions().stream()
                .filter(question -> question.basisSources().contains(InterviewFocusSource.TARGET_ROLE)).count());
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatClient, times(2)).prompt(promptCaptor.capture());
        assertTrue(promptCaptor.getAllValues().stream().allMatch(prompt -> prompt.contains("旧知识点")));
    }

    @Test
    void retriesAllQuestionsFromDirectionWhenResumeBranchFails() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        when(chatClient.prompt(anyString())).thenAnswer(invocation -> {
            String prompt = invocation.getArgument(0);
            if (prompt.contains("只生成基于简历明确事实")) {
                throw new IllegalStateException("简历题生成失败");
            }
            int count = prompt.contains("必须恰好生成 5 道题") ? 5 : 2;
            List<InterviewQuestionDraft> drafts = java.util.stream.IntStream.range(0, count)
                    .mapToObj(index -> new InterviewQuestionDraft(
                            "方向题" + index, "方向目标" + index,
                            List.of(InterviewFocusSource.TARGET_ROLE), "方向查询" + index))
                    .toList();
            ChatClient.ChatClientRequestSpec spec = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec call = mock(ChatClient.CallResponseSpec.class);
            when(spec.call()).thenReturn(call);
            when(call.entity(InterviewQuestionDraftSet.class))
                    .thenReturn(new InterviewQuestionDraftSet(drafts));
            return spec;
        });
        when(searchService.search(anyString(),
                org.mockito.ArgumentMatchers.eq(3), org.mockito.ArgumentMatchers.eq(0.60)))
                .thenReturn(List.of());
        InterviewQuestionGenerationService service =
                new InterviewQuestionGenerationService(chatClient, searchService);

        InterviewQuestionSourceGenerationResult result = service.generateBySources(
                "Java 后端工程师", profile(), 5, List.of());

        assertEquals(5, result.questions().size());
        assertEquals(List.of(InterviewQuestionSource.RESUME), result.unavailableSources());
        assertTrue(result.questions().stream().allMatch(question ->
                question.basisSources().equals(List.of(InterviewFocusSource.TARGET_ROLE))));
        verify(chatClient, atLeast(2)).prompt(anyString());
    }

    @Test
    void preservesKnowledgeDegradationWhenResumeBranchFallsBackToDirection() {
        ChatClient chatClient = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        MarkdownSimilaritySearchService searchService = mock(MarkdownSimilaritySearchService.class);
        when(chatClient.prompt(anyString())).thenAnswer(invocation -> {
            String prompt = invocation.getArgument(0);
            if (prompt.contains("只生成基于简历明确事实")) {
                throw new IllegalStateException("简历题生成失败");
            }
            int count = prompt.contains("必须恰好生成 5 道题") ? 5 : 2;
            List<InterviewQuestionDraft> drafts = java.util.stream.IntStream.range(0, count)
                    .mapToObj(index -> new InterviewQuestionDraft(
                            "方向题" + index, "方向目标" + index,
                            List.of(InterviewFocusSource.TARGET_ROLE), "方向查询" + index))
                    .toList();
            ChatClient.ChatClientRequestSpec spec = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec call = mock(ChatClient.CallResponseSpec.class);
            when(spec.call()).thenReturn(call);
            when(call.entity(InterviewQuestionDraftSet.class))
                    .thenReturn(new InterviewQuestionDraftSet(drafts));
            return spec;
        });
        when(searchService.search(anyString(),
                org.mockito.ArgumentMatchers.eq(3), org.mockito.ArgumentMatchers.eq(0.60)))
                .thenThrow(new IllegalStateException("知识检索不可用"));
        InterviewQuestionGenerationService service =
                new InterviewQuestionGenerationService(chatClient, searchService);

        InterviewQuestionSourceGenerationResult result = service.generateBySources(
                "Java 后端工程师", profile(), 5, List.of());

        assertEquals(List.of(InterviewQuestionSource.RESUME, InterviewQuestionSource.KNOWLEDGE),
                result.unavailableSources());
    }

    private InterviewPreparationProfile profile() {
        return new InterviewPreparationProfile(
                "具备 Java 后端和 RAG 项目经验。",
                List.of("使用 Spring Boot 和 Qdrant 开发 RAG 项目"),
                List.of("岗位要求 MySQL 调优和大模型应用经验"),
                List.of(
                        new InterviewFocusArea(
                                "RAG 向量生命周期",
                                "简历项目与岗位要求均涉及 RAG",
                                List.of(InterviewFocusSource.TARGET_ROLE,
                                        InterviewFocusSource.RESUME,
                                        InterviewFocusSource.JOB_DESCRIPTION)),
                        new InterviewFocusArea(
                                "MySQL 性能优化",
                                "岗位描述明确要求数据库调优",
                                List.of(InterviewFocusSource.JOB_DESCRIPTION))
                )
        );
    }
}
