package com.mrd.ad.business.workorder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mrd.ad.business.workorder.domain.AdWorkOrder;
import com.mrd.ad.business.workorder.dto.AdWorkOrderCreateRequest;
import com.mrd.ad.business.workorder.dto.AdWorkOrderQuery;
import com.mrd.ad.business.workorder.mapper.AdWorkOrderMapper;
import com.mrd.ad.business.workorder.service.AdWorkOrderService;
import com.mrd.ad.common.core.PageResult;
import com.mrd.ad.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class AdWorkOrderServiceImpl implements AdWorkOrderService {

    private final AdWorkOrderMapper adWorkOrderMapper;

    public AdWorkOrderServiceImpl(AdWorkOrderMapper adWorkOrderMapper) {
        this.adWorkOrderMapper = adWorkOrderMapper;
    }

    @Override
    public PageResult<AdWorkOrder> page(AdWorkOrderQuery query) {
        LambdaQueryWrapper<AdWorkOrder> wrapper = new LambdaQueryWrapper<AdWorkOrder>()
                .eq(AdWorkOrder::getDeleted, 0)
                .orderByDesc(AdWorkOrder::getCreateTime);
        if (StringUtils.isNotBlank(query.getKeyword())) {
            wrapper.and(item -> item
                    .like(AdWorkOrder::getTitle, query.getKeyword())
                    .or()
                    .like(AdWorkOrder::getWorkNo, query.getKeyword()));
        }
        if (StringUtils.isNotBlank(query.getSourceType())) {
            wrapper.eq(AdWorkOrder::getSourceType, query.getSourceType());
        }
        if (StringUtils.isNotBlank(query.getPriority())) {
            wrapper.eq(AdWorkOrder::getPriority, query.getPriority());
        }
        if (StringUtils.isNotBlank(query.getStatus())) {
            wrapper.eq(AdWorkOrder::getStatus, query.getStatus());
        }
        if (query.getAssigneeId() != null) {
            wrapper.eq(AdWorkOrder::getAssigneeId, query.getAssigneeId());
        }
        if (query.getRelatedPlanId() != null) {
            wrapper.eq(AdWorkOrder::getRelatedPlanId, query.getRelatedPlanId());
        }
        Page<AdWorkOrder> page = adWorkOrderMapper.selectPage(new Page<AdWorkOrder>(query.getPage(), query.getSize()), wrapper);
        return new PageResult<AdWorkOrder>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public AdWorkOrder getDetail(Long id) {
        AdWorkOrder order = adWorkOrderMapper.selectById(id);
        if (order == null || Integer.valueOf(1).equals(order.getDeleted())) {
            throw new BusinessException(404, "工单不存在");
        }
        return order;
    }

    @Override
    public AdWorkOrder create(AdWorkOrderCreateRequest request) {
        Date now = new Date();
        AdWorkOrder order = new AdWorkOrder();
        order.setWorkNo(nextWorkNo(now));
        order.setSourceType("manual");
        order.setTitle(request.getTitle());
        order.setContent(request.getContent());
        order.setPriority(StringUtils.defaultIfBlank(request.getPriority(), "normal"));
        order.setStatus("open");
        order.setCreatorId(1L);
        order.setAssigneeId(request.getAssigneeId());
        order.setRelatedAdId(request.getRelatedAdId());
        order.setRelatedPlanId(request.getRelatedPlanId());
        order.setCreateBy(1L);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        order.setDeleted(0);
        adWorkOrderMapper.insert(order);
        return order;
    }

    @Override
    public AdWorkOrder createSystemOrder(String title, String content, String priority, Long relatedPlanId) {
        Long count = adWorkOrderMapper.selectCount(new LambdaQueryWrapper<AdWorkOrder>()
                .eq(AdWorkOrder::getSourceType, "delivery")
                .eq(AdWorkOrder::getRelatedPlanId, relatedPlanId)
                .ne(AdWorkOrder::getStatus, "closed"));
        if (count != null && count > 0) {
            return null;
        }
        Date now = new Date();
        AdWorkOrder order = new AdWorkOrder();
        order.setWorkNo(nextWorkNo(now));
        order.setSourceType("delivery");
        order.setTitle(title);
        order.setContent(content);
        order.setPriority(StringUtils.defaultIfBlank(priority, "high"));
        order.setStatus("open");
        order.setCreatorId(0L);
        order.setRelatedPlanId(relatedPlanId);
        order.setCreateBy(0L);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        order.setDeleted(0);
        adWorkOrderMapper.insert(order);
        return order;
    }

    @Override
    public AdWorkOrder assign(Long id, Long assigneeId) {
        if (assigneeId == null) {
            throw new BusinessException("处理人不能为空");
        }
        AdWorkOrder order = getDetail(id);
        if ("closed".equals(order.getStatus())) {
            throw new BusinessException("已关闭工单不能分派");
        }
        order.setAssigneeId(assigneeId);
        order.setStatus("assigned");
        order.setUpdateBy(1L);
        order.setUpdateTime(new Date());
        adWorkOrderMapper.updateById(order);
        return order;
    }

    @Override
    public AdWorkOrder start(Long id) {
        AdWorkOrder order = getDetail(id);
        if ("closed".equals(order.getStatus())) {
            throw new BusinessException("已关闭工单不能处理");
        }
        order.setStatus("processing");
        order.setUpdateBy(1L);
        order.setUpdateTime(new Date());
        adWorkOrderMapper.updateById(order);
        return order;
    }

    @Override
    public AdWorkOrder close(Long id, String content) {
        AdWorkOrder order = getDetail(id);
        order.setStatus("closed");
        order.setCloseTime(new Date());
        if (StringUtils.isNotBlank(content)) {
            order.setContent(StringUtils.defaultString(order.getContent()) + "\n处理结果：" + content);
        }
        order.setUpdateBy(1L);
        order.setUpdateTime(new Date());
        adWorkOrderMapper.updateById(order);
        return order;
    }

    @Override
    public AdWorkOrder reopen(Long id) {
        AdWorkOrder order = getDetail(id);
        if (!"closed".equals(order.getStatus())) {
            throw new BusinessException("只有已关闭工单可以恢复");
        }
        order.setStatus(order.getAssigneeId() == null ? "open" : "assigned");
        order.setCloseTime(null);
        order.setUpdateBy(1L);
        order.setUpdateTime(new Date());
        adWorkOrderMapper.updateById(order);
        return order;
    }

    private String nextWorkNo(Date date) {
        return "WO" + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(date);
    }
}
