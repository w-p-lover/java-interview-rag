package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;
import com.javainterview.rag.interview.session.InterviewConfigurationSnapshot;

import java.util.List;

public record InterviewSessionCreateRequest(
        String targetRole,
        InterviewPreparationProfile profile,
        List<InterviewQuestion> questions,
        Integer maxQuestionCount,
        InterviewConfigurationSnapshot configuration,
        String requestId
) {

    public InterviewSessionCreateRequest(String targetRole,
                                         InterviewPreparationProfile profile,
                                         List<InterviewQuestion> questions,
                                         Integer maxQuestionCount) {
        this(targetRole, profile, questions, maxQuestionCount, null, null);
    }

    public InterviewSessionCreateRequest(String targetRole,
                                         InterviewPreparationProfile profile,
                                         List<InterviewQuestion> questions,
                                         Integer maxQuestionCount,
                                         InterviewConfigurationSnapshot configuration) {
        this(targetRole, profile, questions, maxQuestionCount, configuration, null);
    }
}
