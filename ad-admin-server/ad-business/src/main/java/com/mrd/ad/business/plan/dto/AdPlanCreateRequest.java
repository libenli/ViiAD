package com.mrd.ad.business.plan.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

public class AdPlanCreateRequest {

    @NotBlank(message = "计划名称不能为空")
    private String planName;

    @NotNull(message = "广告不能为空")
    private Long adId;

    @NotEmpty(message = "素材不能为空")
    private List<Long> materialIds;

    @NotEmpty(message = "设备不能为空")
    private List<Long> deviceIds;

    private String regionCode;

    @NotBlank(message = "开始时间不能为空")
    private String startTime;

    @NotBlank(message = "结束时间不能为空")
    private String endTime;

    private Long operatorId;

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public Long getAdId() {
        return adId;
    }

    public void setAdId(Long adId) {
        this.adId = adId;
    }

    public List<Long> getMaterialIds() {
        return materialIds;
    }

    public void setMaterialIds(List<Long> materialIds) {
        this.materialIds = materialIds;
    }

    public List<Long> getDeviceIds() {
        return deviceIds;
    }

    public void setDeviceIds(List<Long> deviceIds) {
        this.deviceIds = deviceIds;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }
}

