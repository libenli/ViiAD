package com.mrd.ad.business.partner.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mrd.ad.business.partner.domain.AdAgent;
import com.mrd.ad.business.partner.dto.AgentCreateRequest;
import com.mrd.ad.business.partner.dto.AgentQuery;
import com.mrd.ad.business.partner.dto.AgentUpdateRequest;
import com.mrd.ad.business.partner.mapper.AdAgentMapper;
import com.mrd.ad.business.partner.service.AdAgentService;
import com.mrd.ad.common.core.PageResult;
import com.mrd.ad.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class AdAgentServiceImpl implements AdAgentService {

    private final AdAgentMapper adAgentMapper;

    public AdAgentServiceImpl(AdAgentMapper adAgentMapper) {
        this.adAgentMapper = adAgentMapper;
    }

    @Override
    public PageResult<AdAgent> page(AgentQuery query) {
        LambdaQueryWrapper<AdAgent> wrapper = new LambdaQueryWrapper<AdAgent>()
                .eq(AdAgent::getDeleted, 0)
                .orderByDesc(AdAgent::getCreateTime);
        if (StringUtils.isNotBlank(query.getKeyword())) {
            wrapper.and(item -> item
                    .like(AdAgent::getAgentName, query.getKeyword())
                    .or()
                    .like(AdAgent::getAgentCode, query.getKeyword()));
        }
        if (StringUtils.isNotBlank(query.getStatus())) {
            wrapper.eq(AdAgent::getStatus, query.getStatus());
        }
        if (query.getOwnerUserId() != null) {
            wrapper.eq(AdAgent::getOwnerUserId, query.getOwnerUserId());
        }
        Page<AdAgent> page = adAgentMapper.selectPage(new Page<AdAgent>(query.getPage(), query.getSize()), wrapper);
        return new PageResult<AdAgent>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public AdAgent getDetail(Long id) {
        AdAgent agent = adAgentMapper.selectById(id);
        if (agent == null || Integer.valueOf(1).equals(agent.getDeleted())) {
            throw new BusinessException(404, "代理商不存在");
        }
        return agent;
    }

    @Override
    public AdAgent create(AgentCreateRequest request) {
        Date now = new Date();
        AdAgent agent = new AdAgent();
        copyRequest(request, agent);
        agent.setAgentCode("AGT" + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(now));
        agent.setStatus("active");
        agent.setCreateBy(1L);
        agent.setCreateTime(now);
        agent.setUpdateTime(now);
        agent.setDeleted(0);
        adAgentMapper.insert(agent);
        return agent;
    }

    @Override
    public AdAgent update(Long id, AgentUpdateRequest request) {
        AdAgent agent = getDetail(id);
        copyRequest(request, agent);
        agent.setUpdateBy(1L);
        agent.setUpdateTime(new Date());
        adAgentMapper.updateById(agent);
        return agent;
    }

    @Override
    public AdAgent enable(Long id) {
        return updateStatus(id, "active");
    }

    @Override
    public AdAgent disable(Long id) {
        return updateStatus(id, "disabled");
    }

    private AdAgent updateStatus(Long id, String status) {
        AdAgent agent = getDetail(id);
        agent.setStatus(status);
        agent.setUpdateBy(1L);
        agent.setUpdateTime(new Date());
        adAgentMapper.updateById(agent);
        return agent;
    }

    private void copyRequest(AgentCreateRequest request, AdAgent agent) {
        agent.setAgentName(request.getAgentName());
        agent.setContactName(request.getContactName());
        agent.setContactPhone(request.getContactPhone());
        agent.setContactEmail(request.getContactEmail());
        agent.setOwnerUserId(request.getOwnerUserId());
        agent.setRemark(request.getRemark());
    }
}
