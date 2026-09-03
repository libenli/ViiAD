package com.mrd.ad.business.system.dto;

public class AuthSelectRoleRequest {

    private String tempToken;
    private Long roleId;

    public String getTempToken() {
        return tempToken;
    }

    public void setTempToken(String tempToken) {
        this.tempToken = tempToken;
    }

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }
}
