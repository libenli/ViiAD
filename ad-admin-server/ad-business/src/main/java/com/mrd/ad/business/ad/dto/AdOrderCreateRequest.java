package com.mrd.ad.business.ad.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.math.BigDecimal;

public class AdOrderCreateRequest {

    @NotBlank(message = "广告名称不能为空")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5A-Za-z0-9\\s\\-_（）()，。,.、：:；;！!？?]+$", message = "广告名称不能包含特殊字符")
    private String adName;

    @NotNull(message = "广告主不能为空")
    private Long advertiserId;

    private Long agentId;

    @NotBlank(message = "广告类型不能为空")
    private String adType;

    private String objective;

    @NotBlank(message = "投放区域不能为空")
    private String regionCode;
    private BigDecimal budgetAmount;

    @Pattern(regexp = "^$|^[\\u4e00-\\u9fa5A-Za-z0-9\\s\\-_（）()，。,.、：:；;！!？?]+$", message = "广告说明不能包含特殊字符")
    private String description;

    public String getAdName() {
        return adName;
    }

    public void setAdName(String adName) {
        this.adName = adName;
    }

    public Long getAdvertiserId() {
        return advertiserId;
    }

    public void setAdvertiserId(Long advertiserId) {
        this.advertiserId = advertiserId;
    }

    public Long getAgentId() {
        return agentId;
    }

    public void setAgentId(Long agentId) {
        this.agentId = agentId;
    }

    public String getAdType() {
        return adType;
    }

    public void setAdType(String adType) {
        this.adType = adType;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
