package com.mrd.ad.business.device.service;

import com.mrd.ad.business.device.dto.ViitalkDeviceCommandAckRequest;
import com.mrd.ad.business.device.dto.ViitalkDeviceCommandAckResult;
import com.mrd.ad.business.device.dto.ViitalkDeviceCommandRequest;
import com.mrd.ad.business.device.dto.ViitalkDeviceCommandResult;
import com.mrd.ad.business.device.dto.ViitalkDeviceOnlineStatus;

public interface ViitalkDeviceOnlineService {

    ViitalkDeviceOnlineStatus queryOnlineStatus(String mzNumber);

    ViitalkDeviceCommandResult sendCommand(ViitalkDeviceCommandRequest request);

    ViitalkDeviceCommandAckResult receiveCommandAck(ViitalkDeviceCommandAckRequest request);

    ViitalkDeviceCommandAckResult queryCommandAck(String requestId);
}
