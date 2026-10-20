package com.mrd.ad.business.wallet.dto;

import com.mrd.ad.business.wallet.domain.AdRechargeOrder;

public class RechargePaymentResult {

    private AdRechargeOrder order;
    private String payMethod;
    private String qrCodeUrl;
    private String prepayId;
    private String message;

    public AdRechargeOrder getOrder() {
        return order;
    }

    public void setOrder(AdRechargeOrder order) {
        this.order = order;
    }

    public String getPayMethod() {
        return payMethod;
    }

    public void setPayMethod(String payMethod) {
        this.payMethod = payMethod;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public String getPrepayId() {
        return prepayId;
    }

    public void setPrepayId(String prepayId) {
        this.prepayId = prepayId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
