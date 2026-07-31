package com.mrd.ad.business.ad.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mrd.ad.business.ad.domain.AdOrder;
import com.mrd.ad.business.ad.dto.AdOrderCreateRequest;
import com.mrd.ad.business.ad.dto.AdOrderQuery;
import com.mrd.ad.business.ad.dto.AdOrderUpdateRequest;
import com.mrd.ad.business.ad.mapper.AdOrderMapper;
import com.mrd.ad.business.ad.service.AdOrderService;
import com.mrd.ad.business.system.service.SysDataScopeService;
import com.mrd.ad.common.core.PageResult;
import com.mrd.ad.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class AdOrderServiceImpl implements AdOrderService {

    private final AdOrderMapper adOrderMapper;
    private final SysDataScopeService dataScopeService;

    public AdOrderServiceImpl(AdOrderMapper adOrderMapper, SysDataScopeService dataScopeService) {
        this.adOrderMapper = adOrderMapper;
        this.dataScopeService = dataScopeService;
    }

    @Override
    public PageResult<AdOrder> page(AdOrderQuery query) {
        LambdaQueryWrapper<AdOrder> wrapper = new LambdaQueryWrapper<AdOrder>()
                .eq(AdOrder::getDeleted, 0)
                .orderByDesc(AdOrder::getCreateTime);
        applyDataScope(wrapper);

        if (StringUtils.isNotBlank(query.getKeyword())) {
            wrapper.and(item -> item
                    .like(AdOrder::getAdName, query.getKeyword())
                    .or()
                    .like(AdOrder::getAdCode, query.getKeyword()));
        }
        if (query.getAdvertiserId() != null) {
            wrapper.eq(AdOrder::getAdvertiserId, query.getAdvertiserId());
        }
        if (query.getAgentId() != null) {
            wrapper.eq(AdOrder::getAgentId, query.getAgentId());
        }
        if (StringUtils.isNotBlank(query.getRegionCode())) {
            wrapper.eq(AdOrder::getRegionCode, query.getRegionCode());
        }
        if (StringUtils.isNotBlank(query.getAdType())) {
            wrapper.eq(AdOrder::getAdType, query.getAdType());
        }
        if (StringUtils.isNotBlank(query.getStatus())) {
            wrapper.eq(AdOrder::getStatus, query.getStatus());
        }

        Page<AdOrder> page = adOrderMapper.selectPage(new Page<AdOrder>(query.getPage(), query.getSize()), wrapper);
        return new PageResult<AdOrder>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public AdOrder getDetail(Long id) {
        AdOrder adOrder = adOrderMapper.selectById(id);
        if (adOrder == null || Integer.valueOf(1).equals(adOrder.getDeleted())) {
            throw new BusinessException(404, "广告不存在");
        }
        dataScopeService.assertAdVisible(adOrder);
        return adOrder;
    }

    @Override
    public AdOrder create(AdOrderCreateRequest request) {
        Date now = new Date();
        AdOrder adOrder = new AdOrder();
        copyRequest(request, adOrder);
        applyOwnerScope(adOrder);
        adOrder.setAdCode(nextAdCode(now));
        adOrder.setStatus("draft");
        adOrder.setCreateUserId(dataScopeService.currentUserId());
        adOrder.setCreateBy(dataScopeService.currentUserId());
        adOrder.setCreateTime(now);
        adOrder.setUpdateTime(now);
        adOrder.setDeleted(0);
        adOrderMapper.insert(adOrder);
        return adOrder;
    }

    @Override
    public AdOrder update(Long id, AdOrderUpdateRequest request) {
        AdOrder adOrder = getDetail(id);
        if (!"draft".equals(adOrder.getStatus()) && !"rejected".equals(adOrder.getStatus())) {
            throw new BusinessException("只有草稿或已驳回广告可以编辑");
        }
        copyRequest(request, adOrder);
        applyOwnerScope(adOrder);
        adOrder.setUpdateTime(new Date());
        adOrderMapper.updateById(adOrder);
        return adOrder;
    }

    @Override
    public AdOrder submit(Long id) {
        AdOrder adOrder = getDetail(id);
        if (!"draft".equals(adOrder.getStatus()) && !"rejected".equals(adOrder.getStatus())) {
            throw new BusinessException("当前状态不能提交审核");
        }
        adOrder.setStatus("submitted");
        adOrder.setSubmitTime(new Date());
        adOrder.setUpdateTime(new Date());
        adOrderMapper.updateById(adOrder);
        return adOrder;
    }

    @Override
    public AdOrder approve(Long id, String comment) {
        AdOrder adOrder = getDetail(id);
        adOrder.setStatus("approved");
        adOrder.setAuditUserId(1L);
        adOrder.setAuditComment(StringUtils.defaultIfBlank(comment, "审核通过"));
        adOrder.setAuditTime(new Date());
        adOrder.setUpdateTime(new Date());
        adOrderMapper.updateById(adOrder);
        return adOrder;
    }

    @Override
    public AdOrder reject(Long id, String comment) {
        if (StringUtils.isBlank(comment)) {
            throw new BusinessException("驳回原因不能为空");
        }
        AdOrder adOrder = getDetail(id);
        adOrder.setStatus("rejected");
        adOrder.setAuditUserId(1L);
        adOrder.setAuditComment(comment);
        adOrder.setAuditTime(new Date());
        adOrder.setUpdateTime(new Date());
        adOrderMapper.updateById(adOrder);
        return adOrder;
    }

    private void copyRequest(AdOrderCreateRequest request, AdOrder adOrder) {
        adOrder.setAdName(request.getAdName());
        adOrder.setAdvertiserId(request.getAdvertiserId());
        adOrder.setAgentId(request.getAgentId());
        adOrder.setAdType(request.getAdType());
        adOrder.setObjective(request.getObjective());
        adOrder.setRegionCode(request.getRegionCode());
        adOrder.setBudgetAmount(request.getBudgetAmount());
        adOrder.setDescription(request.getDescription());
    }

    private String nextAdCode(Date date) {
        return "AD" + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(date);
    }

    private void applyDataScope(LambdaQueryWrapper<AdOrder> wrapper) {
        if (dataScopeService.isAllData()) {
            return;
        }
        if (dataScopeService.isAdvertiserScope() && dataScopeService.currentAdvertiserId() != null) {
            wrapper.eq(AdOrder::getAdvertiserId, dataScopeService.currentAdvertiserId());
            return;
        }
        if (dataScopeService.isAgentScope() && dataScopeService.currentAgentId() != null) {
            wrapper.eq(AdOrder::getAgentId, dataScopeService.currentAgentId());
            return;
        }
        wrapper.eq(AdOrder::getId, -1L);
    }

    private void applyOwnerScope(AdOrder adOrder) {
        if (dataScopeService.isAllData()) {
            return;
        }
        if (dataScopeService.isAdvertiserScope() && dataScopeService.currentAdvertiserId() != null) {
            adOrder.setAdvertiserId(dataScopeService.currentAdvertiserId());
            return;
        }
        if (dataScopeService.isAgentScope() && dataScopeService.currentAgentId() != null) {
            adOrder.setAgentId(dataScopeService.currentAgentId());
            return;
        }
        throw new BusinessException(403, "无数据权限");
    }
}
