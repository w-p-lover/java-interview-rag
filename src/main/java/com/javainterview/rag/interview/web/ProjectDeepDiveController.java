package com.javainterview.rag.interview.web;

import com.javainterview.rag.interview.deepdive.ProjectDeepDiveRequest;
import com.javainterview.rag.interview.deepdive.ProjectDeepDiveResult;
import com.javainterview.rag.interview.deepdive.ProjectDeepDiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Profile("storage")
@RequestMapping("/api/interviews/project-deep-dives")
@Tag(name = "项目深挖", description = "基于已确认项目事实生成四维深挖题")
public class ProjectDeepDiveController {

    private final ProjectDeepDiveService service;

    public ProjectDeepDiveController(ProjectDeepDiveService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "生成项目深挖题", description = "固定覆盖基础、取舍、故障和扩展；知识库只作为可选答题背景")
    public ProjectDeepDiveResult generate(@RequestBody ProjectDeepDiveRequest request) {
        return service.generate(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidArgument(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }
}
