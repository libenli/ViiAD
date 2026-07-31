package com.mrd.ad.business.plan.domain;

import com.baomidou.mybatisplus.annotation.TableName;

@TableName("ad_plan_material")
public class AdPlanMaterial {

    private Long id;
    private Long planId;
    private Long materialId;

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

    public Long getMaterialId() {
        return materialId;
    }

    public void setMaterialId(Long materialId) {
        this.materialId = materialId;
    }
}

