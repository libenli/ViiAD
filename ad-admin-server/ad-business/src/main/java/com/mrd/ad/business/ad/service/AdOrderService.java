package com.mrd.ad.business.ad.service;

import com.mrd.ad.business.ad.domain.AdOrder;
import com.mrd.ad.business.ad.dto.AdOrderCreateRequest;
import com.mrd.ad.business.ad.dto.AdOrderQuery;
import com.mrd.ad.business.ad.dto.AdOrderUpdateRequest;
import com.mrd.ad.common.core.PageResult;

public interface AdOrderService {

    PageResult<AdOrder> page(AdOrderQuery query);

    AdOrder getDetail(Long id);

    AdOrder create(AdOrderCreateRequest request);

    AdOrder update(Long id, AdOrderUpdateRequest request);

    AdOrder submit(Long id);

    AdOrder approve(Long id, String comment);

    AdOrder reject(Long id, String comment);
}

