package com.mrd.ad.system.controller;

import com.mrd.ad.business.ad.domain.AdOrder;
import com.mrd.ad.business.ad.dto.AdOrderCreateRequest;
import com.mrd.ad.business.ad.dto.AdOrderQuery;
import com.mrd.ad.business.ad.dto.AdOrderStatusRequest;
import com.mrd.ad.business.ad.dto.AdOrderUpdateRequest;
import com.mrd.ad.business.ad.service.AdOrderService;
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
@RequestMapping("/api/ads")
public class AdOrderController {

    private final AdOrderService adOrderService;

    public AdOrderController(AdOrderService adOrderService) {
        this.adOrderService = adOrderService;
    }

    @OperLog(module = "广告管理", businessType = "QUERY")
    @GetMapping
    public ApiResult<PageResult<AdOrder>> page(AdOrderQuery query) {
        return ApiResult.success(adOrderService.page(query));
    }

    @GetMapping("/{id}")
    public ApiResult<AdOrder> detail(@PathVariable Long id) {
        return ApiResult.success(adOrderService.getDetail(id));
    }

    @OperLog(module = "广告管理", businessType = "CREATE")
    @RequiresPermission("ad:edit")
    @PostMapping
    public ApiResult<AdOrder> create(@Validated @RequestBody AdOrderCreateRequest request) {
        return ApiResult.success(adOrderService.create(request));
    }

    @OperLog(module = "广告管理", businessType = "UPDATE")
    @RequiresPermission("ad:edit")
    @PutMapping("/{id}")
    public ApiResult<AdOrder> update(@PathVariable Long id, @Validated @RequestBody AdOrderUpdateRequest request) {
        return ApiResult.success(adOrderService.update(id, request));
    }

    @OperLog(module = "广告管理", businessType = "SUBMIT")
    @RequiresPermission("ad:submit")
    @PostMapping("/{id}/submit")
    public ApiResult<AdOrder> submit(@PathVariable Long id) {
        return ApiResult.success(adOrderService.submit(id));
    }

    @OperLog(module = "广告管理", businessType = "APPROVE")
    @RequiresPermission("ad:audit")
    @PostMapping("/{id}/approve")
    public ApiResult<AdOrder> approve(@PathVariable Long id, @RequestBody(required = false) AdOrderStatusRequest request) {
        return ApiResult.success(adOrderService.approve(id, request == null ? null : request.getComment()));
    }

    @OperLog(module = "广告管理", businessType = "REJECT")
    @RequiresPermission("ad:audit")
    @PostMapping("/{id}/reject")
    public ApiResult<AdOrder> reject(@PathVariable Long id, @RequestBody AdOrderStatusRequest request) {
        return ApiResult.success(adOrderService.reject(id, request.getComment()));
    }
}
