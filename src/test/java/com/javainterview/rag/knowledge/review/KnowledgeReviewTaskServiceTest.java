package com.javainterview.rag.knowledge.review;

import com.javainterview.rag.knowledge.rag.RagAnswerClaimEvidenceStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class KnowledgeReviewTaskServiceTest {
    @Test
    void createsDeterministicTaskAndRejectsSupportedClaim() {
        InMemoryStore store = new InMemoryStore();
        KnowledgeReviewTaskService service = new KnowledgeReviewTaskService(store);
        KnowledgeReviewTask first = service.create("HashMap 是什么", "缺少树化条件", RagAnswerClaimEvidenceStatus.EVIDENCE_INSUFFICIENT, List.of(2, 1, 1));
        KnowledgeReviewTask second = service.create(" HashMap 是什么 ", "缺少树化条件", RagAnswerClaimEvidenceStatus.EVIDENCE_INSUFFICIENT, List.of(1, 2));
        assertEquals(first.id(), second.id());
        assertEquals(List.of(1, 2), first.sourceNumbers());
        assertThrows(IllegalArgumentException.class, () -> service.create("问题", "断言", RagAnswerClaimEvidenceStatus.SUPPORTED, List.of()));
    }

    @Test
    void validatesDateRangeAndKeepsCompletionIdempotent() {
        InMemoryStore store = new InMemoryStore();
        KnowledgeReviewTaskService service = new KnowledgeReviewTaskService(store);
        KnowledgeReviewTask task = service.create("问题", "断言", RagAnswerClaimEvidenceStatus.CITATION_INVALID, List.of());
        assertThrows(IllegalArgumentException.class, () -> service.find(null, LocalDate.now(), LocalDate.now().minusDays(1)));
        assertEquals(KnowledgeReviewTaskStatus.COMPLETED, service.complete(task.id()).status());
        assertEquals(KnowledgeReviewTaskStatus.COMPLETED, service.complete(task.id()).status());
    }

    private static final class InMemoryStore implements KnowledgeReviewTaskStore {
        private final List<KnowledgeReviewTask> tasks = new ArrayList<>();
        public KnowledgeReviewTask createIfAbsent(KnowledgeReviewTask task) {
            return tasks.stream().filter(item -> item.id().equals(task.id())).findFirst().orElseGet(() -> { tasks.add(task); return task; });
        }
        public List<KnowledgeReviewTask> find(KnowledgeReviewTaskStatus status, LocalDate from, LocalDate to) {
            return tasks.stream().filter(task -> status == null || task.status() == status).toList();
        }
        public Optional<KnowledgeReviewTask> complete(String id, java.time.LocalDateTime at) {
            return tasks.stream().filter(task -> task.id().equals(id)).findFirst().map(task -> {
                KnowledgeReviewTask updated = task.status() == KnowledgeReviewTaskStatus.COMPLETED ? task : new KnowledgeReviewTask(task.id(), task.question(), task.claim(), task.evidenceStatus(), task.sourceNumbers(), KnowledgeReviewTaskStatus.COMPLETED, task.plannedReviewDate(), task.createdAt(), at);
                tasks.replaceAll(item -> item.id().equals(id) ? updated : item); return updated;
            });
        }
        public Optional<KnowledgeReviewTask> schedule(String id, LocalDate date) { return tasks.stream().filter(task -> task.id().equals(id)).findFirst(); }
    }
}
