package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.rag.RagAnswerResult;
import com.javainterview.rag.knowledge.rag.RagAnswerService;
import com.javainterview.rag.knowledge.vector.MarkdownSearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RagAnswerControllerTest {

    @Test
    void returnsAnswerAndTheSourcesActuallyUsedForIt() throws Exception {
        RagAnswerService service = mock(RagAnswerService.class);
        when(service.answer("HashMap 底层结构")).thenReturn(new RagAnswerResult(
                "HashMap 使用数组、链表和红黑树 [资料 1]。", true,
                List.of(new MarkdownSearchResult(
                        "HashMap 在 Java 8 中使用数组、链表和红黑树。",
                        "java/collections.md", "version-1", 0,
                        List.of("Java", "集合", "HashMap"), 0.91
                ))
        ));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new RagAnswerController(service)).build();

        mockMvc.perform(post("/api/knowledge/answer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"HashMap 底层结构\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("HashMap 使用数组、链表和红黑树 [资料 1]。"))
                .andExpect(jsonPath("$.grounded").value(true))
                .andExpect(jsonPath("$.sources[0].source").value("java/collections.md"))
                .andExpect(jsonPath("$.sources[0].headingPath[2]").value("HashMap"));
    }
}
