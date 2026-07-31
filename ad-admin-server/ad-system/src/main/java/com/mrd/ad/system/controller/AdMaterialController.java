package com.mrd.ad.system.controller;

import com.mrd.ad.business.material.domain.AdMaterial;
import com.mrd.ad.business.material.dto.AdMaterialCreateRequest;
import com.mrd.ad.business.material.dto.AdMaterialQuery;
import com.mrd.ad.business.material.dto.AdMaterialStatusRequest;
import com.mrd.ad.business.material.dto.AdMaterialUpdateRequest;
import com.mrd.ad.business.material.service.AdMaterialService;
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
@RequestMapping("/api/materials")
public class AdMaterialController {

    private final AdMaterialService adMaterialService;

    public AdMaterialController(AdMaterialService adMaterialService) {
        this.adMaterialService = adMaterialService;
    }

    @OperLog(module = "素材管理", businessType = "QUERY")
    @GetMapping
    public ApiResult<PageResult<AdMaterial>> page(AdMaterialQuery query) {
        return ApiResult.success(adMaterialService.page(query));
    }

    @GetMapping("/{id}")
    public ApiResult<AdMaterial> detail(@PathVariable Long id) {
        return ApiResult.success(adMaterialService.getDetail(id));
    }

    @OperLog(module = "素材管理", businessType = "CREATE")
    @RequiresPermission("material:edit")
    @PostMapping
    public ApiResult<AdMaterial> create(@Validated @RequestBody AdMaterialCreateRequest request) {
        return ApiResult.success(adMaterialService.create(request));
    }

    @OperLog(module = "素材管理", businessType = "UPDATE")
    @RequiresPermission("material:edit")
    @PutMapping("/{id}")
    public ApiResult<AdMaterial> update(@PathVariable Long id, @Validated @RequestBody AdMaterialUpdateRequest request) {
        return ApiResult.success(adMaterialService.update(id, request));
    }

    @OperLog(module = "素材管理", businessType = "SUBMIT")
    @RequiresPermission("material:submit")
    @PostMapping("/{id}/submit")
    public ApiResult<AdMaterial> submit(@PathVariable Long id) {
        return ApiResult.success(adMaterialService.submit(id));
    }

    @OperLog(module = "素材管理", businessType = "APPROVE")
    @RequiresPermission("material:audit")
    @PostMapping("/{id}/approve")
    public ApiResult<AdMaterial> approve(@PathVariable Long id, @RequestBody(required = false) AdMaterialStatusRequest request) {
        return ApiResult.success(adMaterialService.approve(id, request == null ? null : request.getComment()));
    }

    @OperLog(module = "素材管理", businessType = "REJECT")
    @RequiresPermission("material:audit")
    @PostMapping("/{id}/reject")
    public ApiResult<AdMaterial> reject(@PathVariable Long id, @RequestBody AdMaterialStatusRequest request) {
        return ApiResult.success(adMaterialService.reject(id, request.getComment()));
    }
}
