package com.javainterview.rag.knowledge.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.javainterview.rag.knowledge.document.KnowledgeDocumentSummary;
import com.javainterview.rag.knowledge.document.MarkdownContentImporter;
import com.javainterview.rag.knowledge.document.MarkdownImportRegistry;
import com.javainterview.rag.knowledge.document.MarkdownImportResult;
import com.javainterview.rag.knowledge.document.MarkdownRebuildResult;
import com.javainterview.rag.knowledge.document.KnowledgeStoredFileParser;
import com.javainterview.rag.file.FileAsset;
import com.javainterview.rag.file.FileAssetConflictException;
import com.javainterview.rag.file.FileAssetService;
import com.javainterview.rag.file.FileAssetUnavailableException;
import com.javainterview.rag.file.FilePurpose;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTask;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskInput;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskOperation;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskService;
import com.javainterview.rag.knowledge.task.KnowledgeDocumentTaskCancellationException;
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
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/knowledge/documents")
@Tag(name = "知识资料", description = "Markdown/TXT/PDF 知识资料导入、重建与删除")
public class MarkdownDocumentController {

    private final MarkdownContentImporter contentImporter;
    private final MarkdownImportRegistry importRegistry;
    private final KnowledgeDocumentTaskService taskService;
    private final FileAssetService files;
    private final KnowledgeStoredFileParser storedFileParser;

    public MarkdownDocumentController(MarkdownContentImporter contentImporter,
                                      MarkdownImportRegistry importRegistry,
                                      KnowledgeDocumentTaskService taskService,
                                      FileAssetService files,
                                      KnowledgeStoredFileParser storedFileParser) {
        this.contentImporter = contentImporter;
        this.importRegistry = importRegistry;
        this.taskService = taskService;
        this.files = files;
        this.storedFileParser = storedFileParser;
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
        return KnowledgeDocumentDetailsResponse.from(versions,
                version -> importRegistry.hasSnapshot(source, version));
    }

    @PostMapping("/rollback")
    @Operation(summary = "回退知识资料历史版本")
    public MarkdownRollbackResponse rollbackDocument(@RequestParam String source,
                                                      @RequestParam String version) {
        validateSource(source);
        return MarkdownRollbackResponse.from(contentImporter.rollbackToVersion(source, version));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "导入知识资料", description = "支持 UTF-8 Markdown/TXT 和带文本层的 PDF 文件")
    public MarkdownImportResponse importDocument(@RequestParam MultipartFile file,
                                                 @RequestParam String source) throws IOException {
        KnowledgeDocumentTaskInput upload = storeAndParse(file, source);
        MarkdownImportResult result = upload.structured()
                ? contentImporter.importStructuredContent(source, upload.content(), upload.sections(),
                null, upload.fileId())
                : contentImporter.importContent(source, upload.content(), null, upload.fileId());
        return MarkdownImportResponse.from(result, upload.format(), upload.pageCount(),
                upload.pagesNeedingOcr(), upload.warnings());
    }

    @PostMapping(value = "/rebuild", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "重建知识资料向量")
    public MarkdownRebuildResponse rebuildDocument(@RequestParam MultipartFile file,
                                                    @RequestParam String source) throws IOException {
        KnowledgeDocumentTaskInput upload = storeAndParse(file, source);
        MarkdownRebuildResult result = upload.structured()
                ? contentImporter.rebuildStructuredContent(source, upload.content(), upload.sections(),
                null, upload.fileId())
                : contentImporter.rebuildContent(source, upload.content(), null, upload.fileId());
        return MarkdownRebuildResponse.from(result,
                upload.format(), upload.pageCount(), upload.pagesNeedingOcr(), upload.warnings());
    }

