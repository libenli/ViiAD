package com.mrd.ad.business.report.dto;

public class ReportOverview {

    private Integer playCount;
    private Integer exposureCount;
    private Integer activeDeviceCount;
    private Integer playDuration;
    private Integer logCount;
    private String completionRate;

    public Integer getPlayCount() {
        return playCount;
    }

    public void setPlayCount(Integer playCount) {
        this.playCount = playCount;
    }

    public Integer getExposureCount() {
        return exposureCount;
    }

    public void setExposureCount(Integer exposureCount) {
        this.exposureCount = exposureCount;
    }

    public Integer getActiveDeviceCount() {
        return activeDeviceCount;
    }

    public void setActiveDeviceCount(Integer activeDeviceCount) {
        this.activeDeviceCount = activeDeviceCount;
    }

    public Integer getPlayDuration() {
        return playDuration;
    }

    public void setPlayDuration(Integer playDuration) {
        this.playDuration = playDuration;
    }

    public Integer getLogCount() {
        return logCount;
    }

    public void setLogCount(Integer logCount) {
        this.logCount = logCount;
    }

    public String getCompletionRate() {
        return completionRate;
    }

    public void setCompletionRate(String completionRate) {
        this.completionRate = completionRate;
    }
}
