package com.javainterview.rag.knowledge.persistence;
import com.fasterxml.jackson.databind.ObjectMapper; import com.javainterview.rag.knowledge.review.*; import com.javainterview.rag.interview.evaluation.InterviewAnswerEvaluation; import java.util.*;
public class MySqlKnowledgeReviewAttemptStore implements KnowledgeReviewAttemptStore {
 private final KnowledgeReviewAttemptMapper mapper; private final ObjectMapper objectMapper;
 public MySqlKnowledgeReviewAttemptStore(KnowledgeReviewAttemptMapper mapper,ObjectMapper objectMapper){this.mapper=mapper;this.objectMapper=objectMapper;}
 public KnowledgeReviewAttempt create(KnowledgeReviewAttempt attempt){try{KnowledgeReviewAttemptEntity e=new KnowledgeReviewAttemptEntity();e.setId(attempt.id());e.setTaskId(attempt.taskId());e.setAnswer(attempt.answer());e.setEvaluationJson(objectMapper.writeValueAsString(attempt.evaluation()));e.setCreatedAt(attempt.createdAt());mapper.insertAttempt(e);return attempt;}catch(Exception ex){throw new IllegalStateException("知识复习回答保存失败",ex);}}
 public List<KnowledgeReviewAttempt> findByTaskId(String taskId){return mapper.findByTaskId(taskId).stream().map(e->{try{return new KnowledgeReviewAttempt(e.getId(),e.getTaskId(),e.getAnswer(),objectMapper.readValue(e.getEvaluationJson(),InterviewAnswerEvaluation.class),e.getCreatedAt());}catch(Exception ex){throw new IllegalStateException("知识复习回答数据损坏",ex);}}).toList();}
}
