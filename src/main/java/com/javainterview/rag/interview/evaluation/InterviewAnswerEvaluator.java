package com.javainterview.rag.interview.evaluation;

import com.javainterview.rag.interview.preparation.InterviewPreparationProfile;
import com.javainterview.rag.interview.question.InterviewQuestion;

public interface InterviewAnswerEvaluator {

    InterviewAnswerAssessment evaluate(InterviewPreparationProfile profile,
                                       InterviewQuestion question,
                                       String answer);
}