    @PostMapping(value = "/import-tasks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "异步导入知识资料")
    public KnowledgeDocumentTaskSubmitResponse submitImportTask(@RequestParam MultipartFile file,
                                                                 @RequestParam String source) {
        FileAsset asset = storeUpload(file, source);
        KnowledgeDocumentTask task = taskService.submit(
                KnowledgeDocumentTaskOperation.IMPORT, source, asset.id());
        return new KnowledgeDocumentTaskSubmitResponse(task.taskId(), task.attemptId(), task.source(), task.fileId(),
                task.operation(), task.status());
    }

    @PostMapping(value = "/rebuild-tasks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "异步重建知识资料")
    public KnowledgeDocumentTaskSubmitResponse submitRebuildTask(@RequestParam MultipartFile file,
                                                                  @RequestParam String source) {
        FileAsset asset = storeUpload(file, source);
        KnowledgeDocumentTask task = taskService.submit(
                KnowledgeDocumentTaskOperation.REBUILD, source, asset.id());
        return new KnowledgeDocumentTaskSubmitResponse(task.taskId(), task.attemptId(), task.source(), task.fileId(),
                task.operation(), task.status());
    }

    @GetMapping("/tasks/{taskId}")
    @Operation(summary = "查询知识资料异步任务")
    public KnowledgeDocumentTaskResponse findTask(
            @org.springframework.web.bind.annotation.PathVariable String taskId) {
        return KnowledgeDocumentTaskResponse.from(taskService.find(taskId));
    }

    @GetMapping("/tasks")
    @Operation(summary = "查询最近知识资料任务")
    public List<KnowledgeDocumentTaskResponse> findRecentTasks(
            @RequestParam(defaultValue = "8") int limit) {
        return taskService.findRecent(limit).stream()
                .map(KnowledgeDocumentTaskResponse::from)
                .toList();
    }

    @org.springframework.web.bind.annotation.PatchMapping("/tasks/{taskId}/cancel")
    @Operation(summary = "取消排队中的知识资料任务")
    public KnowledgeDocumentTaskResponse cancelTask(
            @org.springframework.web.bind.annotation.PathVariable String taskId) {
        return KnowledgeDocumentTaskResponse.from(taskService.cancel(taskId));
    }

    @DeleteMapping
    @Operation(summary = "删除知识资料")
    public MarkdownDeleteResponse deleteDocument(@RequestParam String source) {
        List<String> fileIds = importRegistry.findVersionsBySource(source).stream()
                .map(com.javainterview.rag.knowledge.document.KnowledgeDocumentSummary::fileId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        boolean deleted = contentImporter.deleteBySource(source);
        if (deleted) {
            fileIds.forEach(this::deleteFileWhenUnreferenced);
        }
        return new MarkdownDeleteResponse(source, deleted);
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

    @ExceptionHandler(KnowledgeDocumentTaskCancellationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleTaskCancellation(KnowledgeDocumentTaskCancellationException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(TaskRejectedException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Map<String, String> handleTaskRejected(TaskRejectedException exception) {
        return Map.of("message", "当前资料索引任务较多，请稍后重试");
    }

    @ExceptionHandler(FileAssetConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleFileConflict(FileAssetConflictException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(FileAssetUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> handleFileUnavailable(FileAssetUnavailableException exception) {
        return Map.of("message", exception.getMessage());
    }

    private void validateSource(String source) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("文档来源不能为空");
        }
        if (!source.equals(source.strip())) {
            throw new IllegalArgumentException("文档来源不能包含首尾空白");
        }
    }

    private FileAsset storeUpload(MultipartFile file, String source) {
        validateSource(source);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("资料文件不能为空");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("资料文件名不能为空");
        }
        storedFileParser.validateFilename(filename);
        try {
            return files.store(FilePurpose.KNOWLEDGE, filename, file.getContentType(), file.getBytes());
        } catch (IOException exception) {
            throw new IllegalArgumentException("资料文件读取失败");
        }
    }

    private KnowledgeDocumentTaskInput storeAndParse(MultipartFile file, String source) {
        FileAsset asset = storeUpload(file, source);
        try {
            return storedFileParser.load(source, asset.id());
        } catch (RuntimeException exception) {
            try {
                files.deleteIfUnreferenced(asset.id());
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    private void deleteFileWhenUnreferenced(String fileId) {
        try {
            files.deleteIfUnreferenced(fileId);
        } catch (FileAssetConflictException ignored) {
            // 同一 Hash 可被多个资料版本复用；仍有引用时保留共享对象即可。
        }
    }
}
