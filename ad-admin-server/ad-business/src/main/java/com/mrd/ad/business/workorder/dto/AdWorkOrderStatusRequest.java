package com.mrd.ad.business.workorder.dto;

public class AdWorkOrderStatusRequest {

    private Long assigneeId;
    private String content;

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
