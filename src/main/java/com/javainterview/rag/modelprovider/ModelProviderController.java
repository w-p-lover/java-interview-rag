package com.javainterview.rag.modelprovider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@Profile("storage")
@RequestMapping("/api/model-providers")
@Tag(name = "模型 Provider", description = "查看当前实例的脱敏模型配置并执行固定能力验证")
public class ModelProviderController {

    private final ModelProviderService service;

    public ModelProviderController(ModelProviderService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "查询当前模型能力", description = "返回 Chat 与 Embedding 的模型、端点来源、用途和配置状态，不返回 API Key")
    public ModelProviderOverview overview() {
        return service.overview();
    }

    @PostMapping("/{capability}/test")
    @Operation(summary = "验证模型能力", description = "使用固定无敏感样本执行一次 Chat 结构化输出或 Embedding 维度验证")
    public ModelProviderTestResult test(@PathVariable ModelCapability capability) {
        return service.test(capability);
    }

    @PostMapping("/providers")
    @Operation(summary = "新增 Provider")
    public ModelProviderOverview create(@RequestBody ModelProviderRequest request) {
        return service.create(request);
    }

    @PutMapping("/providers/{id}")
    @Operation(summary = "更新 Provider", description = "API Key 留空时保留现有密钥")
    public ModelProviderOverview update(
            @PathVariable String id, @RequestBody ModelProviderRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/providers/{id}")
    @Operation(summary = "删除非默认 Provider")
    public ModelProviderOverview delete(
            @PathVariable String id, @RequestParam Long version) {
        return service.delete(id, version);
    }

    @PostMapping("/providers/{id}/{capability}/test")
    @Operation(summary = "验证指定 Provider 能力")
    public ModelProviderTestResult test(
            @PathVariable String id, @PathVariable ModelCapability capability) {
        return service.test(id, capability);
    }

    @PutMapping("/defaults")
    @Operation(summary = "更新默认 Chat 与 Embedding Provider")
    public ModelProviderOverview updateDefaults(@RequestBody ModelProviderDefaultsRequest request) {
        return service.updateDefaults(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> invalid(IllegalArgumentException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> missing(NoSuchElementException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(ModelProviderConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> conflict(ModelProviderConflictException exception) {
        return Map.of("message", exception.getMessage());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> duplicate() {
        return Map.of("message", "Provider ID 已存在");
    }
}
