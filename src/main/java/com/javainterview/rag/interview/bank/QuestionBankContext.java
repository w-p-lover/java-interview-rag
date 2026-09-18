package com.javainterview.rag.interview.bank;

import java.util.List;

public record QuestionBankContext(String questionId, String referenceAnswer,
                                  List<String> keyPoints, String scoringRubric,
                                  boolean fixedFollowUp, Integer parentQuestionIndex) {
    public QuestionBankContext {
        questionId = BankQuestion.text(questionId, 36, "题库题目 ID");
        referenceAnswer = BankQuestion.text(referenceAnswer, 8000, "参考答案");
        scoringRubric = BankQuestion.text(scoringRubric, 4000, "评分标准");
        keyPoints = List.copyOf(keyPoints);
        if (fixedFollowUp && (parentQuestionIndex == null || parentQuestionIndex < 0)) {
            throw new IllegalArgumentException("固定追问必须保存父题序号");
        }
    }
}
