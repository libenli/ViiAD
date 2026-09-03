package com.mrd.ad.business.delivery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mrd.ad.business.delivery.domain.AdDeliveryRecord;
import com.mrd.ad.business.delivery.dto.AdDeliveryQuery;
import com.mrd.ad.business.delivery.mapper.AdDeliveryRecordMapper;
import com.mrd.ad.business.delivery.service.AdDeliveryService;
import com.mrd.ad.business.device.domain.AdDevice;
import com.mrd.ad.business.device.dto.ViitalkDeviceCommandRequest;
import com.mrd.ad.business.device.dto.ViitalkDeviceCommandResult;
import com.mrd.ad.business.device.mapper.AdDeviceMapper;
import com.mrd.ad.business.device.service.ViitalkDeviceOnlineService;
import com.mrd.ad.business.material.domain.AdMaterial;
import com.mrd.ad.business.material.mapper.AdMaterialMapper;
import com.mrd.ad.business.plan.domain.AdPlan;
import com.mrd.ad.business.plan.domain.AdPlanMaterial;
import com.mrd.ad.business.plan.mapper.AdPlanMapper;
import com.mrd.ad.business.plan.mapper.AdPlanMaterialMapper;
import com.mrd.ad.business.report.service.AdReportService;
import com.mrd.ad.business.workorder.service.AdWorkOrderService;
import com.mrd.ad.common.core.PageResult;
import com.mrd.ad.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdDeliveryServiceImpl implements AdDeliveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdDeliveryServiceImpl.class);
    private static final String COMMAND_AD_PLAN_PUBLISH = "ad_plan_publish";
    private static final String COMMAND_AD_PLAN_STOP = "ad_plan_stop";
    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final AdDeliveryRecordMapper adDeliveryRecordMapper;
    private final AdDeviceMapper adDeviceMapper;
    private final AdPlanMapper adPlanMapper;
    private final AdPlanMaterialMapper adPlanMaterialMapper;
    private final AdMaterialMapper adMaterialMapper;
    private final AdReportService adReportService;
    private final AdWorkOrderService adWorkOrderService;
    private final ViitalkDeviceOnlineService viitalkDeviceOnlineService;

    public AdDeliveryServiceImpl(AdDeliveryRecordMapper adDeliveryRecordMapper,
                                 AdDeviceMapper adDeviceMapper,
                                 AdPlanMapper adPlanMapper,
                                 AdPlanMaterialMapper adPlanMaterialMapper,
                                 AdMaterialMapper adMaterialMapper,
                                 AdReportService adReportService,
                                 AdWorkOrderService adWorkOrderService,
                                 ViitalkDeviceOnlineService viitalkDeviceOnlineService) {
        this.adDeliveryRecordMapper = adDeliveryRecordMapper;
        this.adDeviceMapper = adDeviceMapper;
        this.adPlanMapper = adPlanMapper;
        this.adPlanMaterialMapper = adPlanMaterialMapper;
        this.adMaterialMapper = adMaterialMapper;
        this.adReportService = adReportService;
        this.adWorkOrderService = adWorkOrderService;
        this.viitalkDeviceOnlineService = viitalkDeviceOnlineService;
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
        AdPlan plan = adPlanMapper.selectById(planId);
        if (plan == null) {
            throw new BusinessException(404, "投放计划不存在");
        }

        createPendingRecords(planId, deviceIds, deliveryType);
        List<AdDeliveryRecord> records = adDeliveryRecordMapper.selectList(new LambdaQueryWrapper<AdDeliveryRecord>()
                .eq(AdDeliveryRecord::getPlanId, planId)
                .in(AdDeliveryRecord::getDeviceId, deviceIds)
                .eq(AdDeliveryRecord::getDeliveryStatus, "pending"));
        for (AdDeliveryRecord record : records) {
            AdDevice device = adDeviceMapper.selectById(record.getDeviceId());
            if (!canDeliver(record, device)) {
                continue;
            }
            try {
                ViitalkDeviceCommandResult result = sendDeviceCommand(device, COMMAND_AD_PLAN_PUBLISH, buildPublishPayload(plan, device));
                record.setDeliveryStatus("pending");
                record.setResponseMsg("已下发到设备，等待ACK，requestId=" + result.getRequestId());
                record.setDeliveryTime(new Date());
                adDeliveryRecordMapper.updateById(record);
            } catch (Exception e) {
                markFailed(record.getId(), "自动下发失败：" + e.getMessage());
            }
        }
    }

    @Override
    public void stopPlan(Long planId, List<Long> deviceIds, String deliveryType) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return;
        }
        AdPlan plan = adPlanMapper.selectById(planId);
        if (plan == null) {
            LOGGER.warn("停止投放计划失败，计划不存在，planId={}", planId);
            return;
        }
        for (Long deviceId : deviceIds) {
            AdDevice device = adDeviceMapper.selectById(deviceId);
            if (device == null || Integer.valueOf(1).equals(device.getDeleted())) {
                LOGGER.warn("停止投放计划失败，设备不存在或已删除，planId={}, deviceId={}", planId, deviceId);
                continue;
            }
            if (StringUtils.isBlank(device.getDeviceCode())) {
                LOGGER.warn("停止投放计划失败，设备编码为空，planId={}, deviceId={}", planId, deviceId);
                continue;
            }
            try {
                ViitalkDeviceCommandResult result = sendDeviceCommand(device, COMMAND_AD_PLAN_STOP, buildStopPayload(plan, deliveryType));
                LOGGER.info("已下发停止投放计划指令，planId={}, deviceId={}, mzNumber={}, requestId={}",
                        planId, deviceId, device.getDeviceCode(), result.getRequestId());
            } catch (Exception e) {
                LOGGER.warn("停止投放计划指令下发失败，planId={}, deviceId={}, mzNumber={}, error={}",
                        planId, deviceId, device.getDeviceCode(), e.getMessage());
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
        dispatchPlan(record.getPlanId(), java.util.Collections.singletonList(record.getDeviceId()), record.getDeliveryType());
        return getDetail(id);
    }

    private boolean canDeliver(AdDeliveryRecord record, AdDevice device) {
        if (device == null || Integer.valueOf(1).equals(device.getDeleted())) {
            markFailed(record.getId(), "设备不存在或已删除");
            return false;
        }
        if (!"active".equals(device.getStatus()) || "fault".equals(device.getFaultStatus())) {
            markFailed(record.getId(), "设备不可投放：" + device.getDeviceName());
            return false;
        }
        if (StringUtils.isBlank(device.getDeviceCode())) {
            markFailed(record.getId(), "设备编码为空，无法匹配ViiTalk大屏账号");
            return false;
        }
        return true;
    }

    private ViitalkDeviceCommandResult sendDeviceCommand(AdDevice device, String command, Map<String, Object> payload) {
        ViitalkDeviceCommandRequest request = new ViitalkDeviceCommandRequest();
        request.setMzNumber(device.getDeviceCode());
        request.setCommand(command);
        request.setPayload(payload);
        return viitalkDeviceOnlineService.sendCommand(request);
    }

    private Map<String, Object> buildPublishPayload(AdPlan plan, AdDevice device) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("plan", toPlanPayload(plan));
        payload.put("device", toDevicePayload(device));
        payload.put("materials", toMaterialPayload(plan.getId()));
        return payload;
    }

    private Map<String, Object> buildStopPayload(AdPlan plan, String reason) {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("planId", plan.getId());
        payload.put("planCode", plan.getPlanCode());
        payload.put("planName", plan.getPlanName());
        payload.put("reason", StringUtils.defaultIfBlank(reason, "stop"));
        return payload;
    }

    private Map<String, Object> toPlanPayload(AdPlan plan) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("planId", plan.getId());
        data.put("planCode", plan.getPlanCode());
        data.put("planName", plan.getPlanName());
        data.put("adId", plan.getAdId());
        data.put("regionCode", plan.getRegionCode());
        data.put("startTime", formatDate(plan.getStartTime()));
        data.put("endTime", formatDate(plan.getEndTime()));
        data.put("scheduleStatus", plan.getScheduleStatus());
        data.put("deliveryStatus", plan.getDeliveryStatus());
        return data;
    }

    private Map<String, Object> toDevicePayload(AdDevice device) {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("deviceId", device.getId());
        data.put("deviceCode", device.getDeviceCode());
        data.put("deviceName", device.getDeviceName());
        data.put("screenSize", device.getScreenSize());
        data.put("resolution", device.getResolution());
        return data;
    }

    private List<Map<String, Object>> toMaterialPayload(Long planId) {
        List<Map<String, Object>> materials = new ArrayList<Map<String, Object>>();
        List<AdPlanMaterial> relations = adPlanMaterialMapper.selectList(
                new LambdaQueryWrapper<AdPlanMaterial>().eq(AdPlanMaterial::getPlanId, planId));
        if (relations == null) {
            return materials;
        }
        for (AdPlanMaterial relation : relations) {
            AdMaterial material = adMaterialMapper.selectById(relation.getMaterialId());
            if (material == null || Integer.valueOf(1).equals(material.getDeleted())) {
                continue;
            }
            Map<String, Object> data = new LinkedHashMap<String, Object>();
            data.put("materialId", material.getId());
            data.put("materialCode", material.getMaterialCode());
            data.put("materialName", material.getMaterialName());
            data.put("materialType", material.getMaterialType());
            data.put("fileUrl", material.getFileUrl());
            data.put("fileSize", material.getFileSize());
            data.put("durationSeconds", material.getDurationSeconds());
            data.put("width", material.getWidth());
            data.put("height", material.getHeight());
            data.put("coverUrl", material.getCoverUrl());
            materials.add(data);
        }
        return materials;
    }

    private String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(DATE_PATTERN).format(date);
    }
}
