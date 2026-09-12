package com.javainterview.rag.knowledge.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.javainterview.rag.knowledge.document.KnowledgeDocumentSummary;
import com.javainterview.rag.knowledge.document.MarkdownContentImporter;
import com.javainterview.rag.knowledge.document.MarkdownImportRegistry;
import com.javainterview.rag.knowledge.document.MarkdownImportResult;
import com.javainterview.rag.knowledge.document.MarkdownRebuildResult;
import com.javainterview.rag.knowledge.document.MarkdownSection;
import com.javainterview.rag.knowledge.document.PdfKnowledgeDocumentExtraction;
import com.javainterview.rag.knowledge.document.PdfKnowledgeDocumentExtractor;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTask;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskInput;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskOperation;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.task.TaskRejectedException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Locale;

@RestController
@RequestMapping("/api/knowledge/documents")
@Tag(name = "知识资料", description = "Markdown/PDF 知识资料导入、重建与删除")
public class MarkdownDocumentController {

    private final MarkdownContentImporter contentImporter;
    private final MarkdownImportRegistry importRegistry;
    private final PdfKnowledgeDocumentExtractor pdfExtractor;
    private final KnowledgeDocumentTaskService taskService;

    public MarkdownDocumentController(MarkdownContentImporter contentImporter,
                                      MarkdownImportRegistry importRegistry,
                                      PdfKnowledgeDocumentExtractor pdfExtractor,
                                      KnowledgeDocumentTaskService taskService) {
        this.contentImporter = contentImporter;
        this.importRegistry = importRegistry;
        this.pdfExtractor = pdfExtractor;
        this.taskService = taskService;
    }

    @GetMapping
    @Operation(summary = "查询当前知识资料", description = "返回每个来源当前生效的知识资料版本")
    public KnowledgeDocumentCatalogResponse findCurrentDocuments() {
        return KnowledgeDocumentCatalogResponse.from(importRegistry.findCurrentDocuments());
    }

    @GetMapping("/detail")
    @Operation(summary = "查询资料版本详情")
    public KnowledgeDocumentDetailsResponse findDocumentDetails(@RequestParam String source) {
        validateSource(source);
        List<KnowledgeDocumentSummary> versions = importRegistry.findVersionsBySource(source);
        if (versions.isEmpty()) {
            throw new NoSuchElementException("资料不存在");
        }
        return KnowledgeDocumentDetailsResponse.from(versions);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "导入知识资料", description = "支持 Markdown 和带文本层的 PDF 文件")
    public MarkdownImportResponse importDocument(@RequestParam MultipartFile file,
                                                 @RequestParam String source) throws IOException {
        KnowledgeUploadContent upload = readContent(file, source);
        MarkdownImportResult result = upload.structured()
                ? contentImporter.importStructuredContent(source, upload.content(), upload.sections())
                : contentImporter.importContent(source, upload.content());
        return MarkdownImportResponse.from(result, upload.format(), upload.pageCount(),
                upload.pagesNeedingOcr(), upload.warnings());
    }

    @PostMapping(value = "/rebuild", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "重建知识资料向量")
    public MarkdownRebuildResponse rebuildDocument(@RequestParam MultipartFile file,
                                                    @RequestParam String source) throws IOException {
        KnowledgeUploadContent upload = readContent(file, source);
        MarkdownRebuildResult result = upload.structured()
                ? contentImporter.rebuildStructuredContent(source, upload.content(), upload.sections())
                : contentImporter.rebuildContent(source, upload.content());
        return MarkdownRebuildResponse.from(result,
                upload.format(), upload.pageCount(), upload.pagesNeedingOcr(), upload.warnings());
    }

    @PostMapping(value = "/import-tasks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "异步导入知识资料")
    public KnowledgeDocumentTaskSubmitResponse submitImportTask(@RequestParam MultipartFile file,
                                                                 @RequestParam String source) {
        KnowledgeUploadContent upload = readContentUnchecked(file, source);
        KnowledgeDocumentTask task = taskService.submit(KnowledgeDocumentTaskOperation.IMPORT,
                toTaskInput(upload, source));
        return new KnowledgeDocumentTaskSubmitResponse(task.taskId(), task.source(),
                task.operation(), task.status());
    }

    @PostMapping(value = "/rebuild-tasks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "异步重建知识资料")
    public KnowledgeDocumentTaskSubmitResponse submitRebuildTask(@RequestParam MultipartFile file,
                                                                  @RequestParam String source) {
        KnowledgeUploadContent upload = readContentUnchecked(file, source);
        KnowledgeDocumentTask task = taskService.submit(KnowledgeDocumentTaskOperation.REBUILD,
                toTaskInput(upload, source));
        return new KnowledgeDocumentTaskSubmitResponse(task.taskId(), task.source(),
                task.operation(), task.status());
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "查询知识资料异步任务")
    public KnowledgeDocumentTaskResponse findTask(
            @org.springframework.web.bind.annotation.PathVariable String taskId) {
        return KnowledgeDocumentTaskResponse.from(taskService.find(taskId));
    }

    @DeleteMapping
    @Operation(summary = "删除知识资料")
    public MarkdownDeleteResponse deleteDocument(@RequestParam String source) {
        return new MarkdownDeleteResponse(source, contentImporter.deleteBySource(source));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleMissingDocument(NoSuchElementException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(TaskRejectedException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Map<String, String> handleTaskRejected(TaskRejectedException exception) {
        return Map.of("message", "当前资料索引任务较多，请稍后重试");
    }

    private void validateSource(String source) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("文档来源不能为空");
        }
        if (!source.equals(source.strip())) {
            throw new IllegalArgumentException("文档来源不能包含首尾空白");
        }
    }

    private KnowledgeUploadContent readContent(MultipartFile file, String source) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("资料文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            PdfKnowledgeDocumentExtraction extraction = pdfExtractor.extract(file);
            return new KnowledgeUploadContent(extraction.markdownContent(), "PDF",
                    extraction.pageCount(), extraction.pagesNeedingOcr(), extraction.warnings(),
                    pdfExtractor.toSections(source, extraction.blocks()));
        }
        return new KnowledgeUploadContent(new String(file.getBytes(), StandardCharsets.UTF_8),
                "MARKDOWN", null, List.of(), List.of(), List.of());
    }

    private KnowledgeUploadContent readContentUnchecked(MultipartFile file, String source) {
        try {
            return readContent(file, source);
        } catch (IOException exception) {
            throw new IllegalArgumentException("资料文件读取失败");
        }
    }

    private KnowledgeDocumentTaskInput toTaskInput(KnowledgeUploadContent upload, String source) {
        return new KnowledgeDocumentTaskInput(source, upload.content(), upload.format(), upload.pageCount(),
                upload.pagesNeedingOcr(), upload.warnings(), upload.sections());
    }

    private record KnowledgeUploadContent(String content, String format, Integer pageCount,
                                          List<Integer> pagesNeedingOcr, List<String> warnings,
                                          List<MarkdownSection> sections) {

        private boolean structured() {
            return "PDF".equals(format);
        }
    }
}
