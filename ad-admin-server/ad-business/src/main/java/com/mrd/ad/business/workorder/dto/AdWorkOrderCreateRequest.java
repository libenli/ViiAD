package com.mrd.ad.business.workorder.dto;

import javax.validation.constraints.NotBlank;

public class AdWorkOrderCreateRequest {

    @NotBlank(message = "工单标题不能为空")
    private String title;

    private String content;
    private String priority;
    private Long assigneeId;
    private Long relatedAdId;
    private Long relatedPlanId;

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
}
