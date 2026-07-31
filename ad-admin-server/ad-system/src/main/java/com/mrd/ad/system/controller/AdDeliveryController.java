package com.mrd.ad.system.controller;

import com.mrd.ad.business.delivery.domain.AdDeliveryRecord;
import com.mrd.ad.business.delivery.dto.AdDeliveryQuery;
import com.mrd.ad.business.delivery.dto.AdDeliveryStatusRequest;
import com.mrd.ad.business.delivery.service.AdDeliveryService;
import com.mrd.ad.common.annotation.RequiresPermission;
import com.mrd.ad.common.core.ApiResult;
import com.mrd.ad.common.core.PageResult;
import com.mrd.ad.logging.annotation.OperLog;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/deliveries")
public class AdDeliveryController {

    private final AdDeliveryService adDeliveryService;

    public AdDeliveryController(AdDeliveryService adDeliveryService) {
        this.adDeliveryService = adDeliveryService;
    }

    @OperLog(module = "下发记录", businessType = "QUERY")
    @GetMapping
    public ApiResult<PageResult<AdDeliveryRecord>> page(AdDeliveryQuery query) {
        return ApiResult.success(adDeliveryService.page(query));
    }

    @GetMapping("/{id}")
    public ApiResult<AdDeliveryRecord> detail(@PathVariable Long id) {
        return ApiResult.success(adDeliveryService.getDetail(id));
    }

    @RequiresPermission("delivery:operate")
    @PostMapping("/{id}/success")
    public ApiResult<AdDeliveryRecord> success(@PathVariable Long id, @RequestBody(required = false) AdDeliveryStatusRequest request) {
        return ApiResult.success(adDeliveryService.markSuccess(id, request == null ? null : request.getResponseMsg()));
    }

    @RequiresPermission("delivery:operate")
    @PostMapping("/{id}/fail")
    public ApiResult<AdDeliveryRecord> fail(@PathVariable Long id, @RequestBody(required = false) AdDeliveryStatusRequest request) {
        return ApiResult.success(adDeliveryService.markFailed(id, request == null ? null : request.getResponseMsg()));
    }

    @RequiresPermission("delivery:operate")
    @PostMapping("/{id}/retry")
    public ApiResult<AdDeliveryRecord> retry(@PathVariable Long id) {
        return ApiResult.success(adDeliveryService.retry(id));
    }
}
