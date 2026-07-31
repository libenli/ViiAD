package com.mrd.ad.business.device.service;

import com.mrd.ad.business.device.domain.AdDevice;
import com.mrd.ad.business.device.dto.AdDeviceCreateRequest;
import com.mrd.ad.business.device.dto.AdDeviceQuery;
import com.mrd.ad.business.device.dto.AdDeviceUpdateRequest;
import com.mrd.ad.common.core.PageResult;

public interface AdDeviceService {

    PageResult<AdDevice> page(AdDeviceQuery query);

    AdDevice getDetail(Long id);

    AdDevice create(AdDeviceCreateRequest request);

    AdDevice update(Long id, AdDeviceUpdateRequest request);

    AdDevice setOnline(Long id);

    AdDevice setOffline(Long id);

    AdDevice markFault(Long id);

    AdDevice repair(Long id);

    AdDevice enable(Long id);

    AdDevice disable(Long id);
}
