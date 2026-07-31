package com.mrd.ad.business.report.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mrd.ad.business.plan.domain.AdPlan;
import com.mrd.ad.business.plan.domain.AdPlanMaterial;
import com.mrd.ad.business.plan.mapper.AdPlanMapper;
import com.mrd.ad.business.plan.mapper.AdPlanMaterialMapper;
import com.mrd.ad.business.report.domain.AdPlayLog;
import com.mrd.ad.business.report.dto.AdPlayLogQuery;
import com.mrd.ad.business.report.dto.ReportOverview;
import com.mrd.ad.business.report.mapper.AdPlayLogMapper;
import com.mrd.ad.business.report.service.AdReportService;
import com.mrd.ad.business.system.service.SysDataScopeService;
import com.mrd.ad.common.core.PageResult;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdReportServiceImpl implements AdReportService {

    private final AdPlayLogMapper adPlayLogMapper;
    private final AdPlanMapper adPlanMapper;
    private final AdPlanMaterialMapper adPlanMaterialMapper;
    private final SysDataScopeService dataScopeService;

    public AdReportServiceImpl(AdPlayLogMapper adPlayLogMapper,
                               AdPlanMapper adPlanMapper,
                               AdPlanMaterialMapper adPlanMaterialMapper,
                               SysDataScopeService dataScopeService) {
        this.adPlayLogMapper = adPlayLogMapper;
        this.adPlanMapper = adPlanMapper;
        this.adPlanMaterialMapper = adPlanMaterialMapper;
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
        Set<Long> devices = new HashSet<Long>();
        for (AdPlayLog log : logs) {
            playCount += log.getPlayCount() == null ? 0 : log.getPlayCount();
            playDuration += log.getPlayDuration() == null ? 0 : log.getPlayDuration();
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
        overview.setCompletionRate(logs.isEmpty() ? "0%" : "100%");
        return overview;
    }

    @Override
    public void createDemoPlayLog(Long planId, Long deviceId) {
        Date today = new Date(System.currentTimeMillis());
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
            wrapper.ge(AdPlayLog::getPlayDate, Date.valueOf(query.getStartDate()));
        }
        if (StringUtils.isNotBlank(query.getEndDate())) {
            wrapper.le(AdPlayLog::getPlayDate, Date.valueOf(query.getEndDate()));
        }
        return wrapper;
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
