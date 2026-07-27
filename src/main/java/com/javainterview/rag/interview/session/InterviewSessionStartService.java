package com.javainterview.rag.interview.session;

import com.javainterview.rag.interview.preparation.InterviewPreparationResult;
import com.javainterview.rag.interview.preparation.InterviewPreparationService;
import com.javainterview.rag.interview.question.InterviewQuestionGenerationResult;
import com.javainterview.rag.interview.question.InterviewQuestionGenerationService;

import java.util.Objects;

public class InterviewSessionStartService {

    private static final int DEFAULT_INITIAL_QUESTION_COUNT = 5;
    private static final int DEFAULT_MAX_QUESTION_COUNT = 8;

    private final InterviewPreparationService preparationService;
    private final InterviewQuestionGenerationService questionGenerationService;
    private final InterviewSessionService sessionService;

    public InterviewSessionStartService(InterviewPreparationService preparationService,
                                        InterviewQuestionGenerationService questionGenerationService,
                                        InterviewSessionService sessionService) {
        this.preparationService = Objects.requireNonNull(preparationService, "面试准备服务不能为空");
        this.questionGenerationService = Objects.requireNonNull(
                questionGenerationService, "面试出题服务不能为空");
        this.sessionService = Objects.requireNonNull(sessionService, "面试会话服务不能为空");
    }

    public InterviewSession start(String targetRole, String resumeText, String jobDescription,
                                  Integer initialQuestionCount, Integer maxQuestionCount) {
        int initialCount = initialQuestionCount == null
                ? DEFAULT_INITIAL_QUESTION_COUNT : initialQuestionCount;
        int maxCount = maxQuestionCount == null ? DEFAULT_MAX_QUESTION_COUNT : maxQuestionCount;
        if (initialCount < 3 || initialCount > 8) {
            throw new IllegalArgumentException("初始题数必须在 3 到 8 之间");
        }
        if (maxCount < initialCount || maxCount > 12) {
            throw new IllegalArgumentException("整场题数上限必须在初始题数到 12 之间");
        }

        InterviewPreparationResult preparation = preparationService.prepare(
                targetRole, resumeText, jobDescription);
        InterviewQuestionGenerationResult questionResult = questionGenerationService.generate(
                preparation.targetRole(), preparation.profile(), initialCount);
        return sessionService.start(
                preparation.targetRole(), preparation.profile(), questionResult.questions(), maxCount);
    }
}
