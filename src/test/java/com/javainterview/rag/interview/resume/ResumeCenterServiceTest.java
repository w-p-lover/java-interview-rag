package com.javainterview.rag.interview.resume;

import com.javainterview.rag.knowledge.document.PdfKnowledgeDocumentExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResumeCenterServiceTest {

    @Test
    void createsRecordAppendsVersionAndKeepsFullTextOptIn() {
        InMemoryStore store = new InMemoryStore();
        ResumeCenterService service = new ResumeCenterService(
                store, new PdfKnowledgeDocumentExtractor());

        ResumeCenterService.SaveResult first = service.save(
                textFile("resume-v1.md", "项目：知识库\n负责：RAG 检索"),
                null, "Java 后端简历", "Java 后端工程师", true);
        ResumeCenterService.SaveResult second = service.save(
                textFile("resume-v2.md", "项目：知识库\n负责：RAG 检索与评估"),
                first.detail().id(), "Java 后端简历", "Java / AI 工程师", false);

        assertFalse(first.duplicate());
        assertFalse(second.duplicate());
        assertEquals(2, second.detail().versions().size());
        assertEquals(2, second.detail().versions().get(0).versionNumber());
        assertEquals(second.savedVersionId(), second.detail().currentVersionId());
        assertEquals("Java / AI 工程师", second.detail().targetRole());
        assertNull(second.detail().versions().get(0).fullText());
        assertEquals("项目：知识库\n负责：RAG 检索",
                second.detail().versions().get(1).fullText());
    }

    @Test
    void returnsExistingVersionForDuplicateContentWithoutCreatingAnotherRecord() {
        InMemoryStore store = new InMemoryStore();
        ResumeCenterService service = new ResumeCenterService(
                store, new PdfKnowledgeDocumentExtractor());
        MockMultipartFile file = textFile("resume.md", "项目：面试训练平台");

        ResumeCenterService.SaveResult first = service.save(
                file, null, "主简历", "Java 后端工程师", true);
        ResumeCenterService.SaveResult duplicate = service.save(
                file, null, "重复简历", "其他岗位", false);

        assertTrue(duplicate.duplicate());
        assertEquals(first.detail().id(), duplicate.detail().id());
        assertEquals(first.savedVersionId(), duplicate.savedVersionId());
        assertEquals(1, service.list().size());
        assertEquals(1, duplicate.detail().versions().size());
    }

    @Test
    void switchesCurrentVersionAndRejectsVersionFromAnotherRecord() {
        InMemoryStore store = new InMemoryStore();
        ResumeCenterService service = new ResumeCenterService(
                store, new PdfKnowledgeDocumentExtractor());
        ResumeCenterService.SaveResult first = service.save(
                textFile("a-v1.txt", "A 第一版"), null, "A", "Java", true);
        ResumeCenterService.SaveResult second = service.save(
                textFile("a-v2.txt", "A 第二版"), first.detail().id(), "A", "Java", true);
        ResumeCenterService.SaveResult other = service.save(
                textFile("b-v1.txt", "B 第一版"), null, "B", "AI", true);

        ResumeCenterService.ResumeDetail switched = service.switchCurrent(
                first.detail().id(), first.savedVersionId(), second.detail().version());

        assertEquals(first.savedVersionId(), switched.currentVersionId());
        assertThrows(IllegalArgumentException.class, () -> service.switchCurrent(
                first.detail().id(), other.savedVersionId(), switched.version()));
    }

    @Test
    void rejectsUnsupportedOrOversizedFilesBeforeStorage() {
        InMemoryStore store = new InMemoryStore();
        ResumeCenterService service = new ResumeCenterService(
                store, new PdfKnowledgeDocumentExtractor());

        assertThrows(IllegalArgumentException.class, () -> service.save(
                new MockMultipartFile("file", "resume.docx", "application/octet-stream", "x".getBytes()),
                null, "简历", "Java", true));
        assertThrows(IllegalArgumentException.class, () -> service.save(
                new MockMultipartFile("file", "resume.txt", "text/plain", new byte[10 * 1024 * 1024 + 1]),
                null, "简历", "Java", true));
        assertTrue(store.records.isEmpty());
    }

    private MockMultipartFile textFile(String name, String content) {
        return new MockMultipartFile("file", name, "text/plain",
                content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static final class InMemoryStore implements ResumeCenterStore {
        private final Map<String, ResumeRecord> records = new LinkedHashMap<>();
        private final Map<String, ResumeVersion> versions = new LinkedHashMap<>();

        @Override
        public List<ResumeRecord> findAll() {
            return new ArrayList<>(records.values());
        }

        @Override
        public Optional<ResumeRecord> findRecord(String id) {
            return Optional.ofNullable(records.get(id));
        }

        @Override
        public Optional<ResumeVersion> findVersion(String id) {
            return versions.values().stream().filter(version -> version.id().equals(id)).findFirst();
        }

        @Override
        public List<ResumeVersion> findVersions(String resumeId) {
            return versions.values().stream()
                    .filter(version -> version.resumeId().equals(resumeId))
                    .sorted(java.util.Comparator.comparingInt(ResumeVersion::versionNumber).reversed())
                    .toList();
        }

        @Override
        public Optional<ResumeVersion> findVersionByHash(String contentHash) {
            return versions.values().stream()
                    .filter(version -> version.contentHash().equals(contentHash))
                    .findFirst();
        }

        @Override
        public SaveVersionResult saveVersion(SaveVersionRequest request) {
            Optional<ResumeVersion> existing = findVersionByHash(request.contentHash());
            if (existing.isPresent()) {
                return new SaveVersionResult(
                        records.get(existing.get().resumeId()), existing.get(), true);
            }
            LocalDateTime now = request.createdAt();
            String recordId = request.resumeId() == null
                    ? "resume-" + (records.size() + 1) : request.resumeId();
            ResumeRecord current = records.get(recordId);
            if (current == null && request.resumeId() != null) {
                throw new java.util.NoSuchElementException("简历记录不存在");
            }
            int versionNumber = findVersions(recordId).size() + 1;
            String versionId = "version-" + (versions.size() + 1);
            ResumeVersion version = new ResumeVersion(
                    versionId, recordId, versionNumber, request.contentHash(),
                    request.originalFilename(), request.contentType(), request.fileSize(),
                    request.fullText(), now);
            ResumeRecord updated = new ResumeRecord(
                    recordId, request.name(), request.targetRole(), versionId,
                    current == null ? 0 : current.version() + 1,
                    current == null ? now : current.createdAt(), now);
            versions.put(versionId, version);
            records.put(recordId, updated);
            return new SaveVersionResult(updated, version, false);
        }

        @Override
        public boolean switchCurrent(String resumeId, String versionId,
                                     long expectedVersion, LocalDateTime updatedAt) {
            ResumeRecord current = records.get(resumeId);
            ResumeVersion version = versions.get(versionId);
            if (current == null || current.version() != expectedVersion
                    || version == null || !version.resumeId().equals(resumeId)) {
                return false;
            }
            records.put(resumeId, new ResumeRecord(
                    current.id(), current.name(), current.targetRole(), versionId,
                    current.version() + 1, current.createdAt(), updatedAt));
            return true;
        }
    }
}
