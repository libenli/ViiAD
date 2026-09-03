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
import com.mrd.ad.business.system.dto.AuthRoleOption;
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
import com.mrd.ad.business.system.service.SysVerificationCodeService;
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
    private final SysVerificationCodeService verificationCodeService;

    public SysAuthServiceImpl(SysUserMapper sysUserMapper,
                              SysRoleMapper sysRoleMapper,
                              SysUserRoleMapper sysUserRoleMapper,
                              SysMenuMapper sysMenuMapper,
                              SysRoleMenuMapper sysRoleMenuMapper,
                              SysLoginLogMapper sysLoginLogMapper,
                              SysOperLogMapper sysOperLogMapper,
                              SysPasswordService passwordService,
                              SysTokenService tokenService,
                              SysLoginSecurityService loginSecurityService,
                              SysVerificationCodeService verificationCodeService) {
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
        this.verificationCodeService = verificationCodeService;
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
        return buildLoginResult(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> loginByVerificationCode(String type, String countryCode, String target, String code) {
        String normalizedType = verificationCodeService.normalizeType(type);
        String loginName = StringUtils.trimToEmpty(target);
        loginSecurityService.checkAllowed(loginName);
        SysUser user = "email".equals(normalizedType)
                ? sysUserMapper.findActiveByEmailOrUsername(loginName)
                : sysUserMapper.findActiveByPhone(StringUtils.defaultIfBlank(StringUtils.trim(countryCode), "+86"), loginName);
        if (user == null) {
            loginSecurityService.recordFailure(loginName);
            saveLoginLog(loginName, null, "fail", "No active account is bound to this phone or email");
            throw new BusinessException("No active account is bound to this phone or email");
        }
        try {
            verificationCodeService.validateLoginCode(normalizedType, countryCode, target, code);
        } catch (BusinessException ex) {
            loginSecurityService.recordFailure(loginName);
            saveLoginLog(loginName, user.getId(), "fail", ex.getMessage());
            throw ex;
        }
        loginSecurityService.recordSuccess(loginName);
        saveLoginLog(loginName, user.getId(), "success", null);
        return buildLoginResult(user);
    }

    private Map<String, Object> buildLoginResult(SysUser user) {
        List<SysRole> roles = getRoles(user.getId());
        if (roles.size() > 1) {
            Map<String, Object> data = new HashMap<String, Object>();
            data.put("needRoleSelect", true);
            data.put("tempToken", tokenService.createTempToken(user.getId()));
            data.put("userId", user.getId());
            data.put("username", user.getUsername());
            data.put("realName", user.getRealName());
            data.put("roles", toRoleOptions(roles));
            return data;
        }
        Long roleId = roles.isEmpty() ? null : roles.get(0).getId();
        return buildFinalLoginResult(user, roleId);
    }

    @Override
    public Map<String, Object> selectRole(String tempToken, Long roleId) {
        Long userId = tokenService.parseTempUserId(tempToken);
        SysUser user = ensureUserExists(userId);
        SysRole role = ensureUserRole(userId, roleId);
        return buildFinalLoginResult(user, role.getId());
    }

    private Map<String, Object> buildFinalLoginResult(SysUser user, Long roleId) {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("needRoleSelect", false);
        data.put("token", tokenService.createToken(user.getId(), roleId));
        data.put("userId", user.getId());
        data.put("username", user.getUsername());
        data.put("realName", user.getRealName());
        if (roleId != null) {
            SysRole role = sysRoleMapper.selectById(roleId);
            if (role != null) {
                data.put("activeRoleId", role.getId());
                data.put("activeRoleCode", role.getRoleCode());
                data.put("activeRoleName", role.getRoleName());
            }
        }
        return data;
    }

    @Override
    public Map<String, Object> info(String token) {
        SysUser user = getUserByToken(token);
        List<SysRole> roles = getEffectiveRoles(token, user.getId());
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
        if (roles.size() == 1) {
            data.put("activeRoleId", roles.get(0).getId());
            data.put("activeRoleCode", roles.get(0).getRoleCode());
            data.put("activeRoleName", roles.get(0).getRoleName());
        }
        data.put("permissions", menus.stream().map(SysMenu::getPermissionCode).filter(StringUtils::isNotBlank).collect(Collectors.toList()));
        data.put("menus", menus.stream().map(SysMenu::getMenuPath).filter(this::isMenuPath).collect(Collectors.toList()));
        data.put("dataScope", roles.stream().anyMatch(this::isSuperAdminRole) ? "all" : "platform");
        return data;
    }

    @Override
    public List<String> menuPaths(String token) {
        SysUser user = getUserByToken(token);
        return getMenus(getEffectiveRoles(token, user.getId())).stream().map(SysMenu::getMenuPath).filter(this::isMenuPath).collect(Collectors.toList());
    }

    @Override
    public boolean hasPermission(String token, String permission) {
        if (StringUtils.isBlank(permission)) {
            return true;
        }
        SysUser user = getUserByToken(token);
        List<SysRole> roles = getEffectiveRoles(token, user.getId());
        if (roles.stream().anyMatch(this::isSuperAdminRole)) {
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

    private List<SysRole> getEffectiveRoles(String token, Long userId) {
        Long activeRoleId = tokenService.parseRoleId(token);
        if (activeRoleId == null) {
            return getRoles(userId);
        }
        SysRole role = ensureUserRole(userId, activeRoleId);
        List<SysRole> roles = new ArrayList<SysRole>();
        roles.add(role);
        return roles;
    }

    private SysRole ensureUserRole(Long userId, Long roleId) {
        if (roleId == null) {
            throw new BusinessException("请选择登录身份");
        }
        List<SysRole> roles = getRoles(userId);
        for (SysRole role : roles) {
            if (roleId.equals(role.getId())) {
                return role;
            }
        }
        throw new BusinessException(403, "当前账号没有该身份权限");
    }

    private List<AuthRoleOption> toRoleOptions(List<SysRole> roles) {
        List<AuthRoleOption> options = new ArrayList<AuthRoleOption>();
        for (SysRole role : roles) {
            options.add(new AuthRoleOption(role.getId(), role.getRoleCode(), role.getRoleName()));
        }
        return options;
    }

    private List<SysMenu> getMenus(List<SysRole> roles) {
        if (roles == null || roles.isEmpty()) {
            return new ArrayList<SysMenu>();
        }
        boolean superAdmin = roles.stream().anyMatch(this::isSuperAdminRole);
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
        if ("advertiser".equals(user.getUserType())) {
            if (request.getAdvertiserId() == null) {
                throw new BusinessException("广告主用户请选择广告主");
            }
            user.setAdvertiserId(request.getAdvertiserId());
            user.setAgentId(null);
        } else if ("agent".equals(user.getUserType())) {
            if (request.getAgentId() == null) {
                throw new BusinessException("代理商用户请选择代理商");
            }
            user.setAdvertiserId(null);
            user.setAgentId(request.getAgentId());
        } else {
            user.setAdvertiserId(null);
            user.setAgentId(null);
        }
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

    private boolean isSuperAdminRole(SysRole role) {
        if (role == null || StringUtils.isBlank(role.getRoleCode())) {
            return false;
        }
        String roleCode = role.getRoleCode();
        return "super_admin".equals(roleCode) || "admin".equals(roleCode) || "ADM".equalsIgnoreCase(roleCode);
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
