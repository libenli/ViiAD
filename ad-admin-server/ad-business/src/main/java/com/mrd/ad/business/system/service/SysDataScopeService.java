package com.mrd.ad.business.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mrd.ad.business.ad.domain.AdOrder;
import com.mrd.ad.business.ad.mapper.AdOrderMapper;
import com.mrd.ad.business.system.domain.SysUser;
import com.mrd.ad.business.system.mapper.SysUserMapper;
import com.mrd.ad.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysDataScopeService {

    private final SysUserMapper sysUserMapper;
    private final AdOrderMapper adOrderMapper;
    private final SysTokenService tokenService;

    public SysDataScopeService(SysUserMapper sysUserMapper, AdOrderMapper adOrderMapper, SysTokenService tokenService) {
        this.sysUserMapper = sysUserMapper;
        this.adOrderMapper = adOrderMapper;
        this.tokenService = tokenService;
    }

    public SysUser currentUser() {
        String token = currentToken();
        if (StringUtils.isBlank(token)) {
            return null;
        }
        try {
            SysUser user = sysUserMapper.selectById(tokenService.parseUserId(token));
            if (user == null || Integer.valueOf(1).equals(user.getDeleted()) || !"active".equals(user.getStatus())) {
                return null;
            }
            return user;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public Long currentUserId() {
        SysUser user = currentUser();
        return user == null ? 1L : user.getId();
    }

    public boolean isAllData() {
        SysUser user = currentUser();
        return user != null && "platform".equals(user.getUserType());
    }

    public boolean isAdvertiserScope() {
        SysUser user = currentUser();
        return user != null && "advertiser".equals(user.getUserType());
    }

    public boolean isAgentScope() {
        SysUser user = currentUser();
        return user != null && "agent".equals(user.getUserType());
    }

    public Long currentAdvertiserId() {
        SysUser user = currentUser();
        return user == null ? null : user.getAdvertiserId();
    }

    public Long currentAgentId() {
        SysUser user = currentUser();
        return user == null ? null : user.getAgentId();
    }

    public List<Long> scopedAdIds() {
        if (isAllData()) {
            return null;
        }
        LambdaQueryWrapper<AdOrder> wrapper = new LambdaQueryWrapper<AdOrder>().eq(AdOrder::getDeleted, 0);
        if (isAdvertiserScope() && currentAdvertiserId() != null) {
            wrapper.eq(AdOrder::getAdvertiserId, currentAdvertiserId());
        } else if (isAgentScope() && currentAgentId() != null) {
            wrapper.eq(AdOrder::getAgentId, currentAgentId());
        } else {
            return Collections.emptyList();
        }
        return adOrderMapper.selectList(wrapper).stream().map(AdOrder::getId).collect(Collectors.toList());
    }

    public void assertAdVisible(AdOrder adOrder) {
        if (adOrder == null || isAllData()) {
            return;
        }
        if (isAdvertiserScope() && currentAdvertiserId() != null && currentAdvertiserId().equals(adOrder.getAdvertiserId())) {
            return;
        }
        if (isAgentScope() && currentAgentId() != null && currentAgentId().equals(adOrder.getAgentId())) {
            return;
        }
        throw new BusinessException(403, "无数据权限");
    }

    public void assertAdIdVisible(Long adId) {
        if (adId == null || isAllData()) {
            return;
        }
        AdOrder adOrder = adOrderMapper.selectById(adId);
        assertAdVisible(adOrder);
    }

    public void assertAdvertiserAgentVisible(Long advertiserId, Long agentId) {
        if (isAllData()) {
            return;
        }
        if (isAdvertiserScope() && currentAdvertiserId() != null && currentAdvertiserId().equals(advertiserId)) {
            return;
        }
        if (isAgentScope() && currentAgentId() != null && currentAgentId().equals(agentId)) {
            return;
        }
        throw new BusinessException(403, "无数据权限");
    }

    private String currentToken() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes)) {
            return null;
        }
        HttpServletRequest request = ((ServletRequestAttributes) attributes).getRequest();
        return request.getHeader("Authorization");
    }
}
