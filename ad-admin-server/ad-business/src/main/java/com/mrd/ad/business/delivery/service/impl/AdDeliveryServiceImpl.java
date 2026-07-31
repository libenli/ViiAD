package com.mrd.ad.business.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mrd.ad.business.delivery.domain.AdDeliveryRecord;
import com.mrd.ad.business.delivery.dto.AdDeliveryQuery;
import com.mrd.ad.business.delivery.mapper.AdDeliveryRecordMapper;
import com.mrd.ad.business.delivery.service.AdDeliveryService;
import com.mrd.ad.business.device.domain.AdDevice;
import com.mrd.ad.business.device.mapper.AdDeviceMapper;
import com.mrd.ad.business.report.service.AdReportService;
import com.mrd.ad.business.workorder.service.AdWorkOrderService;
import com.mrd.ad.common.core.PageResult;
import com.mrd.ad.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class AdDeliveryServiceImpl implements AdDeliveryService {

    private final AdDeliveryRecordMapper adDeliveryRecordMapper;
    private final AdDeviceMapper adDeviceMapper;
    private final AdReportService adReportService;
    private final AdWorkOrderService adWorkOrderService;

    public AdDeliveryServiceImpl(AdDeliveryRecordMapper adDeliveryRecordMapper,
                                 AdDeviceMapper adDeviceMapper,
                                 AdReportService adReportService,
                                 AdWorkOrderService adWorkOrderService) {
        this.adDeliveryRecordMapper = adDeliveryRecordMapper;
        this.adDeviceMapper = adDeviceMapper;
        this.adReportService = adReportService;
        this.adWorkOrderService = adWorkOrderService;
    }

    @Override
    public PageResult<AdDeliveryRecord> page(AdDeliveryQuery query) {
        LambdaQueryWrapper<AdDeliveryRecord> wrapper = new LambdaQueryWrapper<AdDeliveryRecord>()
                .orderByDesc(AdDeliveryRecord::getDeliveryTime);
        if (query.getPlanId() != null) {
            wrapper.eq(AdDeliveryRecord::getPlanId, query.getPlanId());
        }
        if (query.getDeviceId() != null) {
            wrapper.eq(AdDeliveryRecord::getDeviceId, query.getDeviceId());
        }
        if (StringUtils.isNotBlank(query.getDeliveryType())) {
            wrapper.eq(AdDeliveryRecord::getDeliveryType, query.getDeliveryType());
        }
        if (StringUtils.isNotBlank(query.getDeliveryStatus())) {
            wrapper.eq(AdDeliveryRecord::getDeliveryStatus, query.getDeliveryStatus());
        }
        Page<AdDeliveryRecord> page = adDeliveryRecordMapper.selectPage(new Page<AdDeliveryRecord>(query.getPage(), query.getSize()), wrapper);
        return new PageResult<AdDeliveryRecord>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public AdDeliveryRecord getDetail(Long id) {
        AdDeliveryRecord record = adDeliveryRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException(404, "下发记录不存在");
        }
        return record;
    }

    @Override
    public void createPendingRecords(Long planId, List<Long> deviceIds, String deliveryType) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return;
        }
        Date now = new Date();
        for (Long deviceId : deviceIds) {
            Long count = adDeliveryRecordMapper.selectCount(new LambdaQueryWrapper<AdDeliveryRecord>()
                    .eq(AdDeliveryRecord::getPlanId, planId)
                    .eq(AdDeliveryRecord::getDeviceId, deviceId));
            if (count != null && count > 0) {
                continue;
            }
            AdDeliveryRecord record = new AdDeliveryRecord();
            record.setPlanId(planId);
            record.setDeviceId(deviceId);
            record.setDeliveryType(StringUtils.defaultIfBlank(deliveryType, "auto"));
            record.setDeliveryStatus("pending");
            record.setResponseMsg("等待设备接收投放计划");
            record.setRetryCount(0);
            record.setDeliveryTime(now);
            adDeliveryRecordMapper.insert(record);
        }
    }

    @Override
    public void dispatchPlan(Long planId, List<Long> deviceIds, String deliveryType) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return;
        }
        createPendingRecords(planId, deviceIds, deliveryType);
        List<AdDeliveryRecord> records = adDeliveryRecordMapper.selectList(new LambdaQueryWrapper<AdDeliveryRecord>()
                .eq(AdDeliveryRecord::getPlanId, planId)
                .in(AdDeliveryRecord::getDeviceId, deviceIds)
                .eq(AdDeliveryRecord::getDeliveryStatus, "pending"));
        for (AdDeliveryRecord record : records) {
            AdDevice device = adDeviceMapper.selectById(record.getDeviceId());
            if (device == null || Integer.valueOf(1).equals(device.getDeleted())) {
                markFailed(record.getId(), "设备不存在或已删除");
                continue;
            }
            if (!"active".equals(device.getStatus()) || "fault".equals(device.getFaultStatus())) {
                markFailed(record.getId(), "设备不可投放：" + device.getDeviceName());
                continue;
            }
            if ("online".equals(device.getOnlineStatus())) {
                markSuccess(record.getId(), "自动下发成功：设备在线并已接收投放计划");
            } else {
                markFailed(record.getId(), "自动下发失败：设备离线，等待运维处理");
            }
        }
    }

    @Override
    public AdDeliveryRecord markSuccess(Long id, String responseMsg) {
        AdDeliveryRecord record = getDetail(id);
        record.setDeliveryStatus("success");
        record.setResponseMsg(StringUtils.defaultIfBlank(responseMsg, "设备已确认接收计划"));
        record.setDeliveryTime(new Date());
        adDeliveryRecordMapper.updateById(record);
        adReportService.createDemoPlayLog(record.getPlanId(), record.getDeviceId());
        return record;
    }

    @Override
    public AdDeliveryRecord markFailed(Long id, String responseMsg) {
        AdDeliveryRecord record = getDetail(id);
        record.setDeliveryStatus("failed");
        record.setResponseMsg(StringUtils.defaultIfBlank(responseMsg, "设备下发失败"));
        record.setDeliveryTime(new Date());
        adDeliveryRecordMapper.updateById(record);
        adWorkOrderService.createSystemOrder(
                "投放计划下发失败",
                "计划ID：" + record.getPlanId() + "，设备ID：" + record.getDeviceId() + "，原因：" + record.getResponseMsg(),
                "high",
                record.getPlanId());
        return record;
    }

    @Override
    public AdDeliveryRecord retry(Long id) {
        AdDeliveryRecord record = getDetail(id);
        if (!"failed".equals(record.getDeliveryStatus())) {
            throw new BusinessException("只有失败记录可以重试");
        }
        record.setDeliveryStatus("pending");
        record.setResponseMsg("已重新加入下发队列");
        record.setRetryCount(record.getRetryCount() == null ? 1 : record.getRetryCount() + 1);
        record.setDeliveryTime(new Date());
        adDeliveryRecordMapper.updateById(record);
        return record;
    }
}
