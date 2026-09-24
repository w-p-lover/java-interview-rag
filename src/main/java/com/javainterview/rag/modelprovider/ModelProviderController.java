package com.javainterview.rag.modelprovider;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
