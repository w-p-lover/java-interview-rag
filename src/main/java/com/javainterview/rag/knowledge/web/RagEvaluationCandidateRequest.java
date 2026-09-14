package com.javainterview.rag.knowledge.web;

import java.util.List;

public record RagEvaluationCandidateRequest(String question, List<String> expectedSources,
                                            List<String> expectedAnswerPoints) { }
