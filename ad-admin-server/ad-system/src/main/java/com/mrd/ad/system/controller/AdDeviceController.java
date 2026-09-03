package com.mrd.ad.system.controller;

import com.mrd.ad.business.device.domain.AdDevice;
import com.mrd.ad.business.device.dto.AdDeviceCreateRequest;
import com.mrd.ad.business.device.dto.AdDeviceQuery;
import com.mrd.ad.business.device.dto.AdDeviceUpdateRequest;
import com.mrd.ad.business.device.dto.ViitalkDeviceCommandAckRequest;
import com.mrd.ad.business.device.dto.ViitalkDeviceCommandAckResult;
import com.mrd.ad.business.device.dto.ViitalkDeviceCommandRequest;
import com.mrd.ad.business.device.dto.ViitalkDeviceCommandResult;
import com.mrd.ad.business.device.dto.ViitalkDeviceOnlineStatus;
import com.mrd.ad.business.device.service.AdDeviceService;
import com.mrd.ad.business.device.service.ViitalkDeviceOnlineService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/devices")
public class AdDeviceController {

    private final AdDeviceService adDeviceService;
    private final ViitalkDeviceOnlineService viitalkDeviceOnlineService;

    public AdDeviceController(AdDeviceService adDeviceService,
                              ViitalkDeviceOnlineService viitalkDeviceOnlineService) {
        this.adDeviceService = adDeviceService;
        this.viitalkDeviceOnlineService = viitalkDeviceOnlineService;
    }

    @OperLog(module = "设备管理", businessType = "QUERY")
    @GetMapping
    public ApiResult<PageResult<AdDevice>> page(AdDeviceQuery query) {
        return ApiResult.success(adDeviceService.page(query));
    }

    @GetMapping("/viitalk/online")
    public ApiResult<ViitalkDeviceOnlineStatus> viitalkOnline(@RequestParam String mzNumber) {
        return ApiResult.success(viitalkDeviceOnlineService.queryOnlineStatus(mzNumber));
    }

    @OperLog(module = "设备管理", businessType = "SEND")
    @RequiresPermission("device:manage")
    @PostMapping("/viitalk/command")
    public ApiResult<ViitalkDeviceCommandResult> sendViitalkCommand(@Validated @RequestBody ViitalkDeviceCommandRequest request) {
        return ApiResult.success(viitalkDeviceOnlineService.sendCommand(request));
    }

    @PostMapping("/viitalk/command/ack")
    public ApiResult<ViitalkDeviceCommandAckResult> receiveViitalkCommandAck(@RequestBody ViitalkDeviceCommandAckRequest request) {
        return ApiResult.success(viitalkDeviceOnlineService.receiveCommandAck(request));
    }

    @GetMapping("/viitalk/command/ack")
    public ApiResult<ViitalkDeviceCommandAckResult> queryViitalkCommandAck(@RequestParam String requestId) {
        return ApiResult.success(viitalkDeviceOnlineService.queryCommandAck(requestId));
    }

    @GetMapping("/{id}")
    public ApiResult<AdDevice> detail(@PathVariable Long id) {
        return ApiResult.success(adDeviceService.getDetail(id));
    }

    @OperLog(module = "设备管理", businessType = "CREATE")
    @RequiresPermission("device:manage")
    @PostMapping
    public ApiResult<AdDevice> create(@Validated @RequestBody AdDeviceCreateRequest request) {
        return ApiResult.success(adDeviceService.create(request));
    }

    @OperLog(module = "设备管理", businessType = "UPDATE")
    @RequiresPermission("device:manage")
    @PutMapping("/{id}")
    public ApiResult<AdDevice> update(@PathVariable Long id, @Validated @RequestBody AdDeviceUpdateRequest request) {
        return ApiResult.success(adDeviceService.update(id, request));
    }

    @RequiresPermission("device:manage")
    @PostMapping("/{id}/online")
    public ApiResult<AdDevice> online(@PathVariable Long id) {
        return ApiResult.success(adDeviceService.setOnline(id));
    }

    @RequiresPermission("device:manage")
    @PostMapping("/{id}/offline")
    public ApiResult<AdDevice> offline(@PathVariable Long id) {
        return ApiResult.success(adDeviceService.setOffline(id));
    }

    @RequiresPermission("device:manage")
    @PostMapping("/{id}/fault")
    public ApiResult<AdDevice> fault(@PathVariable Long id) {
        return ApiResult.success(adDeviceService.markFault(id));
    }

    @RequiresPermission("device:manage")
    @PostMapping("/{id}/repair")
    public ApiResult<AdDevice> repair(@PathVariable Long id) {
        return ApiResult.success(adDeviceService.repair(id));
    }

    @RequiresPermission("device:manage")
    @PostMapping("/{id}/enable")
    public ApiResult<AdDevice> enable(@PathVariable Long id) {
        return ApiResult.success(adDeviceService.enable(id));
    }

    @RequiresPermission("device:manage")
    @PostMapping("/{id}/disable")
    public ApiResult<AdDevice> disable(@PathVariable Long id) {
        return ApiResult.success(adDeviceService.disable(id));
    }
}
