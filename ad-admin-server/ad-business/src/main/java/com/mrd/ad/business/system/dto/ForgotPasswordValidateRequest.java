package com.mrd.ad.business.system.dto;

public class ForgotPasswordValidateRequest extends ForgotPasswordCodeRequest {

    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
