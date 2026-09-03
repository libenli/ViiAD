package com.mrd.ad.business.device.service;

import com.mrd.ad.business.device.dto.DeviceIpLocation;

public interface DeviceIpLocationService {

    DeviceIpLocation resolve(String ip);
}
