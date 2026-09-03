package com.mrd.ad.business.device.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mrd.ad.business.device.domain.AdDevice;
import com.mrd.ad.business.device.dto.AdDeviceCreateRequest;
import com.mrd.ad.business.device.dto.AdDeviceQuery;
import com.mrd.ad.business.device.dto.AdDeviceUpdateRequest;
import com.mrd.ad.business.device.dto.DeviceIpLocation;
import com.mrd.ad.business.device.mapper.AdDeviceMapper;
import com.mrd.ad.business.device.mq.ViiAdMqProperties;
import com.mrd.ad.business.device.service.AdDeviceService;
import com.mrd.ad.business.device.service.DeviceIpLocationService;
import com.mrd.ad.common.core.PageResult;
import com.mrd.ad.common.exception.BusinessException;
import com.mysher.platform.user.ViiUser;
import com.mysher.platform.viiUser.ViiUserService;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class AdDeviceServiceImpl implements AdDeviceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdDeviceServiceImpl.class);
    private static final String ONLINE = "online";
    private static final String OFFLINE = "offline";
    private static final String NORMAL = "normal";
    private static final String ACTIVE = "active";
    private static final String CHINA = "中国";

    private final AdDeviceMapper adDeviceMapper;
    private final DeviceIpLocationService deviceIpLocationService;
    private final ViiAdMqProperties mqProperties;

    @Reference(version = "2.0", check = false)
    private ViiUserService viiUserService;

    public AdDeviceServiceImpl(AdDeviceMapper adDeviceMapper,
                               DeviceIpLocationService deviceIpLocationService,
                               ViiAdMqProperties mqProperties) {
        this.adDeviceMapper = adDeviceMapper;
        this.deviceIpLocationService = deviceIpLocationService;
        this.mqProperties = mqProperties;
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
                    .like(AdDevice::getIpAddress, query.getKeyword())
                    .or()
                    .like(AdDevice::getRegionName, query.getKeyword()));
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
        device.setDeviceCode(StringUtils.trimToNull(request.getDeviceCode()));
        validateDeviceCodeUnique(device.getDeviceCode(), null);
        validateViitalkAccountExists(device.getDeviceCode());
        applyLocation(device, deviceIpLocationService.resolve(device.getIpAddress()));
        device.setOnlineStatus(OFFLINE);
        device.setFaultStatus(NORMAL);
        device.setStatus(ACTIVE);
        device.setCreateBy(1L);
        device.setUpdateBy(1L);
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
        if (StringUtils.isNotBlank(request.getDeviceCode())) {
            device.setDeviceCode(request.getDeviceCode().trim());
        }
        validateDeviceCodeUnique(device.getDeviceCode(), id);
        validateViitalkAccountExists(device.getDeviceCode());
        applyLocation(device, deviceIpLocationService.resolve(device.getIpAddress()));
        device.setUpdateBy(1L);
        device.setUpdateTime(new Date());
        adDeviceMapper.updateById(device);
        return device;
    }

    @Override
    public AdDevice setOnline(Long id) {
        AdDevice device = getDetail(id);
        if (!ACTIVE.equals(device.getStatus())) {
            throw new BusinessException("停用设备不能上线");
        }
        device.setOnlineStatus(ONLINE);
        device.setLastOnlineTime(new Date());
        return updateStatus(device);
    }

    @Override
    public AdDevice setOffline(Long id) {
        AdDevice device = getDetail(id);
        device.setOnlineStatus(OFFLINE);
        return updateStatus(device);
    }

    @Override
    public AdDevice markFault(Long id) {
        AdDevice device = getDetail(id);
        device.setFaultStatus("fault");
        device.setOnlineStatus(OFFLINE);
        return updateStatus(device);
    }

    @Override
    public AdDevice repair(Long id) {
        AdDevice device = getDetail(id);
        device.setFaultStatus(NORMAL);
        return updateStatus(device);
    }

    @Override
    public AdDevice enable(Long id) {
        AdDevice device = getDetail(id);
        device.setStatus(ACTIVE);
        return updateStatus(device);
    }

    @Override
    public AdDevice disable(Long id) {
        AdDevice device = getDetail(id);
        device.setStatus("disabled");
        device.setOnlineStatus(OFFLINE);
        return updateStatus(device);
    }

    @Override
    public AdDevice upsertViitalkOnlineDevice(String mzNumber, String jid, String ip, String deviceType, Date eventTime) {
        String deviceCode = StringUtils.trimToNull(mzNumber);
        if (StringUtils.isBlank(deviceCode)) {
            throw new BusinessException("大屏账号不能为空");
        }
        Date now = eventTime == null ? new Date() : eventTime;
        DeviceIpLocation location = deviceIpLocationService.resolve(ip);
        if (mqProperties.isOnlyChinaDevice() && location.isResolved() && !CHINA.equals(location.getCountryName())) {
            LOGGER.info("skip non-China ViiTalk device, mzNumber={}, jid={}, ip={}, region={}", deviceCode, jid, ip, location.getRegionName());
            return null;
        }

        AdDevice device = findByDeviceCode(deviceCode);
        if (device == null) {
            device = new AdDevice();
            device.setDeviceCode(deviceCode);
            device.setDeviceName(defaultDeviceName(deviceCode, deviceType));
            device.setOnlineStatus(ONLINE);
            device.setFaultStatus(NORMAL);
            device.setStatus(ACTIVE);
            device.setIpAddress(location.getIp());
            applyLocation(device, location);
            device.setLastOnlineTime(now);
            device.setCreateBy(1L);
            device.setUpdateBy(1L);
            device.setCreateTime(now);
            device.setUpdateTime(now);
            device.setDeleted(0);
            adDeviceMapper.insert(device);
            LOGGER.info("auto created ViiTalk device, mzNumber={}, jid={}, ip={}, region={}", deviceCode, jid, ip, location.getRegionName());
            return device;
        }

        device.setOnlineStatus(ONLINE);
        device.setIpAddress(location.getIp());
        applyLocation(device, location);
        device.setLastOnlineTime(now);
        device.setUpdateBy(1L);
        device.setUpdateTime(now);
        adDeviceMapper.updateById(device);
        LOGGER.info("updated ViiTalk device online, mzNumber={}, jid={}, ip={}, region={}", deviceCode, jid, ip, location.getRegionName());
        return device;
    }

    @Override
    public AdDevice updateViitalkOfflineDevice(String mzNumber, Date eventTime) {
        String deviceCode = StringUtils.trimToNull(mzNumber);
        if (StringUtils.isBlank(deviceCode)) {
            throw new BusinessException("大屏账号不能为空");
        }
        AdDevice device = findByDeviceCode(deviceCode);
        if (device == null) {
            LOGGER.info("ignore ViiTalk device offline, device not found, mzNumber={}", deviceCode);
            return null;
        }
        Date now = eventTime == null ? new Date() : eventTime;
        device.setOnlineStatus(OFFLINE);
        device.setUpdateBy(1L);
        device.setUpdateTime(now);
        adDeviceMapper.updateById(device);
        LOGGER.info("updated ViiTalk device offline, mzNumber={}", deviceCode);
        return device;
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
        device.setIpAddress(StringUtils.trimToNull(request.getIpAddress()));
        device.setMacAddress(request.getMacAddress());
    }

    private void validateDeviceCodeUnique(String deviceCode, Long excludeId) {
        if (StringUtils.isBlank(deviceCode)) {
            throw new BusinessException("设备编码不能为空");
        }
        LambdaQueryWrapper<AdDevice> wrapper = new LambdaQueryWrapper<AdDevice>()
                .eq(AdDevice::getDeleted, 0)
                .eq(AdDevice::getDeviceCode, deviceCode);
        if (excludeId != null) {
            wrapper.ne(AdDevice::getId, excludeId);
        }
        Long count = adDeviceMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new BusinessException("设备编码已存在：" + deviceCode);
        }
    }

    private void validateViitalkAccountExists(String deviceCode) {
        try {
            ViiUser user = viiUserService.findByUsername(deviceCode);
            if (user == null) {
                throw new BusinessException("大屏账号不存在：" + deviceCode);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(500, "校验大屏账号失败：" + e.getMessage());
        }
    }

    private AdDevice findByDeviceCode(String deviceCode) {
        LambdaQueryWrapper<AdDevice> wrapper = new LambdaQueryWrapper<AdDevice>()
                .eq(AdDevice::getDeleted, 0)
                .eq(AdDevice::getDeviceCode, deviceCode)
                .last("limit 1");
        return adDeviceMapper.selectOne(wrapper);
    }

    private void applyLocation(AdDevice device, DeviceIpLocation location) {
        if (location == null) {
            return;
        }
        device.setIpAddress(StringUtils.defaultIfBlank(location.getIp(), device.getIpAddress()));
        device.setCountryName(location.getCountryName());
        device.setProvinceName(location.getProvinceName());
        device.setCityName(location.getCityName());
        device.setRegionName(location.getRegionName());
    }

    private String defaultDeviceName(String mzNumber, String deviceType) {
        if (StringUtils.isBlank(deviceType)) {
            return "ViiTalk大屏-" + mzNumber;
        }
        return "ViiTalk大屏-" + deviceType + "-" + mzNumber;
    }
}
