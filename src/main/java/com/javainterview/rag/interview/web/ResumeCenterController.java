package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.resume.ResumeCenterConflictException;
import com.javainterview.rag.interview.resume.ResumeCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@Profile("storage")
@RequestMapping("/api/resumes")
@Tag(name = "简历中心", description = "简历记录、不可变版本与当前版本管理")
public class ResumeCenterController {

    private final ResumeCenterService service;

    public ResumeCenterController(ResumeCenterService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "查询简历记录")
    public List<ResumeCenterService.ResumeSummary> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询简历版本历史")
    public ResumeCenterService.ResumeDetail detail(@PathVariable String id) {
        return service.detail(id);
    }

    @PostMapping(value = "/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "新建简历或追加不可变版本")
    public ResumeCenterService.SaveResult saveVersion(
            @RequestParam MultipartFile file,
            @RequestParam(required = false) String resumeId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String targetRole,
            @RequestParam(defaultValue = "false") boolean saveFullText) {
        return service.save(file, resumeId, name, targetRole, saveFullText);
    }

    @PatchMapping("/{id}/current-version")
    @Operation(summary = "切换简历当前版本")
    public ResumeCenterService.ResumeDetail switchCurrent(
            @PathVariable String id,
            @RequestBody SwitchCurrentVersionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("切换版本请求不能为空");
        }
        return service.switchCurrent(id, request.versionId(), request.expectedVersion());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleMissingRecord(NoSuchElementException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(ResumeCenterConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleConflict(ResumeCenterConflictException exception) {
        return Map.of("message", exception.getMessage());
    }

    public record SwitchCurrentVersionRequest(String versionId, long expectedVersion) { }
}
