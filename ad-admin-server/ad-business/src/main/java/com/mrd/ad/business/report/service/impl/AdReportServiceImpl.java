package com.mrd.ad.business.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mrd.ad.business.device.domain.AdDevice;
import com.mrd.ad.business.device.mapper.AdDeviceMapper;
import com.mrd.ad.business.plan.domain.AdPlan;
import com.mrd.ad.business.plan.domain.AdPlanDevice;
import com.mrd.ad.business.plan.domain.AdPlanMaterial;
import com.mrd.ad.business.plan.mapper.AdPlanDeviceMapper;
import com.mrd.ad.business.plan.mapper.AdPlanMapper;
import com.mrd.ad.business.plan.mapper.AdPlanMaterialMapper;
import com.mrd.ad.business.report.domain.AdPlayLog;
import com.mrd.ad.business.report.dto.AdPlayLogQuery;
import com.mrd.ad.business.report.dto.AdPlayReportRequest;
import com.mrd.ad.business.report.dto.AdPlayReportResult;
import com.mrd.ad.business.report.dto.ReportOverview;
import com.mrd.ad.business.report.mapper.AdPlayLogMapper;
import com.mrd.ad.business.report.service.AdReportService;
import com.mrd.ad.business.system.service.SysDataScopeService;
import com.mrd.ad.common.core.PageResult;
import com.mrd.ad.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdReportServiceImpl implements AdReportService {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    private static final int MESSAGE_MAX_LENGTH = 1000;

    private final AdPlayLogMapper adPlayLogMapper;
    private final AdPlanMapper adPlanMapper;
    private final AdPlanMaterialMapper adPlanMaterialMapper;
    private final AdPlanDeviceMapper adPlanDeviceMapper;
    private final AdDeviceMapper adDeviceMapper;
    private final SysDataScopeService dataScopeService;

    public AdReportServiceImpl(AdPlayLogMapper adPlayLogMapper,
                               AdPlanMapper adPlanMapper,
                               AdPlanMaterialMapper adPlanMaterialMapper,
                               AdPlanDeviceMapper adPlanDeviceMapper,
                               AdDeviceMapper adDeviceMapper,
                               SysDataScopeService dataScopeService) {
        this.adPlayLogMapper = adPlayLogMapper;
        this.adPlanMapper = adPlanMapper;
        this.adPlanMaterialMapper = adPlanMaterialMapper;
        this.adPlanDeviceMapper = adPlanDeviceMapper;
        this.adDeviceMapper = adDeviceMapper;
        this.dataScopeService = dataScopeService;
    }

    @Override
    public PageResult<AdPlayLog> pagePlayLogs(AdPlayLogQuery query) {
        Page<AdPlayLog> page = adPlayLogMapper.selectPage(new Page<AdPlayLog>(query.getPage(), query.getSize()), buildWrapper(query));
        return new PageResult<AdPlayLog>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public ReportOverview overview(AdPlayLogQuery query) {
        List<AdPlayLog> logs = adPlayLogMapper.selectList(buildWrapper(query));
        int playCount = 0;
        int playDuration = 0;
        int completed = 0;
        Set<Long> devices = new HashSet<Long>();
        for (AdPlayLog log : logs) {
            if (!"failed".equals(log.getPlayStatus())) {
                playCount += log.getPlayCount() == null ? 0 : log.getPlayCount();
                playDuration += log.getPlayDuration() == null ? 0 : log.getPlayDuration();
            }
            if (StringUtils.isBlank(log.getPlayStatus()) || "completed".equals(log.getPlayStatus())) {
                completed++;
            }
            if (log.getDeviceId() != null) {
                devices.add(log.getDeviceId());
            }
        }
        ReportOverview overview = new ReportOverview();
        overview.setPlayCount(playCount);
        overview.setExposureCount(playCount * 120);
        overview.setActiveDeviceCount(devices.size());
        overview.setPlayDuration(playDuration);
        overview.setLogCount(logs.size());
        overview.setCompletionRate(logs.isEmpty() ? "0%" : (completed * 100 / logs.size()) + "%");
        return overview;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdPlayReportResult reportDevicePlay(AdPlayReportRequest request) {
        AdPlan plan = adPlanMapper.selectById(request.getPlanId());
        if (plan == null || Integer.valueOf(1).equals(plan.getDeleted())) {
            throw new BusinessException(404, "投放计划不存在");
        }

        Long deviceId = resolveDeviceId(request);
        assertDeviceInPlan(plan.getId(), deviceId);
        if (request.getMaterialId() != null) {
            assertMaterialInPlan(plan.getId(), request.getMaterialId());
        }

        java.util.Date now = resolveReportTime(request);
        String playStatus = normalizePlayStatus(request.getPlayStatus());
        AdPlayLog log = new AdPlayLog();
        log.setAdId(plan.getAdId());
        log.setPlanId(plan.getId());
        log.setMaterialId(request.getMaterialId());
        log.setDeviceId(deviceId);
        log.setPlayDate(new java.sql.Date(now.getTime()));
        log.setPlayStatus(playStatus);
        log.setPlayStartTime(parseDate(request.getPlayStartTime()));
        log.setPlayEndTime(parseDate(request.getPlayEndTime()));
        log.setPlayDuration(resolvePlayDuration(request, playStatus));
        log.setPlayCount(resolvePlayCount(request, playStatus));
        log.setRequestId(StringUtils.trimToNull(request.getRequestId()));
        log.setErrorMessage(limitMessage(request.getErrorMessage()));
        log.setSourceType("device");
        log.setCreateTime(now);
        adPlayLogMapper.insert(log);

        AdPlayReportResult result = new AdPlayReportResult();
        result.setLogId(log.getId());
        result.setPlanId(plan.getId());
        result.setDeviceId(deviceId);
        result.setMaterialId(log.getMaterialId());
        result.setPlayStatus(log.getPlayStatus());
        result.setReportTime(now);
        return result;
    }

    @Override
    public void createDemoPlayLog(Long planId, Long deviceId) {
        java.sql.Date today = new java.sql.Date(System.currentTimeMillis());
        Long count = adPlayLogMapper.selectCount(new LambdaQueryWrapper<AdPlayLog>()
                .eq(AdPlayLog::getPlanId, planId)
                .eq(AdPlayLog::getDeviceId, deviceId)
                .eq(AdPlayLog::getPlayDate, today));
        if (count != null && count > 0) {
            return;
        }

        AdPlan plan = adPlanMapper.selectById(planId);
        if (plan == null) {
            return;
        }
        List<AdPlanMaterial> materials = adPlanMaterialMapper.selectList(
                new LambdaQueryWrapper<AdPlanMaterial>().eq(AdPlanMaterial::getPlanId, planId));
        Long materialId = materials == null || materials.isEmpty() ? null : materials.get(0).getMaterialId();

        int playCount = 80 + (int) ((planId + deviceId) % 180);
        AdPlayLog log = new AdPlayLog();
        log.setAdId(plan.getAdId());
        log.setPlanId(planId);
        log.setMaterialId(materialId);
        log.setDeviceId(deviceId);
        log.setPlayDate(today);
        log.setPlayCount(playCount);
        log.setPlayDuration(playCount * 15);
        log.setPlayStatus("completed");
        log.setPlayStartTime(new java.util.Date());
        log.setPlayEndTime(new java.util.Date());
        log.setSourceType("demo");
        log.setCreateTime(new java.util.Date());
        adPlayLogMapper.insert(log);
    }

    private LambdaQueryWrapper<AdPlayLog> buildWrapper(AdPlayLogQuery query) {
        LambdaQueryWrapper<AdPlayLog> wrapper = new LambdaQueryWrapper<AdPlayLog>()
                .orderByDesc(AdPlayLog::getPlayDate)
                .orderByDesc(AdPlayLog::getCreateTime);
        applyDataScope(wrapper);
        if (query.getAdId() != null) {
            wrapper.eq(AdPlayLog::getAdId, query.getAdId());
        }
        if (query.getPlanId() != null) {
            wrapper.eq(AdPlayLog::getPlanId, query.getPlanId());
        }
        if (query.getDeviceId() != null) {
            wrapper.eq(AdPlayLog::getDeviceId, query.getDeviceId());
        }
        if (StringUtils.isNotBlank(query.getStartDate())) {
            wrapper.ge(AdPlayLog::getPlayDate, java.sql.Date.valueOf(query.getStartDate()));
        }
        if (StringUtils.isNotBlank(query.getEndDate())) {
            wrapper.le(AdPlayLog::getPlayDate, java.sql.Date.valueOf(query.getEndDate()));
        }
        return wrapper;
    }

    private Long resolveDeviceId(AdPlayReportRequest request) {
        if (request.getDeviceId() != null) {
            return request.getDeviceId();
        }
        if (StringUtils.isBlank(request.getMzNumber())) {
            throw new BusinessException("设备不能为空");
        }
        AdDevice device = adDeviceMapper.selectOne(new LambdaQueryWrapper<AdDevice>()
                .eq(AdDevice::getDeviceCode, request.getMzNumber().trim())
                .eq(AdDevice::getDeleted, 0)
                .last("LIMIT 1"));
        if (device == null) {
            throw new BusinessException(404, "设备不存在：" + request.getMzNumber());
        }
        return device.getId();
    }

    private void assertDeviceInPlan(Long planId, Long deviceId) {
        Long count = adPlanDeviceMapper.selectCount(new LambdaQueryWrapper<AdPlanDevice>()
                .eq(AdPlanDevice::getPlanId, planId)
                .eq(AdPlanDevice::getDeviceId, deviceId));
        if (count == null || count <= 0) {
            throw new BusinessException("设备不属于当前投放计划");
        }
    }

    private void assertMaterialInPlan(Long planId, Long materialId) {
        Long count = adPlanMaterialMapper.selectCount(new LambdaQueryWrapper<AdPlanMaterial>()
                .eq(AdPlanMaterial::getPlanId, planId)
                .eq(AdPlanMaterial::getMaterialId, materialId));
        if (count == null || count <= 0) {
            throw new BusinessException("素材不属于当前投放计划");
        }
    }

    private String normalizePlayStatus(String status) {
        String value = StringUtils.defaultIfBlank(status, "completed").trim().toLowerCase();
        if ("start".equals(value) || "started".equals(value) || "playing".equals(value)) {
            return "playing";
        }
        if ("end".equals(value) || "finish".equals(value) || "finished".equals(value)
                || "complete".equals(value) || "completed".equals(value) || "success".equals(value)) {
            return "completed";
        }
        if ("fail".equals(value) || "failed".equals(value) || "error".equals(value)) {
            return "failed";
        }
        if ("pause".equals(value) || "paused".equals(value)) {
            return "paused";
        }
        throw new BusinessException("不支持的播放状态：" + status);
    }

    private java.util.Date resolveReportTime(AdPlayReportRequest request) {
        if (request.getTimestamp() != null && request.getTimestamp() > 0) {
            return new java.util.Date(request.getTimestamp());
        }
        java.util.Date endTime = parseDate(request.getPlayEndTime());
        if (endTime != null) {
            return endTime;
        }
        java.util.Date startTime = parseDate(request.getPlayStartTime());
        return startTime == null ? new java.util.Date() : startTime;
    }

    private java.util.Date parseDate(String value) {
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            SimpleDateFormat format = new SimpleDateFormat(DATE_TIME_PATTERN);
            format.setLenient(false);
            return format.parse(value.trim());
        } catch (ParseException e) {
            throw new BusinessException("播放时间格式不正确，请使用 yyyy-MM-dd HH:mm:ss");
        }
    }

    private Integer resolvePlayDuration(AdPlayReportRequest request, String playStatus) {
        if (request.getPlayDuration() != null) {
            return Math.max(request.getPlayDuration(), 0);
        }
        java.util.Date start = parseDate(request.getPlayStartTime());
        java.util.Date end = parseDate(request.getPlayEndTime());
        if (start != null && end != null && end.after(start)) {
            return (int) ((end.getTime() - start.getTime()) / 1000);
        }
        return "completed".equals(playStatus) ? 0 : 0;
    }

    private Integer resolvePlayCount(AdPlayReportRequest request, String playStatus) {
        if (request.getPlayCount() != null) {
            return Math.max(request.getPlayCount(), 0);
        }
        return "completed".equals(playStatus) ? 1 : 0;
    }

    private String limitMessage(String message) {
        if (StringUtils.isBlank(message)) {
            return null;
        }
        String value = message.trim();
        return value.length() <= MESSAGE_MAX_LENGTH ? value : value.substring(0, MESSAGE_MAX_LENGTH);
    }

    private void applyDataScope(LambdaQueryWrapper<AdPlayLog> wrapper) {
        List<Long> adIds = dataScopeService.scopedAdIds();
        if (adIds == null) {
            return;
        }
        if (adIds.isEmpty()) {
            wrapper.eq(AdPlayLog::getId, -1L);
            return;
        }
        wrapper.in(AdPlayLog::getAdId, adIds);
    }
}
