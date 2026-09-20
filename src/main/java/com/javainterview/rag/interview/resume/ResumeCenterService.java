package com.javainterview.rag.interview.resume;

import com.javainterview.rag.knowledge.document.PdfKnowledgeDocumentExtractor;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;

public class ResumeCenterService {

    public static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    public static final int MAX_TEXT_LENGTH = 50_000;

    private final ResumeCenterStore store;
    private final PdfKnowledgeDocumentExtractor pdfExtractor;

    public ResumeCenterService(ResumeCenterStore store,
                               PdfKnowledgeDocumentExtractor pdfExtractor) {
        this.store = Objects.requireNonNull(store, "简历中心存储不能为空");
        this.pdfExtractor = Objects.requireNonNull(pdfExtractor, "PDF 解析器不能为空");
    }

    public List<ResumeSummary> list() {
        return store.findAll().stream().map(record -> {
            List<ResumeVersion> versions = store.findVersions(record.id());
            ResumeVersion current = versions.stream()
                    .filter(version -> version.id().equals(record.currentVersionId()))
                    .findFirst().orElse(null);
            return new ResumeSummary(
                    record.id(), record.name(), record.targetRole(), record.currentVersionId(),
                    record.version(), versions.size(),
                    current == null ? null : current.versionNumber(),
                    current != null && current.fullTextSaved(),
                    record.createdAt(), record.updatedAt());
        }).toList();
    }

    public ResumeDetail detail(String resumeId) {
        ResumeRecord record = requireRecord(resumeId);
        return ResumeDetail.from(record, store.findVersions(record.id()));
    }

    public SaveResult save(MultipartFile file,
                           String resumeId,
                           String name,
                           String targetRole,
                           boolean saveFullText) {
        byte[] content = requireFile(file);
        String contentHash = hash(content);
        ResumeVersion duplicate = store.findVersionByHash(contentHash).orElse(null);
        if (duplicate != null) {
            return new SaveResult(detail(duplicate.resumeId()), duplicate.id(), true);
        }

        ResumeRecord existing = resumeId == null || resumeId.isBlank()
                ? null : requireRecord(resumeId.strip());
        String normalizedName = name == null || name.isBlank()
                ? existing == null ? null : existing.name() : name;
        String normalizedTargetRole = targetRole == null || targetRole.isBlank()
                ? existing == null ? null : existing.targetRole() : targetRole;
        normalizedName = ResumeRecord.text(normalizedName, 200, "简历名称");
        normalizedTargetRole = ResumeRecord.text(normalizedTargetRole, 200, "目标岗位");

        String filename = requireFilename(file.getOriginalFilename());
        String text = parse(filename, file, content);
        ResumeCenterStore.SaveVersionResult result = store.saveVersion(
                new ResumeCenterStore.SaveVersionRequest(
                        existing == null ? null : existing.id(), normalizedName, normalizedTargetRole,
                        contentHash, filename, contentType(filename), content.length,
                        saveFullText ? text : null, LocalDateTime.now()));
        return new SaveResult(detail(result.record().id()), result.version().id(), result.duplicate());
    }

    public ResumeDetail switchCurrent(String resumeId, String versionId, long expectedVersion) {
        ResumeRecord record = requireRecord(resumeId);
        String normalizedVersionId = ResumeRecord.text(versionId, 36, "简历版本 ID");
        boolean belongsToRecord = store.findVersions(record.id()).stream()
                .anyMatch(version -> version.id().equals(normalizedVersionId));
        if (!belongsToRecord) {
            throw new IllegalArgumentException("目标版本不属于当前简历记录");
        }
        if (!store.switchCurrent(record.id(), normalizedVersionId, expectedVersion, LocalDateTime.now())) {
            throw new ResumeCenterConflictException("简历记录已更新，请刷新后重试");
        }
        return detail(record.id());
    }

    private ResumeRecord requireRecord(String resumeId) {
        String normalizedId = ResumeRecord.text(resumeId, 36, "简历记录 ID");
        return store.findRecord(normalizedId)
                .orElseThrow(() -> new NoSuchElementException("简历记录不存在"));
    }

    private byte[] requireFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("简历文件不能为空");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("简历文件不能超过 10 MiB");
        }
        requireFilename(file.getOriginalFilename());
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new IllegalArgumentException("读取简历文件失败", exception);
        }
    }

    private String requireFilename(String filename) {
        String normalized = ResumeRecord.text(filename, 255, "简历文件名");
        String lower = normalized.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".pdf") && !lower.endsWith(".md")
                && !lower.endsWith(".markdown") && !lower.endsWith(".txt")) {
            throw new IllegalArgumentException("当前仅支持 Markdown、TXT 或 PDF 简历");
        }
        return normalized;
    }

    private String parse(String filename, MultipartFile file, byte[] content) {
        String text = filename.toLowerCase(Locale.ROOT).endsWith(".pdf")
                ? pdfExtractor.extract(file).markdownContent()
                : decodeUtf8(content);
        String normalized = text.strip();
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("简历未解析到可用正文");
        }
        if (normalized.length() > MAX_TEXT_LENGTH) {
            throw new IllegalArgumentException("简历正文不能超过 50000 个字符");
        }
        return normalized;
    }

    private String decodeUtf8(byte[] content) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content)).toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("文本简历必须使用 UTF-8 编码");
        }
    }

    private String contentType(String filename) {
        return filename.toLowerCase(Locale.ROOT).endsWith(".pdf")
                ? "application/pdf" : "text/plain";
    }

    private String hash(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256", exception);
        }
    }

    public record SaveResult(ResumeDetail detail, String savedVersionId, boolean duplicate) { }

    public record ResumeSummary(
            String id,
            String name,
            String targetRole,
            String currentVersionId,
            long version,
            int versionCount,
            Integer currentVersionNumber,
            boolean currentFullTextSaved,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) { }

    public record ResumeDetail(
            String id,
            String name,
            String targetRole,
            String currentVersionId,
            long version,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            List<ResumeVersion> versions
    ) {
        static ResumeDetail from(ResumeRecord record, List<ResumeVersion> versions) {
            return new ResumeDetail(
                    record.id(), record.name(), record.targetRole(), record.currentVersionId(),
                    record.version(), record.createdAt(), record.updatedAt(), List.copyOf(versions));
        }
    }
}
