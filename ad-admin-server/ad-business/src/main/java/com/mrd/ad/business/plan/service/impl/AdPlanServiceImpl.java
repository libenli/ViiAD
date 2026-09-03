package com.mrd.ad.business.plan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mrd.ad.business.ad.domain.AdOrder;
import com.mrd.ad.business.ad.mapper.AdOrderMapper;
import com.mrd.ad.business.delivery.service.AdDeliveryService;
import com.mrd.ad.business.device.domain.AdDevice;
import com.mrd.ad.business.device.mapper.AdDeviceMapper;
import com.mrd.ad.business.material.domain.AdMaterial;
import com.mrd.ad.business.material.mapper.AdMaterialMapper;
import com.mrd.ad.business.plan.domain.AdPlan;
import com.mrd.ad.business.plan.domain.AdPlanDevice;
import com.mrd.ad.business.plan.domain.AdPlanMaterial;
import com.mrd.ad.business.plan.dto.AdPlanCreateRequest;
import com.mrd.ad.business.plan.dto.AdPlanQuery;
import com.mrd.ad.business.plan.dto.AdPlanUpdateRequest;
import com.mrd.ad.business.plan.mapper.AdPlanDeviceMapper;
import com.mrd.ad.business.plan.mapper.AdPlanMapper;
import com.mrd.ad.business.plan.mapper.AdPlanMaterialMapper;
import com.mrd.ad.business.plan.service.AdPlanService;
import com.mrd.ad.business.system.service.SysDataScopeService;
import com.mrd.ad.common.core.PageResult;
import com.mrd.ad.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdPlanServiceImpl implements AdPlanService {

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm:ss";

    private final AdPlanMapper adPlanMapper;
    private final AdPlanMaterialMapper adPlanMaterialMapper;
    private final AdPlanDeviceMapper adPlanDeviceMapper;
    private final AdOrderMapper adOrderMapper;
    private final AdMaterialMapper adMaterialMapper;
    private final AdDeviceMapper adDeviceMapper;
    private final AdDeliveryService adDeliveryService;
    private final SysDataScopeService dataScopeService;

    public AdPlanServiceImpl(AdPlanMapper adPlanMapper,
                             AdPlanMaterialMapper adPlanMaterialMapper,
                             AdPlanDeviceMapper adPlanDeviceMapper,
                             AdOrderMapper adOrderMapper,
                             AdMaterialMapper adMaterialMapper,
                             AdDeviceMapper adDeviceMapper,
                             AdDeliveryService adDeliveryService,
                             SysDataScopeService dataScopeService) {
        this.adPlanMapper = adPlanMapper;
        this.adPlanMaterialMapper = adPlanMaterialMapper;
        this.adPlanDeviceMapper = adPlanDeviceMapper;
        this.adOrderMapper = adOrderMapper;
        this.adMaterialMapper = adMaterialMapper;
        this.adDeviceMapper = adDeviceMapper;
        this.adDeliveryService = adDeliveryService;
        this.dataScopeService = dataScopeService;
    }

    @Override
    public PageResult<AdPlan> page(AdPlanQuery query) {
        LambdaQueryWrapper<AdPlan> wrapper = new LambdaQueryWrapper<AdPlan>()
                .eq(AdPlan::getDeleted, 0)
                .orderByDesc(AdPlan::getCreateTime);
        applyDataScope(wrapper);

        if (StringUtils.isNotBlank(query.getKeyword())) {
            wrapper.and(item -> item
                    .like(AdPlan::getPlanName, query.getKeyword())
                    .or()
                    .like(AdPlan::getPlanCode, query.getKeyword()));
        }
        if (query.getAdId() != null) {
            wrapper.eq(AdPlan::getAdId, query.getAdId());
        }
        if (StringUtils.isNotBlank(query.getRegionCode())) {
            wrapper.like(AdPlan::getRegionCode, query.getRegionCode());
        }
        if (StringUtils.isNotBlank(query.getScheduleStatus())) {
            wrapper.eq(AdPlan::getScheduleStatus, query.getScheduleStatus());
        }
        if (StringUtils.isNotBlank(query.getDeliveryStatus())) {
            wrapper.eq(AdPlan::getDeliveryStatus, query.getDeliveryStatus());
        }

        Page<AdPlan> page = adPlanMapper.selectPage(new Page<AdPlan>(query.getPage(), query.getSize()), wrapper);
        return new PageResult<AdPlan>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public AdPlan getDetail(Long id) {
        AdPlan plan = adPlanMapper.selectById(id);
        if (plan == null || Integer.valueOf(1).equals(plan.getDeleted())) {
            throw new BusinessException(404, "投放计划不存在");
        }
        dataScopeService.assertAdIdVisible(plan.getAdId());
        fillRelations(plan);
        return plan;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdPlan create(AdPlanCreateRequest request) {
        validatePlanRequest(request, true);

        Date now = new Date();
        AdPlan plan = new AdPlan();
        copyRequest(request, plan);
        plan.setPlanCode(nextPlanCode(now));
        plan.setScheduleStatus("draft");
        plan.setDeliveryStatus("not_started");
        plan.setCreateUserId(dataScopeService.currentUserId());
        plan.setCreateBy(dataScopeService.currentUserId());
        plan.setCreateTime(now);
        plan.setUpdateTime(now);
        plan.setDeleted(0);
        adPlanMapper.insert(plan);
        saveRelations(plan.getId(), request.getMaterialIds(), request.getDeviceIds());
        fillRelations(plan);
        return plan;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdPlan update(Long id, AdPlanUpdateRequest request) {
        AdPlan plan = getDetail(id);
        if (!canEditPlan(plan)) {
            throw new BusinessException("只有草稿或已排期且未投放的计划可以编辑");
        }
        validatePlanRequest(request, false);
        copyRequest(request, plan);
        plan.setUpdateBy(dataScopeService.currentUserId());
        plan.setUpdateTime(new Date());
        adPlanMapper.updateById(plan);
        saveRelations(plan.getId(), request.getMaterialIds(), request.getDeviceIds());
        fillRelations(plan);
        return plan;
    }

    @Override
    public AdPlan schedule(Long id) {
        AdPlan plan = getDetail(id);
        if (!"draft".equals(plan.getScheduleStatus())) {
            throw new BusinessException("只有草稿计划可以提交排期");
        }
        plan.setScheduleStatus("scheduled");
        plan.setDeliveryStatus("not_started");
        plan.setUpdateBy(dataScopeService.currentUserId());
        plan.setUpdateTime(new Date());
        adPlanMapper.updateById(plan);
        return getDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdPlan start(Long id) {
        AdPlan plan = getDetail(id);
        if (!"scheduled".equals(plan.getScheduleStatus())) {
            throw new BusinessException("计划排期后才能启动投放");
        }
        if ("finished".equals(plan.getDeliveryStatus())) {
            throw new BusinessException("已结束计划不能重新启动");
        }
        plan.setDeliveryStatus("live");
        plan.setUpdateBy(dataScopeService.currentUserId());
        plan.setUpdateTime(new Date());
        adPlanMapper.updateById(plan);
        updateCurrentPlanForDevices(plan);
        adDeliveryService.dispatchPlan(plan.getId(), plan.getDeviceIds(), "auto");
        return getDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdPlan pause(Long id) {
        AdPlan plan = getDetail(id);
        if (!"live".equals(plan.getDeliveryStatus())) {
            throw new BusinessException("只有投放中的计划可以暂停");
        }
        plan.setDeliveryStatus("paused");
        plan.setUpdateBy(dataScopeService.currentUserId());
        plan.setUpdateTime(new Date());
        adPlanMapper.updateById(plan);
        adDeliveryService.stopPlan(plan.getId(), plan.getDeviceIds(), "pause");
        releaseCurrentPlanForDevices(plan);
        return getDetail(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdPlan finish(Long id) {
        AdPlan plan = getDetail(id);
        if ("finished".equals(plan.getDeliveryStatus())) {
            throw new BusinessException("计划已结束");
        }
        plan.setDeliveryStatus("finished");
        plan.setUpdateBy(dataScopeService.currentUserId());
        plan.setUpdateTime(new Date());
        adPlanMapper.updateById(plan);
        adDeliveryService.stopPlan(plan.getId(), plan.getDeviceIds(), "finish");
        releaseCurrentPlanForDevices(plan);
        return getDetail(id);
    }

    private void validatePlanRequest(AdPlanCreateRequest request, boolean requireStartNotPast) {
        AdOrder adOrder = adOrderMapper.selectById(request.getAdId());
        if (adOrder == null || Integer.valueOf(1).equals(adOrder.getDeleted())) {
            throw new BusinessException("关联广告不存在");
        }
        dataScopeService.assertAdVisible(adOrder);
        if (!"approved".equals(adOrder.getStatus())) {
            throw new BusinessException("广告审核通过后才能创建投放计划");
        }
        Date start = parseDate(request.getStartTime(), "开始时间格式不正确");
        Date end = parseDate(request.getEndTime(), "结束时间格式不正确");
        if (requireStartNotPast && start.before(new Date())) {
            throw new BusinessException("开始时间不能早于当前时间");
        }
        if (!end.after(start)) {
            throw new BusinessException("结束时间必须晚于开始时间");
        }
        for (Long materialId : request.getMaterialIds()) {
            AdMaterial material = adMaterialMapper.selectById(materialId);
            if (material == null || Integer.valueOf(1).equals(material.getDeleted())) {
                throw new BusinessException("关联素材不存在：" + materialId);
            }
            if (!"approved".equals(material.getStatus())) {
                throw new BusinessException("只能选择审核通过的素材：" + material.getMaterialName());
            }
            if (!request.getAdId().equals(material.getAdId())) {
                throw new BusinessException("素材必须属于当前广告：" + material.getMaterialName());
            }
        }
        for (Long deviceId : request.getDeviceIds()) {
            AdDevice device = adDeviceMapper.selectById(deviceId);
            if (device == null || Integer.valueOf(1).equals(device.getDeleted())) {
                throw new BusinessException("关联设备不存在：" + deviceId);
            }
            if (!"active".equals(device.getStatus())) {
                throw new BusinessException("只能选择启用中的设备：" + device.getDeviceName());
            }
            if ("fault".equals(device.getFaultStatus())) {
                throw new BusinessException("故障设备不能加入投放计划：" + device.getDeviceName());
            }
        }
    }

    private void copyRequest(AdPlanCreateRequest request, AdPlan plan) {
        plan.setPlanName(StringUtils.trim(request.getPlanName()));
        plan.setAdId(request.getAdId());
        plan.setRegionCode(StringUtils.trimToNull(request.getRegionCode()));
        plan.setStartTime(parseDate(request.getStartTime(), "开始时间格式不正确"));
        plan.setEndTime(parseDate(request.getEndTime(), "结束时间格式不正确"));
        plan.setOperatorId(request.getOperatorId());
    }

    private Date parseDate(String value, String message) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN);
            format.setLenient(false);
            return format.parse(value);
        } catch (ParseException e) {
            throw new BusinessException(message);
        }
    }

    private boolean canEditPlan(AdPlan plan) {
        return ("draft".equals(plan.getScheduleStatus()) || "scheduled".equals(plan.getScheduleStatus()))
                && !"live".equals(plan.getDeliveryStatus())
                && !"finished".equals(plan.getDeliveryStatus());
    }

    private void saveRelations(Long planId, List<Long> materialIds, List<Long> deviceIds) {
        adPlanMaterialMapper.delete(new LambdaQueryWrapper<AdPlanMaterial>().eq(AdPlanMaterial::getPlanId, planId));
        adPlanDeviceMapper.delete(new LambdaQueryWrapper<AdPlanDevice>().eq(AdPlanDevice::getPlanId, planId));

        for (Long materialId : materialIds) {
            AdPlanMaterial relation = new AdPlanMaterial();
            relation.setPlanId(planId);
            relation.setMaterialId(materialId);
            adPlanMaterialMapper.insert(relation);
        }
        for (Long deviceId : deviceIds) {
            AdPlanDevice relation = new AdPlanDevice();
            relation.setPlanId(planId);
            relation.setDeviceId(deviceId);
            adPlanDeviceMapper.insert(relation);
        }
    }

    private void fillRelations(AdPlan plan) {
        if (plan == null || plan.getId() == null) {
            return;
        }
        List<AdPlanMaterial> materials = adPlanMaterialMapper.selectList(
                new LambdaQueryWrapper<AdPlanMaterial>().eq(AdPlanMaterial::getPlanId, plan.getId()));
        List<AdPlanDevice> devices = adPlanDeviceMapper.selectList(
                new LambdaQueryWrapper<AdPlanDevice>().eq(AdPlanDevice::getPlanId, plan.getId()));
        plan.setMaterialIds(materials == null ? Collections.<Long>emptyList() : materials.stream()
                .map(AdPlanMaterial::getMaterialId)
                .collect(Collectors.toList()));
        plan.setDeviceIds(devices == null ? Collections.<Long>emptyList() : devices.stream()
                .map(AdPlanDevice::getDeviceId)
                .collect(Collectors.toList()));
    }

    private void updateCurrentPlanForDevices(AdPlan plan) {
        if (plan.getDeviceIds() == null) {
            return;
        }
        for (Long deviceId : plan.getDeviceIds()) {
            AdDevice device = adDeviceMapper.selectById(deviceId);
            if (device == null || Integer.valueOf(1).equals(device.getDeleted())) {
                throw new BusinessException("关联设备不存在：" + deviceId);
            }
            if (!"active".equals(device.getStatus()) || "fault".equals(device.getFaultStatus())) {
                throw new BusinessException("设备不可投放：" + device.getDeviceName());
            }
            device.setCurrentPlanId(plan.getId());
            device.setUpdateBy(dataScopeService.currentUserId());
            device.setUpdateTime(new Date());
            adDeviceMapper.updateById(device);
        }
    }

    private void releaseCurrentPlanForDevices(AdPlan plan) {
        if (plan.getDeviceIds() == null) {
            return;
        }
        for (Long deviceId : plan.getDeviceIds()) {
            AdDevice device = adDeviceMapper.selectById(deviceId);
            if (device == null || Integer.valueOf(1).equals(device.getDeleted())) {
                continue;
            }
            if (!plan.getId().equals(device.getCurrentPlanId())) {
                continue;
            }
            device.setCurrentPlanId(null);
            device.setUpdateBy(dataScopeService.currentUserId());
            device.setUpdateTime(new Date());
            adDeviceMapper.updateById(device);
        }
    }

    private String nextPlanCode(Date date) {
        return "PLAN" + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(date);
    }

    private void applyDataScope(LambdaQueryWrapper<AdPlan> wrapper) {
        List<Long> adIds = dataScopeService.scopedAdIds();
        if (adIds == null) {
            return;
        }
        if (adIds.isEmpty()) {
            wrapper.eq(AdPlan::getId, -1L);
            return;
        }
        wrapper.in(AdPlan::getAdId, adIds);
    }
}

