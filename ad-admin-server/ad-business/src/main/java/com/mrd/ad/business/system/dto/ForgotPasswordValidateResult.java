package com.mrd.ad.business.system.dto;

public class ForgotPasswordValidateResult {

    private String resetToken;

    public ForgotPasswordValidateResult() {
    }

    public ForgotPasswordValidateResult(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }
}
