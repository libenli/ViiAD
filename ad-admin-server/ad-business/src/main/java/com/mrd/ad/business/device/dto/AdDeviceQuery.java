package com.mrd.ad.business.device.dto;

public class AdDeviceQuery {

    private String keyword;
    private Long buildingId;
    private String onlineStatus;
    private String faultStatus;
    private String status;
    private Long currentPlanId;
    private Long page = 1L;
    private Long size = 10L;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public String getOnlineStatus() {
        return onlineStatus;
    }

    public void setOnlineStatus(String onlineStatus) {
        this.onlineStatus = onlineStatus;
    }

    public String getFaultStatus() {
        return faultStatus;
    }

    public void setFaultStatus(String faultStatus) {
        this.faultStatus = faultStatus;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCurrentPlanId() {
        return currentPlanId;
    }

    public void setCurrentPlanId(Long currentPlanId) {
        this.currentPlanId = currentPlanId;
    }

    public Long getPage() {
        return page;
    }

    public void setPage(Long page) {
        this.page = page;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
