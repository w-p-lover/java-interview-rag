package com.javainterview.rag.knowledge.web;

import com.javainterview.rag.knowledge.evaluation.*;
import com.javainterview.rag.knowledge.feedback.RagAnswerFeedback;
import com.javainterview.rag.knowledge.feedback.RagAnswerFeedbackStore;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@Profile("storage")
@RequestMapping("/api/knowledge/evaluation-candidates")
public class RagEvaluationCandidateController {
    private final RagEvaluationCandidateService service;
    private final RagAnswerFeedbackStore feedbackStore;
    public RagEvaluationCandidateController(RagEvaluationCandidateService service, RagAnswerFeedbackStore feedbackStore) {
        this.service = service;
        this.feedbackStore = feedbackStore;
    }
    @PostMapping
    public RagEvaluationCandidate create(@RequestParam(required = false) Long feedbackId, @RequestBody RagEvaluationCandidateRequest request) {
        if (request == null) throw new IllegalArgumentException("评估候选请求不能为空");
        if (feedbackId != null) {
            RagAnswerFeedback feedback = feedbackStore.findById(feedbackId)
                    .orElseThrow(() -> new IllegalArgumentException("反馈不存在"));
            if (!"NOT_HELPFUL".equals(feedback.feedback()) || !"EVALUATION".equals(feedback.reviewStatus())) {
                throw new IllegalArgumentException("只有已纳入评估的差评可以创建回归候选");
            }
        }
        return service.create(feedbackId, request.question(), request.expectedSources(), request.expectedAnswerPoints());
    }
    @GetMapping
    public List<RagEvaluationCandidate> recent(@RequestParam(defaultValue = "20") int limit, @RequestParam(required = false) RagEvaluationCandidateStatus status) { return service.findRecent(limit, status); }
    @PatchMapping("/{id}")
    public RagEvaluationCandidate review(@PathVariable String id, @RequestBody RagEvaluationCandidateReviewRequest request) {
        if (request == null || request.status() == null) throw new IllegalArgumentException("评估候选状态不能为空");
        try { return service.review(id, RagEvaluationCandidateStatus.valueOf(request.status())); }
        catch (IllegalArgumentException exception) { throw new IllegalArgumentException("评估候选状态只能是 ACTIVE 或 IGNORED"); }
    }
    @PostMapping("/{id}/evaluate")
    public RagEvaluationCandidateResult evaluate(@PathVariable String id) { return service.evaluate(id); }
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalid(IllegalArgumentException exception) { return Map.of("message", exception.getMessage()); }
}
