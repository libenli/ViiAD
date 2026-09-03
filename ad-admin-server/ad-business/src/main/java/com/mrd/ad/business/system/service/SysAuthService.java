package com.mrd.ad.business.system.service;

import com.mrd.ad.business.system.domain.SysRole;
import com.mrd.ad.business.system.domain.SysUser;
import com.mrd.ad.business.system.domain.SysMenu;
import com.mrd.ad.business.system.domain.SysLoginLog;
import com.mrd.ad.business.system.domain.SysOperLog;
import com.mrd.ad.business.system.dto.SysLoginLogQuery;
import com.mrd.ad.business.system.dto.SysOperLogQuery;
import com.mrd.ad.business.system.dto.SysUserQuery;
import com.mrd.ad.business.system.dto.SysUserSaveRequest;
import com.mrd.ad.common.core.PageResult;

import java.util.List;
import java.util.Map;

public interface SysAuthService {

    Map<String, Object> login(String username, String password);

    Map<String, Object> loginByVerificationCode(String type, String countryCode, String target, String code);

    Map<String, Object> selectRole(String tempToken, Long roleId);

    void changePassword(String token, String oldPassword, String newPassword);

    Map<String, Object> info(String token);

    List<String> menuPaths(String token);

    boolean hasPermission(String token, String permission);

    PageResult<SysUser> pageUsers(SysUserQuery query);

    SysUser createUser(SysUserSaveRequest request);

    SysUser updateUser(Long id, SysUserSaveRequest request);

    SysUser changeUserStatus(Long id, String status);

    SysUser resetPassword(Long id, String password);

    List<Long> listUserRoleIds(Long userId);

    void saveUserRoles(Long userId, List<Long> roleIds);

    List<SysRole> listRoles();

    List<SysMenu> listMenus();

    List<Long> listRoleMenuIds(Long roleId);

    void saveRoleMenus(Long roleId, List<Long> menuIds);

    PageResult<SysLoginLog> pageLoginLogs(SysLoginLogQuery query);

    PageResult<SysOperLog> pageOperLogs(SysOperLogQuery query);
}
