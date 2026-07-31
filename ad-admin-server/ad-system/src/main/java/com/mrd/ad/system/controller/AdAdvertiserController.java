package com.mrd.ad.system.controller;

import com.mrd.ad.business.partner.domain.AdAdvertiser;
import com.mrd.ad.business.partner.dto.AdvertiserCreateRequest;
import com.mrd.ad.business.partner.dto.AdvertiserQuery;
import com.mrd.ad.business.partner.dto.AdvertiserUpdateRequest;
import com.mrd.ad.business.partner.service.AdAdvertiserService;
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
@RequestMapping("/api/advertisers")
public class AdAdvertiserController {

    private final AdAdvertiserService adAdvertiserService;

    public AdAdvertiserController(AdAdvertiserService adAdvertiserService) {
        this.adAdvertiserService = adAdvertiserService;
    }

    @OperLog(module = "广告主", businessType = "QUERY")
    @GetMapping
    public ApiResult<PageResult<AdAdvertiser>> page(AdvertiserQuery query) {
        return ApiResult.success(adAdvertiserService.page(query));
    }

    @GetMapping("/{id}")
    public ApiResult<AdAdvertiser> detail(@PathVariable Long id) {
        return ApiResult.success(adAdvertiserService.getDetail(id));
    }

    @OperLog(module = "广告主", businessType = "CREATE")
    @RequiresPermission("partner:manage")
    @PostMapping
    public ApiResult<AdAdvertiser> create(@Validated @RequestBody AdvertiserCreateRequest request) {
        return ApiResult.success(adAdvertiserService.create(request));
    }

    @OperLog(module = "广告主", businessType = "UPDATE")
    @RequiresPermission("partner:manage")
    @PutMapping("/{id}")
    public ApiResult<AdAdvertiser> update(@PathVariable Long id, @Validated @RequestBody AdvertiserUpdateRequest request) {
        return ApiResult.success(adAdvertiserService.update(id, request));
    }

    @RequiresPermission("partner:manage")
    @PostMapping("/{id}/enable")
    public ApiResult<AdAdvertiser> enable(@PathVariable Long id) {
        return ApiResult.success(adAdvertiserService.enable(id));
    }

    @RequiresPermission("partner:manage")
    @PostMapping("/{id}/disable")
    public ApiResult<AdAdvertiser> disable(@PathVariable Long id) {
        return ApiResult.success(adAdvertiserService.disable(id));
    }
}
