package com.mrd.ad.business.system.dto;

import javax.validation.constraints.NotNull;
import java.util.List;

public class RoleMenuSaveRequest {

    @NotNull(message = "权限不能为空")
    private List<Long> menuIds;

    public List<Long> getMenuIds() {
        return menuIds;
    }

    public void setMenuIds(List<Long> menuIds) {
        this.menuIds = menuIds;
    }
}
