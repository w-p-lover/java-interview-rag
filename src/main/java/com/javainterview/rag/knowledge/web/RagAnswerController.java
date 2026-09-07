package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.rag.RagAnswerService;
import com.javainterview.rag.knowledge.rag.RagAnswerStreamEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.List;

@RestController
@Profile("storage")
@RequestMapping("/api/knowledge/answer")
@Tag(name = "知识问答", description = "基于知识库生成带来源引用的回答")
public class RagAnswerController {

    private final RagAnswerService answerService;

    public RagAnswerController(RagAnswerService answerService) {
        this.answerService = answerService;
    }

    @PostMapping
    @Operation(summary = "生成知识库回答", description = "检索知识片段后生成回答，并返回可追溯来源")
    public RagAnswerResponse answer(@RequestBody RagAnswerRequest request) {
        return RagAnswerResponse.from(answerService.answer(request.question(), history(request)));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式生成知识库回答", description = "检索知识片段后以 SSE 增量返回回答，结束事件携带可追溯来源")
    public Flux<ServerSentEvent<RagAnswerStreamEvent>> stream(@RequestBody RagAnswerRequest request) {
        return answerService.streamAnswer(request.question(), history(request))
                .map(event -> ServerSentEvent.<RagAnswerStreamEvent>builder(event)
                        .event(event.type())
                        .build());
    }

    private List<com.javainterview.rag.knowledge.rag.RagConversationTurn> history(RagAnswerRequest request) {
        return request.history() == null ? List.of() : request.history();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }
}
