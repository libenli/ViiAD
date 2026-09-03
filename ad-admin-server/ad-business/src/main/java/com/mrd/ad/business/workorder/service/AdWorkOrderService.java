package com.mrd.ad.business.workorder.service;

import com.mrd.ad.business.workorder.domain.AdWorkOrder;
import com.mrd.ad.business.workorder.dto.AdWorkOrderCreateRequest;
import com.mrd.ad.business.workorder.dto.AdWorkOrderQuery;
import com.mrd.ad.common.core.PageResult;

public interface AdWorkOrderService {

    PageResult<AdWorkOrder> page(AdWorkOrderQuery query);

    AdWorkOrder getDetail(Long id);

    AdWorkOrder create(AdWorkOrderCreateRequest request);

    AdWorkOrder createSystemOrder(String title, String content, String priority, Long relatedPlanId);

    AdWorkOrder assign(Long id, Long assigneeId);

    AdWorkOrder start(Long id);

    AdWorkOrder close(Long id, String content);

    AdWorkOrder reopen(Long id);
}
