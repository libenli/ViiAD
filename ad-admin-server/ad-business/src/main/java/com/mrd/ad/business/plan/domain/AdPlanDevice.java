package com.mrd.ad.business.plan.domain;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("ad_plan_device")
public class AdPlanDevice {

    private Long id;
    private Long planId;
    private Long deviceId;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }
}

