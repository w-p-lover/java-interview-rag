package com.javainterview.rag.file;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/files")
@Tag(name = "文件对象", description = "知识资料与显式保存简历的原文件下载和一致性检查")
public class FileAssetController {

    private final FileAssetService service;

    public FileAssetController(FileAssetService service) {
        this.service = service;
    }

    @GetMapping("/{id}/content")
    @Operation(summary = "下载原文件")
    public ResponseEntity<byte[]> download(@PathVariable String id) {
        StoredFileContent stored = service.read(id);
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(stored.asset().contentType());
        } catch (IllegalArgumentException exception) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(stored.asset().originalFilename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(stored.content().length)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(stored.content());
    }

    @GetMapping("/{id}/consistency")
    @Operation(summary = "检查文件元数据与对象一致性")
    public FileAssetService.FileConsistency inspect(@PathVariable String id) {
        return service.inspect(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "软删除无业务引用的文件")
    public FileAsset delete(@PathVariable String id) {
        return service.deleteIfUnreferenced(id);
    }

    @ExceptionHandler({IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalid(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> missing(NoSuchElementException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(FileAssetConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> conflict(FileAssetConflictException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(FileAssetUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Map<String, String> unavailable(FileAssetUnavailableException exception) {
        return Map.of("message", exception.getMessage());
    }
}
