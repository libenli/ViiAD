package com.mrd.ad.business.bill.dto;

import javax.validation.constraints.NotBlank;

public class AdBillGenerateRequest {

    @NotBlank(message = "账单月份不能为空")
    private String billMonth;

    public String getBillMonth() {
        return billMonth;
    }

    public void setBillMonth(String billMonth) {
        this.billMonth = billMonth;
    }
}
