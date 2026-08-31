package com.javainterview.rag.observability;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/observability")
@Tag(name = "业务观测", description = "查看当前实例的业务请求成功率和耗时摘要")
public class BusinessObservabilityController {
    private final BusinessObservabilityService service;

    public BusinessObservabilityController(BusinessObservabilityService service) {
        this.service = service;
    }

    @GetMapping("/overview")
    @Operation(summary = "查询业务观测概览", description = "按业务路由返回请求量、失败量、失败率和耗时，不返回业务正文")
    public BusinessObservabilityOverview overview() {
        return service.overview();
    }
}
