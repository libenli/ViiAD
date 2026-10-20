package com.mrd.ad.business.wallet.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class RefundReconcileActionRequest {

    @NotBlank(message = "处理类型不能为空")
    private String actionType;

    @NotBlank(message = "处理原因不能为空")
    @Size(max = 500, message = "处理原因不能超过500个字符")
    private String reason;

    @Size(max = 128, message = "退款流水号不能超过128个字符")
    private String refundTransactionNo;

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRefundTransactionNo() { return refundTransactionNo; }
    public void setRefundTransactionNo(String refundTransactionNo) { this.refundTransactionNo = refundTransactionNo; }
}