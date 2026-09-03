package com.mrd.ad.business.bill.dto;

import javax.validation.constraints.NotBlank;

public class AdBillPayRequest {

    @NotBlank(message = "银行汇款单号不能为空")
    private String paymentVoucherNo;

    @NotBlank(message = "付款截图不能为空")
    private String paymentVoucherUrl;

    public String getPaymentVoucherNo() {
        return paymentVoucherNo;
    }

    public void setPaymentVoucherNo(String paymentVoucherNo) {
        this.paymentVoucherNo = paymentVoucherNo;
    }

    public String getPaymentVoucherUrl() {
        return paymentVoucherUrl;
    }

    public void setPaymentVoucherUrl(String paymentVoucherUrl) {
        this.paymentVoucherUrl = paymentVoucherUrl;
    }
}
