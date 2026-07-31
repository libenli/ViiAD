package com.mrd.ad.business.system.dto;

import javax.validation.constraints.NotNull;
import java.util.List;

public class UserRoleSaveRequest {

    @NotNull(message = "角色不能为空")
    private List<Long> roleIds;

    public List<Long> getRoleIds() {
        return roleIds;
    }

    public void setRoleIds(List<Long> roleIds) {
        this.roleIds = roleIds;
    }
}
