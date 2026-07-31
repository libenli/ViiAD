package com.mrd.ad.system.controller;

import com.mrd.ad.business.partner.domain.AdAgent;
import com.mrd.ad.business.partner.dto.AgentCreateRequest;
import com.mrd.ad.business.partner.dto.AgentQuery;
import com.mrd.ad.business.partner.dto.AgentUpdateRequest;
import com.mrd.ad.business.partner.service.AdAgentService;
import com.mrd.ad.common.annotation.RequiresPermission;
import com.mrd.ad.common.core.ApiResult;
import com.mrd.ad.common.core.PageResult;
import com.mrd.ad.logging.annotation.OperLog;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agents")
public class AdAgentController {

    private final AdAgentService adAgentService;

    public AdAgentController(AdAgentService adAgentService) {
        this.adAgentService = adAgentService;
    }

    @OperLog(module = "代理商", businessType = "QUERY")
    @GetMapping
    public ApiResult<PageResult<AdAgent>> page(AgentQuery query) {
        return ApiResult.success(adAgentService.page(query));
    }

    @GetMapping("/{id}")
    public ApiResult<AdAgent> detail(@PathVariable Long id) {
        return ApiResult.success(adAgentService.getDetail(id));
    }

    @OperLog(module = "代理商", businessType = "CREATE")
    @RequiresPermission("partner:manage")
    @PostMapping
    public ApiResult<AdAgent> create(@Validated @RequestBody AgentCreateRequest request) {
        return ApiResult.success(adAgentService.create(request));
    }

    @OperLog(module = "代理商", businessType = "UPDATE")
    @RequiresPermission("partner:manage")
    @PutMapping("/{id}")
    public ApiResult<AdAgent> update(@PathVariable Long id, @Validated @RequestBody AgentUpdateRequest request) {
        return ApiResult.success(adAgentService.update(id, request));
    }

    @RequiresPermission("partner:manage")
    @PostMapping("/{id}/enable")
    public ApiResult<AdAgent> enable(@PathVariable Long id) {
        return ApiResult.success(adAgentService.enable(id));
    }

    @RequiresPermission("partner:manage")
    @PostMapping("/{id}/disable")
    public ApiResult<AdAgent> disable(@PathVariable Long id) {
        return ApiResult.success(adAgentService.disable(id));
    }
}
