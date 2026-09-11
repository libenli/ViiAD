package com.mrd.ad.business.report.domain;

import com.baomidou.mybatisplus.annotation.TableName;

import java.sql.Date;

@TableName("ad_play_log")
public class AdPlayLog {

    private Long id;
    private Long adId;
    private Long planId;
    private Long materialId;
    private Long deviceId;
    private Date playDate;
    private Integer playCount;
    private Integer playDuration;
    private String sourceType;
    private java.util.Date createTime;
    private String playStatus;
    private java.util.Date playStartTime;
    private java.util.Date playEndTime;
    private String requestId;
    private String errorMessage;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAdId() {
        return adId;
    }

    public void setAdId(Long adId) {
        this.adId = adId;
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

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public Date getPlayDate() {
        return playDate;
    }

    public void setPlayDate(Date playDate) {
        this.playDate = playDate;
    }

    public Integer getPlayCount() {
        return playCount;
    }

    public void setPlayCount(Integer playCount) {
        this.playCount = playCount;
    }

    public Integer getPlayDuration() {
        return playDuration;
    }

    public void setPlayDuration(Integer playDuration) {
        this.playDuration = playDuration;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public java.util.Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(java.util.Date createTime) {
        this.createTime = createTime;
    }

    public String getPlayStatus() {
        return playStatus;
    }

    public void setPlayStatus(String playStatus) {
        this.playStatus = playStatus;
    }

    public java.util.Date getPlayStartTime() {
        return playStartTime;
    }

    public void setPlayStartTime(java.util.Date playStartTime) {
        this.playStartTime = playStartTime;
    }

    public java.util.Date getPlayEndTime() {
        return playEndTime;
    }

    public void setPlayEndTime(java.util.Date playEndTime) {
        this.playEndTime = playEndTime;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
