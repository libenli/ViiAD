package com.mrd.ad.business.workorder.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mrd.ad.common.domain.BaseEntity;

import java.util.Date;

@TableName("ad_work_order")
public class AdWorkOrder extends BaseEntity {

    private String workNo;
    private String sourceType;
    private String title;
    private String content;
    private String priority;
    private String status;
    private Long creatorId;
    private Long assigneeId;
    private Long relatedAdId;
    private Long relatedPlanId;
    private Date closeTime;

    public String getWorkNo() {
        return workNo;
    }

    public void setWorkNo(String workNo) {
        this.workNo = workNo;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Long getRelatedAdId() {
        return relatedAdId;
    }

    public void setRelatedAdId(Long relatedAdId) {
        this.relatedAdId = relatedAdId;
    }

    public Long getRelatedPlanId() {
        return relatedPlanId;
    }

    public void setRelatedPlanId(Long relatedPlanId) {
        this.relatedPlanId = relatedPlanId;
    }

    public Date getCloseTime() {
        return closeTime;
    }

    public void setCloseTime(Date closeTime) {
        this.closeTime = closeTime;
    }
}
