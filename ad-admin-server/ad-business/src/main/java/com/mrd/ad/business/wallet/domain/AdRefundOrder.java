package com.mrd.ad.business.wallet.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mrd.ad.common.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

@TableName("ad_refund_order")
public class AdRefundOrder extends BaseEntity {

    private String refundNo;
    private Long rechargeOrderId;
    private String rechargeNo;
    private Long accountId;
    private String ownerType;
    private Long ownerId;
    private String payMethod;
    private BigDecimal refundAmount;
    private String status;
    private String refundReason;
    private Long auditUserId;
    private Date auditTime;
    private String auditRemark;
    private String transactionNo;
    private String refundTransactionNo;
    private Date refundTime;
    private String failReason;
    private BigDecimal reservedAmount;
    private String gatewayRefundStatus;
    private String reconcileStatus;
    private String reconcileMessage;
    private Date lastSyncTime;
    private Integer syncRetryCount;

    public String getRefundNo() {
        return refundNo;
    }

    public void setRefundNo(String refundNo) {
        this.refundNo = refundNo;
    }

    public Long getRechargeOrderId() {
        return rechargeOrderId;
    }

    public void setRechargeOrderId(Long rechargeOrderId) {
        this.rechargeOrderId = rechargeOrderId;
    }

    public String getRechargeNo() {
        return rechargeNo;
    }

    public void setRechargeNo(String rechargeNo) {
        this.rechargeNo = rechargeNo;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getOwnerType() {
        return ownerType;
    }

    public void setOwnerType(String ownerType) {
        this.ownerType = ownerType;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public String getPayMethod() {
        return payMethod;
    }

    public void setPayMethod(String payMethod) {
        this.payMethod = payMethod;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRefundReason() {
        return refundReason;
    }

    public void setRefundReason(String refundReason) {
        this.refundReason = refundReason;
    }

    public Long getAuditUserId() {
        return auditUserId;
    }

    public void setAuditUserId(Long auditUserId) {
        this.auditUserId = auditUserId;
    }

    public Date getAuditTime() {
        return auditTime;
    }

    public void setAuditTime(Date auditTime) {
        this.auditTime = auditTime;
    }

    public String getAuditRemark() {
        return auditRemark;
    }

    public void setAuditRemark(String auditRemark) {
        this.auditRemark = auditRemark;
    }

    public String getTransactionNo() {
        return transactionNo;
    }

    public void setTransactionNo(String transactionNo) {
        this.transactionNo = transactionNo;
    }

    public String getRefundTransactionNo() {
        return refundTransactionNo;
    }

    public void setRefundTransactionNo(String refundTransactionNo) {
        this.refundTransactionNo = refundTransactionNo;
    }

    public Date getRefundTime() {
        return refundTime;
    }

    public void setRefundTime(Date refundTime) {
        this.refundTime = refundTime;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public BigDecimal getReservedAmount() {
        return reservedAmount;
    }

    public void setReservedAmount(BigDecimal reservedAmount) {
        this.reservedAmount = reservedAmount;
    }

    public String getGatewayRefundStatus() {
        return gatewayRefundStatus;
    }

    public void setGatewayRefundStatus(String gatewayRefundStatus) {
        this.gatewayRefundStatus = gatewayRefundStatus;
    }

    public String getReconcileStatus() {
        return reconcileStatus;
    }

    public void setReconcileStatus(String reconcileStatus) {
        this.reconcileStatus = reconcileStatus;
    }

    public String getReconcileMessage() {
        return reconcileMessage;
    }

    public void setReconcileMessage(String reconcileMessage) {
        this.reconcileMessage = reconcileMessage;
    }

    public Date getLastSyncTime() {
        return lastSyncTime;
    }

    public void setLastSyncTime(Date lastSyncTime) {
        this.lastSyncTime = lastSyncTime;
    }

    public Integer getSyncRetryCount() {
        return syncRetryCount;
    }

    public void setSyncRetryCount(Integer syncRetryCount) {
        this.syncRetryCount = syncRetryCount;
    }}