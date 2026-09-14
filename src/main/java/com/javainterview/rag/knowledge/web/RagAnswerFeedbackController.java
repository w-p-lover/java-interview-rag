package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.feedback.RagAnswerFeedback;
import com.javainterview.rag.knowledge.feedback.RagAnswerFeedbackStore;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;

@RestController
@Profile("storage")
@RequestMapping("/api/knowledge/answer/feedback")
public class RagAnswerFeedbackController {
    private final RagAnswerFeedbackStore store;

    public RagAnswerFeedbackController(RagAnswerFeedbackStore store) { this.store = store; }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void record(@RequestBody RagAnswerFeedbackRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        store.record(new RagAnswerFeedback(hash(request.question()), request.grounded(), request.retrievalStrategy(), request.feedback(), LocalDateTime.now()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalid(IllegalArgumentException exception) { return Map.of("message", exception.getMessage()); }

    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.strip().getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException("问题标识生成失败", exception); }
    }
}
