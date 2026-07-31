package com.mrd.ad.business.system.dto;

import javax.validation.constraints.NotBlank;

public class ResetPasswordRequest {

    @NotBlank(message = "新密码不能为空")
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
