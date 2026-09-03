package com.mrd.ad.business.system.dto;

public class ForgotPasswordResetRequest {

    private String resetToken;
    private String password;

    public String getResetToken() {
        return resetToken;
    }

    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
