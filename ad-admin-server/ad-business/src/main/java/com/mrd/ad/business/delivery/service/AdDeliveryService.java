package com.mrd.ad.business.delivery.service;

import com.mrd.ad.business.delivery.domain.AdDeliveryRecord;
import com.mrd.ad.business.delivery.dto.AdDeliveryQuery;
import com.mrd.ad.business.device.dto.ViitalkDeviceCommandAckRequest;
import com.mrd.ad.business.device.dto.ViitalkDeviceCommandAckResult;
import com.mrd.ad.common.core.PageResult;

import java.util.List;

public interface AdDeliveryService {

    PageResult<AdDeliveryRecord> page(AdDeliveryQuery query);

    AdDeliveryRecord getDetail(Long id);

    void createPendingRecords(Long planId, List<Long> deviceIds, String deliveryType);

    void dispatchPlan(Long planId, List<Long> deviceIds, String deliveryType);

    void stopPlan(Long planId, List<Long> deviceIds, String deliveryType);

    void handleCommandAck(ViitalkDeviceCommandAckRequest request, ViitalkDeviceCommandAckResult ackResult);

    AdDeliveryRecord markSuccess(Long id, String responseMsg);

    AdDeliveryRecord markFailed(Long id, String responseMsg);

    AdDeliveryRecord retry(Long id);
}
