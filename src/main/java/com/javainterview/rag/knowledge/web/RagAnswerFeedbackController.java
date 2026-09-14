package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.feedback.RagAnswerFeedback;
import com.javainterview.rag.knowledge.feedback.RagAnswerFeedbackStore;
import com.javainterview.rag.knowledge.rag.RagRetrievalLogStore;
import com.javainterview.rag.knowledge.review.KnowledgeReviewTask;
import com.javainterview.rag.knowledge.review.KnowledgeReviewTaskRequest;
import com.javainterview.rag.knowledge.review.KnowledgeReviewTaskService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Map;
import java.util.List;

@RestController
@Profile("storage")
@RequestMapping("/api/knowledge/answer/feedback")
public class RagAnswerFeedbackController {
    private final RagAnswerFeedbackStore store;
    private final RagRetrievalLogStore retrievalLogStore;
    private final KnowledgeReviewTaskService reviewTaskService;

    public RagAnswerFeedbackController(RagAnswerFeedbackStore store, RagRetrievalLogStore retrievalLogStore,
                                       KnowledgeReviewTaskService reviewTaskService) {
        this.store = store;
        this.retrievalLogStore = retrievalLogStore;
        this.reviewTaskService = reviewTaskService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void record(@RequestBody RagAnswerFeedbackRequest request) {
        if (request == null || request.question() == null || request.question().isBlank()) {
            throw new IllegalArgumentException("问题不能为空");
        }
        store.record(new RagAnswerFeedback(null, hash(request.question()), request.grounded(), request.retrievalStrategy(),
                request.feedback(), "PENDING", null, null, LocalDateTime.now(), null));
    }

    @GetMapping
    public List<RagAnswerFeedback> recent(@RequestParam(defaultValue = "20") int limit,
                                          @RequestParam(required = false) String feedback,
                                          @RequestParam(required = false) String reviewStatus) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("反馈数量必须在 1 到 100 之间");
        return store.findRecent(limit, feedback, reviewStatus);
    }

    @PatchMapping("/{id}")
    public RagAnswerFeedback review(@PathVariable long id, @RequestBody RagAnswerFeedbackReviewRequest request) {
        if (request == null) throw new IllegalArgumentException("审阅请求不能为空");
        return store.review(id, request.reviewStatus(), request.reviewNote(), request.failureLayer());
    }

    @GetMapping("/{id}/diagnosis")
    public RagAnswerFeedbackDiagnosis diagnosis(@PathVariable long id) {
        RagAnswerFeedback feedback = store.findById(id).orElseThrow(() -> new IllegalArgumentException("反馈不存在"));
        return new RagAnswerFeedbackDiagnosis(feedback, retrievalLogStore.findRecentByQuestionHash(feedback.questionHash(), 100));
    }

    @PostMapping("/{id}/review-task")
    public KnowledgeReviewTask createReviewTask(@PathVariable long id,
                                                @RequestBody KnowledgeReviewTaskRequest request) {
        RagAnswerFeedback feedback = store.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("反馈不存在"));
        if (!"NOT_HELPFUL".equals(feedback.feedback()) || !"EVALUATION".equals(feedback.reviewStatus())) {
            throw new IllegalArgumentException("只有已纳入评估的差评可以创建复习任务");
        }
        if (request == null) {
            throw new IllegalArgumentException("复习任务请求不能为空");
        }
        return reviewTaskService.create(request.question(), request.claim(),
                request.evidenceStatus(), request.sourceNumbers());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalid(IllegalArgumentException exception) { return Map.of("message", exception.getMessage()); }

    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.strip().getBytes(StandardCharsets.UTF_8))); }
        catch (Exception exception) { throw new IllegalStateException("问题标识生成失败", exception); }
    }
}
