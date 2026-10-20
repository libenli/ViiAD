package com.mrd.ad.business.wallet.dto;

public class AuditRefundOrderRequest {

    private String auditRemark;
    private String refundTransactionNo;

    public String getAuditRemark() {
        return auditRemark;
    }

    public void setAuditRemark(String auditRemark) {
        this.auditRemark = auditRemark;
    }

    public String getRefundTransactionNo() {
        return refundTransactionNo;
    }

    public void setRefundTransactionNo(String refundTransactionNo) {
        this.refundTransactionNo = refundTransactionNo;
    }
}