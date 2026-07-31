package com.mrd.ad.business.partner.service;

import com.mrd.ad.business.partner.domain.AdAdvertiser;
import com.mrd.ad.business.partner.dto.AdvertiserCreateRequest;
import com.mrd.ad.business.partner.dto.AdvertiserQuery;
import com.mrd.ad.business.partner.dto.AdvertiserUpdateRequest;
import com.mrd.ad.common.core.PageResult;

public interface AdAdvertiserService {

    PageResult<AdAdvertiser> page(AdvertiserQuery query);

    AdAdvertiser getDetail(Long id);

    AdAdvertiser create(AdvertiserCreateRequest request);

    AdAdvertiser update(Long id, AdvertiserUpdateRequest request);

    AdAdvertiser enable(Long id);

    AdAdvertiser disable(Long id);
}
