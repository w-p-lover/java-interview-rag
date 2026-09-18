package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.draft.InterviewDraftResult;
import com.javainterview.rag.interview.draft.InterviewDraftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@Profile("storage")
@RequestMapping("/api/interviews/drafts")
@Tag(name = "面试题草稿", description = "生成、复用和替换面试题草稿")
public class InterviewDraftController {

    private final InterviewDraftService draftService;

    public InterviewDraftController(InterviewDraftService draftService) {
        this.draftService = draftService;
    }

    @PostMapping
    @Operation(summary = "生成或复用面试题草稿")
    public InterviewDraftResult generate(@RequestBody InterviewDraftRequest request) {
        int questionCount = request.questionCount() == null ? 5 : request.questionCount();
        int maxQuestionCount = request.maxQuestionCount() == null
                ? Math.max(questionCount, 8) : request.maxQuestionCount();
        return draftService.generate(
                request.targetRole(),
                request.resumeText(),
                request.jobDescription(),
                questionCount,
                maxQuestionCount,
                request.skill(),
                request.difficulty(),
                Boolean.TRUE.equals(request.regenerate()));
    }

    @PostMapping("/{draftId}/questions/{questionIndex}/replace")
    @Operation(summary = "替换一道草稿题")
    public InterviewDraftResult replaceQuestion(@PathVariable String draftId,
                                                @PathVariable int questionIndex,
                                                @RequestBody InterviewQuestionReplacementRequest request) {
        return draftService.replaceQuestion(draftId, questionIndex, request.generation());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleMissingDraft(NoSuchElementException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> handleUnavailableDraft(IllegalStateException exception) {
        return Map.of("message", "面试题草稿服务暂不可用，请稍后重试");
    }
}
