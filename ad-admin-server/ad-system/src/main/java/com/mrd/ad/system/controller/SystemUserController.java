package com.mrd.ad.system.controller;

import com.mrd.ad.business.system.domain.SysLoginLog;
import com.mrd.ad.business.system.domain.SysMenu;
import com.mrd.ad.business.system.domain.SysOperLog;
import com.mrd.ad.business.system.domain.SysRole;
import com.mrd.ad.business.system.domain.SysUser;
import com.mrd.ad.business.system.dto.RoleMenuSaveRequest;
import com.mrd.ad.business.system.dto.ResetPasswordRequest;
import com.mrd.ad.business.system.dto.SysDictOption;
import com.mrd.ad.business.system.dto.SysLoginLogQuery;
import com.mrd.ad.business.system.dto.SysOperLogQuery;
import com.mrd.ad.business.system.dto.SysUserQuery;
import com.mrd.ad.business.system.dto.SysUserSaveRequest;
import com.mrd.ad.business.system.dto.UserRoleSaveRequest;
import com.mrd.ad.business.system.service.SysAuthService;
import com.mrd.ad.business.system.service.SysDictService;
import com.mrd.ad.common.annotation.RequiresPermission;
import com.mrd.ad.common.core.ApiResult;
import com.mrd.ad.common.core.PageResult;
import com.mrd.ad.logging.annotation.OperLog;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemUserController {

    private final SysAuthService sysAuthService;
    private final SysDictService sysDictService;

    public SystemUserController(SysAuthService sysAuthService, SysDictService sysDictService) {
        this.sysAuthService = sysAuthService;
        this.sysDictService = sysDictService;
    }

    @OperLog(module = "系统用户", businessType = "QUERY")
    @GetMapping("/users")
    public ApiResult<PageResult<SysUser>> users(SysUserQuery query) {
        return ApiResult.success(sysAuthService.pageUsers(query));
    }

    @RequiresPermission("system:user:manage")
    @PostMapping("/users")
    public ApiResult<SysUser> createUser(@Validated @RequestBody SysUserSaveRequest request) {
        return ApiResult.success(sysAuthService.createUser(request));
    }

    @RequiresPermission("system:user:manage")
    @PutMapping("/users/{id}")
    public ApiResult<SysUser> updateUser(@PathVariable Long id, @Validated @RequestBody SysUserSaveRequest request) {
        return ApiResult.success(sysAuthService.updateUser(id, request));
    }

    @RequiresPermission("system:user:manage")
    @PostMapping("/users/{id}/enable")
    public ApiResult<SysUser> enableUser(@PathVariable Long id) {
        return ApiResult.success(sysAuthService.changeUserStatus(id, "active"));
    }

    @RequiresPermission("system:user:manage")
    @PostMapping("/users/{id}/disable")
    public ApiResult<SysUser> disableUser(@PathVariable Long id) {
        return ApiResult.success(sysAuthService.changeUserStatus(id, "disabled"));
    }

    @RequiresPermission("system:user:manage")
    @PostMapping("/users/{id}/reset-password")
    public ApiResult<SysUser> resetPassword(@PathVariable Long id, @Validated @RequestBody ResetPasswordRequest request) {
        return ApiResult.success(sysAuthService.resetPassword(id, request.getPassword()));
    }

    @GetMapping("/users/{id}/roles")
    public ApiResult<List<Long>> userRoles(@PathVariable Long id) {
        return ApiResult.success(sysAuthService.listUserRoleIds(id));
    }

    @RequiresPermission("system:user:manage")
    @PutMapping("/users/{id}/roles")
    public ApiResult<Void> saveUserRoles(@PathVariable Long id, @Validated @RequestBody UserRoleSaveRequest request) {
        sysAuthService.saveUserRoles(id, request.getRoleIds());
        return ApiResult.success();
    }

    @GetMapping("/roles")
    public ApiResult<List<SysRole>> roles() {
        return ApiResult.success(sysAuthService.listRoles());
    }

    @GetMapping("/menus")
    public ApiResult<List<SysMenu>> menus() {
        return ApiResult.success(sysAuthService.listMenus());
    }

    @GetMapping("/roles/{roleId}/menus")
    public ApiResult<List<Long>> roleMenus(@PathVariable Long roleId) {
        return ApiResult.success(sysAuthService.listRoleMenuIds(roleId));
    }

    @RequiresPermission("system:role:grant")
    @PutMapping("/roles/{roleId}/menus")
    public ApiResult<Void> saveRoleMenus(@PathVariable Long roleId, @Validated @RequestBody RoleMenuSaveRequest request) {
        sysAuthService.saveRoleMenus(roleId, request.getMenuIds());
        return ApiResult.success();
    }

    @RequiresPermission("system:user:manage")
    @GetMapping("/login-logs")
    public ApiResult<PageResult<SysLoginLog>> loginLogs(SysLoginLogQuery query) {
        return ApiResult.success(sysAuthService.pageLoginLogs(query));
    }

    @OperLog(module = "操作日志", businessType = "QUERY")
    @RequiresPermission("system:log:view")
    @GetMapping("/oper-logs")
    public ApiResult<PageResult<SysOperLog>> operLogs(SysOperLogQuery query) {
        return ApiResult.success(sysAuthService.pageOperLogs(query));
    }

    @GetMapping("/dicts")
    public ApiResult<Map<String, List<SysDictOption>>> dicts() {
        return ApiResult.success(sysDictService.listAll());
    }

    @GetMapping("/dicts/{code}")
    public ApiResult<List<SysDictOption>> dictItems(@PathVariable String code) {
        return ApiResult.success(sysDictService.listByCode(code));
    }
}
