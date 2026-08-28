package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.rag.RagAnswerService;
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

import java.util.Map;

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
        return RagAnswerResponse.from(answerService.answer(request.question()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }
}
