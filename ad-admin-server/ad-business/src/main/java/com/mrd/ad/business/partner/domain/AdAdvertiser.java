package com.mrd.ad.business.partner.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mrd.ad.common.domain.BaseEntity;

@TableName("ad_advertiser")
public class AdAdvertiser extends BaseEntity {

    private String advertiserCode;
    private String advertiserName;
    private String companyName;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private Long ownerUserId;
    private String status;
    private String sourceType;
    private String remark;

    public String getAdvertiserCode() {
        return advertiserCode;
    }

    public void setAdvertiserCode(String advertiserCode) {
        this.advertiserCode = advertiserCode;
    }

    public String getAdvertiserName() {
        return advertiserName;
    }

    public void setAdvertiserName(String advertiserName) {
        this.advertiserName = advertiserName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
