package com.javainterview.rag.knowledge.document;

import com.javainterview.rag.file.FileAsset;
import com.javainterview.rag.file.FileAssetService;
import com.javainterview.rag.file.FilePurpose;
import com.javainterview.rag.file.StoredFileContent;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskInput;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class KnowledgeStoredFileParser {

    private final FileAssetService files;
    private final PdfKnowledgeDocumentExtractor pdfExtractor;

    public KnowledgeStoredFileParser(FileAssetService files,
                                     PdfKnowledgeDocumentExtractor pdfExtractor) {
        this.files = Objects.requireNonNull(files, "文件服务不能为空");
        this.pdfExtractor = Objects.requireNonNull(pdfExtractor, "PDF 解析器不能为空");
    }

    public KnowledgeDocumentTaskInput load(String source, String fileId) {
        validateSource(source);
        StoredFileContent stored = files.read(fileId);
        FileAsset asset = stored.asset();
        if (asset.purpose() != FilePurpose.KNOWLEDGE) {
            throw new IllegalArgumentException("文件用途不是知识资料");
        }
        return parse(source, asset, stored.content());
    }

    public void validateFilename(String filename) {
        String normalized = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        if (!normalized.endsWith(".pdf") && !normalized.endsWith(".md")
                && !normalized.endsWith(".markdown") && !normalized.endsWith(".txt")) {
            throw new IllegalArgumentException("当前仅支持 Markdown、TXT 或 PDF 文件");
        }
    }

    private KnowledgeDocumentTaskInput parse(String source, FileAsset asset, byte[] content) {
        String filename = asset.originalFilename();
        validateFilename(filename);
        if (filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            PdfKnowledgeDocumentExtraction extraction = pdfExtractor.extract(filename, content);
            return new KnowledgeDocumentTaskInput(asset.id(), source, extraction.markdownContent(), "PDF",
                    extraction.pageCount(), extraction.pagesNeedingOcr(), extraction.warnings(),
                    pdfExtractor.toSections(source, extraction.blocks()));
        }
        return new KnowledgeDocumentTaskInput(asset.id(), source, decodeUtf8(content), "MARKDOWN",
                null, List.of(), List.of(), List.of());
    }

    private void validateSource(String source) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("文档来源不能为空");
        }
        if (!source.equals(source.strip())) {
            throw new IllegalArgumentException("文档来源不能包含首尾空白");
        }
    }

    private String decodeUtf8(byte[] content) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(content)).toString();
        } catch (CharacterCodingException exception) {
            throw new IllegalArgumentException("文本资料必须使用 UTF-8 编码");
        }
    }
}
