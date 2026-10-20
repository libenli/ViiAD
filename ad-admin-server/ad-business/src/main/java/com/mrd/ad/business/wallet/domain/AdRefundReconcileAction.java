package com.mrd.ad.business.wallet.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mrd.ad.common.domain.BaseEntity;

import java.math.BigDecimal;

@TableName("ad_refund_reconcile_action")
public class AdRefundReconcileAction extends BaseEntity {

    private Long refundOrderId;
    private String refundNo;
    private String actionType;
    private String reason;
    private Long operatorUserId;
    private String operatorName;
    private String gatewayRefundStatus;
    private String refundStatusBefore;
    private String refundStatusAfter;
    private String reconcileStatusBefore;
    private String reconcileStatusAfter;
    private BigDecimal availableBalanceBefore;
    private BigDecimal availableBalanceAfter;
    private BigDecimal frozenBalanceBefore;
    private BigDecimal frozenBalanceAfter;
    private BigDecimal reservedAmountBefore;
    private BigDecimal reservedAmountAfter;
    private String resultStatus;
    private String resultMessage;

    public Long getRefundOrderId() { return refundOrderId; }
    public void setRefundOrderId(Long refundOrderId) { this.refundOrderId = refundOrderId; }
    public String getRefundNo() { return refundNo; }
    public void setRefundNo(String refundNo) { this.refundNo = refundNo; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Long getOperatorUserId() { return operatorUserId; }
    public void setOperatorUserId(Long operatorUserId) { this.operatorUserId = operatorUserId; }
    public String getOperatorName() { return operatorName; }
    public void setOperatorName(String operatorName) { this.operatorName = operatorName; }
    public String getGatewayRefundStatus() { return gatewayRefundStatus; }
    public void setGatewayRefundStatus(String gatewayRefundStatus) { this.gatewayRefundStatus = gatewayRefundStatus; }
    public String getRefundStatusBefore() { return refundStatusBefore; }
    public void setRefundStatusBefore(String refundStatusBefore) { this.refundStatusBefore = refundStatusBefore; }
    public String getRefundStatusAfter() { return refundStatusAfter; }
    public void setRefundStatusAfter(String refundStatusAfter) { this.refundStatusAfter = refundStatusAfter; }
    public String getReconcileStatusBefore() { return reconcileStatusBefore; }
    public void setReconcileStatusBefore(String reconcileStatusBefore) { this.reconcileStatusBefore = reconcileStatusBefore; }
    public String getReconcileStatusAfter() { return reconcileStatusAfter; }
    public void setReconcileStatusAfter(String reconcileStatusAfter) { this.reconcileStatusAfter = reconcileStatusAfter; }
    public BigDecimal getAvailableBalanceBefore() { return availableBalanceBefore; }
    public void setAvailableBalanceBefore(BigDecimal availableBalanceBefore) { this.availableBalanceBefore = availableBalanceBefore; }
    public BigDecimal getAvailableBalanceAfter() { return availableBalanceAfter; }
    public void setAvailableBalanceAfter(BigDecimal availableBalanceAfter) { this.availableBalanceAfter = availableBalanceAfter; }
    public BigDecimal getFrozenBalanceBefore() { return frozenBalanceBefore; }
    public void setFrozenBalanceBefore(BigDecimal frozenBalanceBefore) { this.frozenBalanceBefore = frozenBalanceBefore; }
    public BigDecimal getFrozenBalanceAfter() { return frozenBalanceAfter; }
    public void setFrozenBalanceAfter(BigDecimal frozenBalanceAfter) { this.frozenBalanceAfter = frozenBalanceAfter; }
    public BigDecimal getReservedAmountBefore() { return reservedAmountBefore; }
    public void setReservedAmountBefore(BigDecimal reservedAmountBefore) { this.reservedAmountBefore = reservedAmountBefore; }
    public BigDecimal getReservedAmountAfter() { return reservedAmountAfter; }
    public void setReservedAmountAfter(BigDecimal reservedAmountAfter) { this.reservedAmountAfter = reservedAmountAfter; }
    public String getResultStatus() { return resultStatus; }
    public void setResultStatus(String resultStatus) { this.resultStatus = resultStatus; }
    public String getResultMessage() { return resultMessage; }
    public void setResultMessage(String resultMessage) { this.resultMessage = resultMessage; }
}