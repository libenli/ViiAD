package com.mrd.ad.business.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mrd.ad.business.device.domain.AdDevice;
import com.mrd.ad.business.device.dto.AdDeviceCreateRequest;
import com.mrd.ad.business.device.dto.AdDeviceQuery;
import com.mrd.ad.business.device.dto.AdDeviceUpdateRequest;
import com.mrd.ad.business.device.mapper.AdDeviceMapper;
import com.mrd.ad.business.device.service.AdDeviceService;
import com.mrd.ad.common.core.PageResult;
import com.mrd.ad.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class AdDeviceServiceImpl implements AdDeviceService {

    private final AdDeviceMapper adDeviceMapper;

    public AdDeviceServiceImpl(AdDeviceMapper adDeviceMapper) {
        this.adDeviceMapper = adDeviceMapper;
    }

    @Override
    public PageResult<AdDevice> page(AdDeviceQuery query) {
        LambdaQueryWrapper<AdDevice> wrapper = new LambdaQueryWrapper<AdDevice>()
                .eq(AdDevice::getDeleted, 0)
                .orderByDesc(AdDevice::getCreateTime);

        if (StringUtils.isNotBlank(query.getKeyword())) {
            wrapper.and(item -> item
                    .like(AdDevice::getDeviceName, query.getKeyword())
                    .or()
                    .like(AdDevice::getDeviceCode, query.getKeyword())
                    .or()
                    .like(AdDevice::getIpAddress, query.getKeyword()));
        }
        if (query.getBuildingId() != null) {
            wrapper.eq(AdDevice::getBuildingId, query.getBuildingId());
        }
        if (StringUtils.isNotBlank(query.getOnlineStatus())) {
            wrapper.eq(AdDevice::getOnlineStatus, query.getOnlineStatus());
        }
        if (StringUtils.isNotBlank(query.getFaultStatus())) {
            wrapper.eq(AdDevice::getFaultStatus, query.getFaultStatus());
        }
        if (StringUtils.isNotBlank(query.getStatus())) {
            wrapper.eq(AdDevice::getStatus, query.getStatus());
        }
        if (query.getCurrentPlanId() != null) {
            wrapper.eq(AdDevice::getCurrentPlanId, query.getCurrentPlanId());
        }

        Page<AdDevice> page = adDeviceMapper.selectPage(new Page<AdDevice>(query.getPage(), query.getSize()), wrapper);
        return new PageResult<AdDevice>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public AdDevice getDetail(Long id) {
        AdDevice device = adDeviceMapper.selectById(id);
        if (device == null || Integer.valueOf(1).equals(device.getDeleted())) {
            throw new BusinessException(404, "设备不存在");
        }
        return device;
    }

    @Override
    public AdDevice create(AdDeviceCreateRequest request) {
        Date now = new Date();
        AdDevice device = new AdDevice();
        copyRequest(request, device);
        device.setDeviceCode(nextDeviceCode(now));
        device.setOnlineStatus("offline");
        device.setFaultStatus("normal");
        device.setStatus("active");
        device.setCreateBy(1L);
        device.setCreateTime(now);
        device.setUpdateTime(now);
        device.setDeleted(0);
        adDeviceMapper.insert(device);
        return device;
    }

    @Override
    public AdDevice update(Long id, AdDeviceUpdateRequest request) {
        AdDevice device = getDetail(id);
        copyRequest(request, device);
        device.setUpdateBy(1L);
        device.setUpdateTime(new Date());
        adDeviceMapper.updateById(device);
        return device;
    }

    @Override
    public AdDevice setOnline(Long id) {
        AdDevice device = getDetail(id);
        if (!"active".equals(device.getStatus())) {
            throw new BusinessException("停用设备不能上线");
        }
        device.setOnlineStatus("online");
        device.setLastOnlineTime(new Date());
        return updateStatus(device);
    }

    @Override
    public AdDevice setOffline(Long id) {
        AdDevice device = getDetail(id);
        device.setOnlineStatus("offline");
        return updateStatus(device);
    }

    @Override
    public AdDevice markFault(Long id) {
        AdDevice device = getDetail(id);
        device.setFaultStatus("fault");
        device.setOnlineStatus("offline");
        return updateStatus(device);
    }

    @Override
    public AdDevice repair(Long id) {
        AdDevice device = getDetail(id);
        device.setFaultStatus("normal");
        return updateStatus(device);
    }

    @Override
    public AdDevice enable(Long id) {
        AdDevice device = getDetail(id);
        device.setStatus("active");
        return updateStatus(device);
    }

    @Override
    public AdDevice disable(Long id) {
        AdDevice device = getDetail(id);
        device.setStatus("disabled");
        device.setOnlineStatus("offline");
        return updateStatus(device);
    }

    private AdDevice updateStatus(AdDevice device) {
        device.setUpdateBy(1L);
        device.setUpdateTime(new Date());
        adDeviceMapper.updateById(device);
        return device;
    }

    private void copyRequest(AdDeviceCreateRequest request, AdDevice device) {
        device.setDeviceName(request.getDeviceName());
        device.setBuildingId(request.getBuildingId());
        device.setFloorNo(request.getFloorNo());
        device.setScreenSize(request.getScreenSize());
        device.setResolution(request.getResolution());
        device.setIpAddress(request.getIpAddress());
        device.setMacAddress(request.getMacAddress());
    }

    private String nextDeviceCode(Date date) {
        return "DEV" + new SimpleDateFormat("yyyyMMddHHmmssSSS").format(date);
    }
}
