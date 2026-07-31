package com.mrd.ad.business.material.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class AdMaterialCreateRequest {

    @NotNull(message = "所属广告不能为空")
    private Long adId;

    @NotBlank(message = "素材名称不能为空")
    private String materialName;

    @NotBlank(message = "素材类型不能为空")
    private String materialType;

    @NotBlank(message = "文件地址不能为空")
    private String fileUrl;

    private Long fileSize;
    private Integer durationSeconds;
    private Integer width;
    private Integer height;
    private String coverUrl;

    public Long getAdId() {
        return adId;
    }

    public void setAdId(Long adId) {
        this.adId = adId;
    }

    public String getMaterialName() {
        return materialName;
    }

    public void setMaterialName(String materialName) {
        this.materialName = materialName;
    }

    public String getMaterialType() {
        return materialType;
    }

    public void setMaterialType(String materialType) {
        this.materialType = materialType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }
}

