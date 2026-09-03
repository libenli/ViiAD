package com.mrd.ad.system.controller;

import com.mrd.ad.business.workorder.domain.AdWorkOrder;
import com.mrd.ad.business.workorder.dto.AdWorkOrderCreateRequest;
import com.mrd.ad.business.workorder.dto.AdWorkOrderQuery;
import com.mrd.ad.business.workorder.dto.AdWorkOrderStatusRequest;
import com.mrd.ad.business.workorder.service.AdWorkOrderService;
import com.mrd.ad.common.annotation.RequiresPermission;
import com.mrd.ad.common.core.ApiResult;
import com.mrd.ad.common.core.PageResult;
import com.mrd.ad.logging.annotation.OperLog;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workorders")
public class AdWorkOrderController {

    private final AdWorkOrderService adWorkOrderService;

    public AdWorkOrderController(AdWorkOrderService adWorkOrderService) {
        this.adWorkOrderService = adWorkOrderService;
    }

    @OperLog(module = "工单反馈", businessType = "QUERY")
    @GetMapping
    public ApiResult<PageResult<AdWorkOrder>> page(AdWorkOrderQuery query) {
        return ApiResult.success(adWorkOrderService.page(query));
    }

    @GetMapping("/{id}")
    public ApiResult<AdWorkOrder> detail(@PathVariable Long id) {
        return ApiResult.success(adWorkOrderService.getDetail(id));
    }

    @OperLog(module = "工单反馈", businessType = "CREATE")
    @RequiresPermission("workOrder:operate")
    @PostMapping
    public ApiResult<AdWorkOrder> create(@Validated @RequestBody AdWorkOrderCreateRequest request) {
        return ApiResult.success(adWorkOrderService.create(request));
    }

    @RequiresPermission("workOrder:operate")
    @PostMapping("/{id}/assign")
    public ApiResult<AdWorkOrder> assign(@PathVariable Long id, @RequestBody AdWorkOrderStatusRequest request) {
        return ApiResult.success(adWorkOrderService.assign(id, request.getAssigneeId()));
    }

    @RequiresPermission("workOrder:operate")
    @PostMapping("/{id}/start")
    public ApiResult<AdWorkOrder> start(@PathVariable Long id) {
        return ApiResult.success(adWorkOrderService.start(id));
    }

    @RequiresPermission("workOrder:operate")
    @PostMapping("/{id}/close")
    public ApiResult<AdWorkOrder> close(@PathVariable Long id, @RequestBody(required = false) AdWorkOrderStatusRequest request) {
        return ApiResult.success(adWorkOrderService.close(id, request == null ? null : request.getContent()));
    }

    @RequiresPermission("workOrder:operate")
    @PostMapping("/{id}/reopen")
    public ApiResult<AdWorkOrder> reopen(@PathVariable Long id) {
        return ApiResult.success(adWorkOrderService.reopen(id));
    }
}
