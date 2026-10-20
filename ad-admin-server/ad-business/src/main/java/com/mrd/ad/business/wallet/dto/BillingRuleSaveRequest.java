package com.mrd.ad.business.wallet.dto;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

public class BillingRuleSaveRequest {

    @NotBlank(message = "规则名称不能为空")
    private String ruleName;

    @NotBlank(message = "素材类型不能为空")
    private String materialType;

    @NotBlank(message = "计费方式不能为空")
    private String chargeMode;

    @NotNull(message = "单价不能为空")
    @DecimalMin(value = "0.0001", message = "单价必须大于0")
    private BigDecimal unitPrice;

    private Integer priority;
    private String status;
    private String remark;

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }

    public String getChargeMode() {
        return chargeMode;
    }

    public void setChargeMode(String chargeMode) {
        this.chargeMode = chargeMode;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
