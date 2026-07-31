package com.mrd.ad.system.controller;

import com.mrd.ad.business.plan.domain.AdPlan;
import com.mrd.ad.business.plan.dto.AdPlanCreateRequest;
import com.mrd.ad.business.plan.dto.AdPlanQuery;
import com.mrd.ad.business.plan.dto.AdPlanUpdateRequest;
import com.mrd.ad.business.plan.service.AdPlanService;
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
@RequestMapping("/api/plans")
public class AdPlanController {

    private final AdPlanService adPlanService;

    public AdPlanController(AdPlanService adPlanService) {
        this.adPlanService = adPlanService;
    }

    @OperLog(module = "投放计划", businessType = "QUERY")
    @GetMapping
    public ApiResult<PageResult<AdPlan>> page(AdPlanQuery query) {
        return ApiResult.success(adPlanService.page(query));
    }

    @GetMapping("/{id}")
    public ApiResult<AdPlan> detail(@PathVariable Long id) {
        return ApiResult.success(adPlanService.getDetail(id));
    }

    @OperLog(module = "投放计划", businessType = "CREATE")
    @RequiresPermission("plan:edit")
    @PostMapping
    public ApiResult<AdPlan> create(@Validated @RequestBody AdPlanCreateRequest request) {
        return ApiResult.success(adPlanService.create(request));
    }

    @OperLog(module = "投放计划", businessType = "UPDATE")
    @RequiresPermission("plan:edit")
    @PutMapping("/{id}")
    public ApiResult<AdPlan> update(@PathVariable Long id, @Validated @RequestBody AdPlanUpdateRequest request) {
        return ApiResult.success(adPlanService.update(id, request));
    }

    @OperLog(module = "投放计划", businessType = "SCHEDULE")
    @RequiresPermission("plan:schedule")
    @PostMapping("/{id}/schedule")
    public ApiResult<AdPlan> schedule(@PathVariable Long id) {
        return ApiResult.success(adPlanService.schedule(id));
    }

    @OperLog(module = "投放计划", businessType = "START")
    @RequiresPermission("plan:delivery")
    @PostMapping("/{id}/start")
    public ApiResult<AdPlan> start(@PathVariable Long id) {
        return ApiResult.success(adPlanService.start(id));
    }

    @OperLog(module = "投放计划", businessType = "PAUSE")
    @RequiresPermission("plan:delivery")
    @PostMapping("/{id}/pause")
    public ApiResult<AdPlan> pause(@PathVariable Long id) {
        return ApiResult.success(adPlanService.pause(id));
    }

    @OperLog(module = "投放计划", businessType = "FINISH")
    @RequiresPermission("plan:delivery")
    @PostMapping("/{id}/finish")
    public ApiResult<AdPlan> finish(@PathVariable Long id) {
        return ApiResult.success(adPlanService.finish(id));
    }
}
