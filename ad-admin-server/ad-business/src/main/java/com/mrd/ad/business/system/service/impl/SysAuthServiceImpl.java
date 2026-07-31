package com.mrd.ad.business.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mrd.ad.business.system.domain.SysMenu;
import com.mrd.ad.business.system.domain.SysRole;
import com.mrd.ad.business.system.domain.SysRoleMenu;
import com.mrd.ad.business.system.domain.SysUser;
import com.mrd.ad.business.system.domain.SysUserRole;
import com.mrd.ad.business.system.domain.SysLoginLog;
import com.mrd.ad.business.system.domain.SysOperLog;
import com.mrd.ad.business.system.dto.SysLoginLogQuery;
import com.mrd.ad.business.system.dto.SysOperLogQuery;
import com.mrd.ad.business.system.dto.SysUserQuery;
import com.mrd.ad.business.system.dto.SysUserSaveRequest;
import com.mrd.ad.business.system.mapper.SysLoginLogMapper;
import com.mrd.ad.business.system.mapper.SysMenuMapper;
import com.mrd.ad.business.system.mapper.SysOperLogMapper;
import com.mrd.ad.business.system.mapper.SysRoleMapper;
import com.mrd.ad.business.system.mapper.SysRoleMenuMapper;
import com.mrd.ad.business.system.mapper.SysUserMapper;
import com.mrd.ad.business.system.mapper.SysUserRoleMapper;
import com.mrd.ad.business.system.service.SysAuthService;
import com.mrd.ad.business.system.service.SysLoginSecurityService;
import com.mrd.ad.business.system.service.SysPasswordService;
import com.mrd.ad.business.system.service.SysTokenService;
import com.mrd.ad.common.core.PageResult;
import com.mrd.ad.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SysAuthServiceImpl implements SysAuthService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysLoginLogMapper sysLoginLogMapper;
    private final SysOperLogMapper sysOperLogMapper;
    private final SysPasswordService passwordService;
    private final SysTokenService tokenService;
    private final SysLoginSecurityService loginSecurityService;

    public SysAuthServiceImpl(SysUserMapper sysUserMapper,
                              SysRoleMapper sysRoleMapper,
                              SysUserRoleMapper sysUserRoleMapper,
                              SysMenuMapper sysMenuMapper,
                              SysRoleMenuMapper sysRoleMenuMapper,
                              SysLoginLogMapper sysLoginLogMapper,
                              SysOperLogMapper sysOperLogMapper,
                              SysPasswordService passwordService,
                              SysTokenService tokenService,
                              SysLoginSecurityService loginSecurityService) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysMenuMapper = sysMenuMapper;
        this.sysRoleMenuMapper = sysRoleMenuMapper;
        this.sysLoginLogMapper = sysLoginLogMapper;
        this.sysOperLogMapper = sysOperLogMapper;
        this.passwordService = passwordService;
        this.tokenService = tokenService;
        this.loginSecurityService = loginSecurityService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> login(String username, String password) {
        loginSecurityService.checkAllowed(username);
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDeleted, 0)
                .last("LIMIT 1"));
        if (user == null || !"active".equals(user.getStatus())) {
            loginSecurityService.recordFailure(username);
            saveLoginLog(username, null, "fail", "账号不存在或已停用");
            throw new BusinessException("账号不存在或已停用");
        }
        if (!passwordService.matches(password, user.getPassword())) {
            loginSecurityService.recordFailure(username);
            saveLoginLog(username, user.getId(), "fail", "账号或密码错误");
            throw new BusinessException("账号或密码错误");
        }
        if (!passwordService.isEncoded(user.getPassword())) {
            user.setPassword(passwordService.encode(password));
            sysUserMapper.updateById(user);
        }
        loginSecurityService.recordSuccess(username);
        saveLoginLog(username, user.getId(), "success", null);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("token", tokenService.createToken(user.getId()));
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        return data;
    }

    @Override
    public Map<String, Object> info(String token) {
        SysUser user = getUserByToken(token);
        List<SysRole> roles = getRoles(user.getId());
        List<SysMenu> menus = getMenus(roles);
        Map<String, Object> userMap = new HashMap<String, Object>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("realName", user.getRealName());
        userMap.put("userType", user.getUserType());
        userMap.put("advertiserId", user.getAdvertiserId());
        userMap.put("agentId", user.getAgentId());

        Map<String, Object> data = new HashMap<String, Object>();
        data.put("user", userMap);
        data.put("roles", roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList()));
        data.put("permissions", menus.stream().map(SysMenu::getPermissionCode).filter(StringUtils::isNotBlank).collect(Collectors.toList()));
        data.put("menus", menus.stream().map(SysMenu::getMenuPath).filter(this::isMenuPath).collect(Collectors.toList()));
        data.put("dataScope", roles.stream().anyMatch(item -> "super_admin".equals(item.getRoleCode())) ? "all" : "platform");
        return data;
    }

    @Override
    public List<String> menuPaths(String token) {
        SysUser user = getUserByToken(token);
        return getMenus(getRoles(user.getId())).stream().map(SysMenu::getMenuPath).filter(this::isMenuPath).collect(Collectors.toList());
    }

    @Override
    public boolean hasPermission(String token, String permission) {
        if (StringUtils.isBlank(permission)) {
            return true;
        }
        SysUser user = getUserByToken(token);
        List<SysRole> roles = getRoles(user.getId());
        if (roles.stream().anyMatch(item -> "super_admin".equals(item.getRoleCode()))) {
            return true;
        }
        return getMenus(roles).stream()
                .map(SysMenu::getPermissionCode)
                .filter(StringUtils::isNotBlank)
                .anyMatch(permission::equals);
    }

    @Override
    public PageResult<SysUser> pageUsers(SysUserQuery query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeleted, 0)
                .orderByDesc(SysUser::getCreateTime);
        if (StringUtils.isNotBlank(query.getKeyword())) {
            wrapper.and(item -> item
                    .like(SysUser::getUsername, query.getKeyword())
                    .or()
                    .like(SysUser::getRealName, query.getKeyword()));
        }
        if (StringUtils.isNotBlank(query.getStatus())) {
            wrapper.eq(SysUser::getStatus, query.getStatus());
        }
        if (StringUtils.isNotBlank(query.getUserType())) {
            wrapper.eq(SysUser::getUserType, query.getUserType());
        }
        Page<SysUser> page = sysUserMapper.selectPage(new Page<SysUser>(query.getPage(), query.getSize()), wrapper);
        for (SysUser user : page.getRecords()) {
            user.setPassword(null);
        }
        return new PageResult<SysUser>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser createUser(SysUserSaveRequest request) {
        ensureUsernameAvailable(null, request.getUsername());
        SysUser user = new SysUser();
        fillUser(user, request);
        user.setPassword(passwordService.encode(StringUtils.defaultIfBlank(request.getPassword(), "admin123")));
        user.setStatus(StringUtils.defaultIfBlank(request.getStatus(), "active"));
        user.setDeleted(0);
        sysUserMapper.insert(user);
        saveUserRoles(user.getId(), request.getRoleIds());
        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser updateUser(Long id, SysUserSaveRequest request) {
        SysUser user = ensureUserExists(id);
        ensureUsernameAvailable(id, request.getUsername());
        fillUser(user, request);
        if (StringUtils.isNotBlank(request.getStatus())) {
            user.setStatus(request.getStatus());
        }
        sysUserMapper.updateById(user);
        saveUserRoles(id, request.getRoleIds());
        user.setPassword(null);
        return user;
    }

    @Override
    public SysUser changeUserStatus(Long id, String status) {
        if (!"active".equals(status) && !"disabled".equals(status)) {
            throw new BusinessException("账号状态不正确");
        }
        SysUser user = ensureUserExists(id);
        user.setStatus(status);
        sysUserMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

    @Override
    public SysUser resetPassword(Long id, String password) {
        if (StringUtils.isBlank(password)) {
            throw new BusinessException("新密码不能为空");
        }
        SysUser user = ensureUserExists(id);
        user.setPassword(passwordService.encode(password));
        sysUserMapper.updateById(user);
        user.setPassword(null);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(String token, String oldPassword, String newPassword) {
        if (StringUtils.isBlank(newPassword) || newPassword.length() < 6) {
            throw new BusinessException("新密码至少 6 位");
        }
        SysUser user = getUserByToken(token);
        if (!passwordService.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        user.setPassword(passwordService.encode(newPassword));
        sysUserMapper.updateById(user);
    }

    @Override
    public PageResult<SysLoginLog> pageLoginLogs(SysLoginLogQuery query) {
        LambdaQueryWrapper<SysLoginLog> wrapper = new LambdaQueryWrapper<SysLoginLog>()
                .orderByDesc(SysLoginLog::getLoginTime);
        if (StringUtils.isNotBlank(query.getUsername())) {
            wrapper.like(SysLoginLog::getUsername, query.getUsername());
        }
        if (StringUtils.isNotBlank(query.getLoginStatus())) {
            wrapper.eq(SysLoginLog::getLoginStatus, query.getLoginStatus());
        }
        Page<SysLoginLog> page = sysLoginLogMapper.selectPage(new Page<SysLoginLog>(query.getPage(), query.getSize()), wrapper);
        return new PageResult<SysLoginLog>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public PageResult<SysOperLog> pageOperLogs(SysOperLogQuery query) {
        LambdaQueryWrapper<SysOperLog> wrapper = new LambdaQueryWrapper<SysOperLog>()
                .orderByDesc(SysOperLog::getCreateTime);
        if (StringUtils.isNotBlank(query.getModuleName())) {
            wrapper.like(SysOperLog::getModuleName, query.getModuleName());
        }
        if (StringUtils.isNotBlank(query.getBusinessType())) {
            wrapper.eq(SysOperLog::getBusinessType, query.getBusinessType());
        }
        if (StringUtils.isNotBlank(query.getOperatorName())) {
            wrapper.like(SysOperLog::getOperatorName, query.getOperatorName());
        }
        if (query.getStatus() != null) {
            wrapper.eq(SysOperLog::getStatus, query.getStatus());
        }
        if (StringUtils.isNotBlank(query.getStartTime())) {
            wrapper.ge(SysOperLog::getCreateTime, query.getStartTime());
        }
        if (StringUtils.isNotBlank(query.getEndTime())) {
            wrapper.le(SysOperLog::getCreateTime, query.getEndTime());
        }
        Page<SysOperLog> page = sysOperLogMapper.selectPage(new Page<SysOperLog>(query.getPage(), query.getSize()), wrapper);
        return new PageResult<SysOperLog>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<Long> listUserRoleIds(Long userId) {
        ensureUserExists(userId);
        return sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserRoles(Long userId, List<Long> roleIds) {
        ensureUserExists(userId);
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        Set<Long> distinctRoleIds = new LinkedHashSet<Long>(roleIds);
        for (Long roleId : distinctRoleIds) {
            if (roleId == null) {
                continue;
            }
            ensureRoleExists(roleId);
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            sysUserRoleMapper.insert(userRole);
        }
    }

    @Override
    public List<SysRole> listRoles() {
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>().eq(SysRole::getStatus, "active"));
    }

    @Override
    public List<SysMenu> listMenus() {
        return sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .eq(SysMenu::getStatus, "active")
                .orderByAsc(SysMenu::getSortNo));
    }

    @Override
    public List<Long> listRoleMenuIds(Long roleId) {
        ensureRoleExists(roleId);
        return sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveRoleMenus(Long roleId, List<Long> menuIds) {
        ensureRoleExists(roleId);
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        Set<Long> distinctMenuIds = new LinkedHashSet<Long>(menuIds);
        for (Long menuId : distinctMenuIds) {
            if (menuId == null) {
                continue;
            }
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menuId);
            sysRoleMenuMapper.insert(roleMenu);
        }
    }

    private SysUser getUserByToken(String token) {
        Long userId = tokenService.parseUserId(token);
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException(401, "登录已失效");
        }
        return user;
    }

    private List<SysRole> getRoles(Long userId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (userRoles == null || userRoles.isEmpty()) {
            return new ArrayList<SysRole>();
        }
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds).eq(SysRole::getStatus, "active"));
    }

    private List<SysMenu> getMenus(List<SysRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return new ArrayList<SysMenu>();
        }
        boolean superAdmin = roles.stream().anyMatch(item -> "super_admin".equals(item.getRoleCode()));
        if (superAdmin) {
            return sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getStatus, "active").orderByAsc(SysMenu::getSortNo));
        }
        List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());
        List<SysRoleMenu> roleMenus = sysRoleMenuMapper.selectList(new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds));
        if (roleMenus == null || roleMenus.isEmpty()) {
            return new ArrayList<SysMenu>();
        }
        Set<Long> menuIds = roleMenus.stream().map(SysRoleMenu::getMenuId).collect(Collectors.toCollection(LinkedHashSet::new));
        return sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>().in(SysMenu::getId, menuIds).eq(SysMenu::getStatus, "active").orderByAsc(SysMenu::getSortNo));
    }

    private void ensureRoleExists(Long roleId) {
        if (roleId == null) {
            throw new BusinessException("角色不能为空");
        }
        SysRole role = sysRoleMapper.selectById(roleId);
        if (role == null || !"active".equals(role.getStatus())) {
            throw new BusinessException(404, "角色不存在或已停用");
        }
    }

    private SysUser ensureUserExists(Long id) {
        if (id == null) {
            throw new BusinessException("用户不能为空");
        }
        SysUser user = sysUserMapper.selectById(id);
        if (user == null || Integer.valueOf(1).equals(user.getDeleted())) {
            throw new BusinessException(404, "用户不存在");
        }
        return user;
    }

    private void ensureUsernameAvailable(Long id, String username) {
        SysUser exists = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .eq(SysUser::getDeleted, 0)
                .last("LIMIT 1"));
        if (exists != null && (id == null || !id.equals(exists.getId()))) {
            throw new BusinessException("账号已存在");
        }
    }

    private void fillUser(SysUser user, SysUserSaveRequest request) {
        user.setUsername(request.getUsername());
        user.setRealName(request.getRealName());
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user.setUserType(StringUtils.defaultIfBlank(request.getUserType(), "platform"));
        user.setAdvertiserId(request.getAdvertiserId());
        user.setAgentId(request.getAgentId());
    }

    private void saveLoginLog(String username, Long userId, String status, String reason) {
        SysLoginLog log = new SysLoginLog();
        log.setUsername(username);
        log.setUserId(userId);
        log.setLoginStatus(status);
        log.setFailReason(reason);
        log.setIpAddress(currentIpAddress());
        log.setUserAgent(currentUserAgent());
        log.setLoginTime(new Date());
        sysLoginLogMapper.insert(log);
    }

    private boolean isMenuPath(String path) {
        return StringUtils.isNotBlank(path) && path.startsWith("/");
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            return null;
        }
        return ((ServletRequestAttributes) attributes).getRequest();
    }

    private String currentIpAddress() {
        HttpServletRequest request = currentRequest();
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return StringUtils.defaultIfBlank(realIp, request.getRemoteAddr());
    }

    private String currentUserAgent() {
        HttpServletRequest request = currentRequest();
        return request == null ? null : request.getHeader("User-Agent");
    }
}
